package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveApprovalHistory;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.LeaveApprovalHistoryRepository;
import com.cozumtr.leave_management_system.repository.LeaveEntitlementRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import com.cozumtr.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestService Audit Log Unit Tests")
class LeaveRequestServiceAuditLogTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;
    @Mock
    private LeaveCalculationService leaveCalculationService;
    @Mock
    private LeaveEntitlementRepository leaveEntitlementRepository;
    @Mock
    private LeaveApprovalHistoryRepository leaveApprovalHistoryRepository;
    @Mock
    private PublicHolidayRepository publicHolidayRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private Employee approver;
    private User approverUser;
    private LeaveRequest leaveRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        approver = new Employee();
        approver.setId(10L);
        approver.setEmail("hr@example.com");

        Role hrRole = new Role();
        hrRole.setRoleName("HR");

        approverUser = new User();
        approverUser.setEmployee(approver);
        approverUser.setRoles(Set.of(hrRole));

        LeaveType leaveType = new LeaveType();
        leaveType.setWorkflowDefinition("HR");
        leaveType.setDeductsFromAnnual(false);

        leaveRequest = new LeaveRequest();
        leaveRequest.setId(1L);
        leaveRequest.setEmployee(approver); // basit senaryo
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        leaveRequest.setWorkflowNextApproverRole("HR");
    }

    @Test
    @DisplayName("approveLeaveRequest - log kaydı APPROVED ile oluşturulmalı")
    void approveLeaveRequest_ShouldCreateApprovalHistory() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(approver.getEmail());
        when(employeeRepository.findByEmail(approver.getEmail())).thenReturn(Optional.of(approver));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(userRepository.findByEmployeeEmail(approver.getEmail())).thenReturn(Optional.of(approverUser));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<LeaveApprovalHistory> historyCaptor = ArgumentCaptor.forClass(LeaveApprovalHistory.class);
        when(leaveApprovalHistoryRepository.save(historyCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        leaveRequestService.approveLeaveRequest(1L, "Onaylandı");

        // Assert
        LeaveApprovalHistory savedHistory = historyCaptor.getValue();
        assertNotNull(savedHistory);
        assertEquals(leaveRequest, savedHistory.getLeaveRequest());
        assertEquals(approver, savedHistory.getApprover());
        assertEquals(RequestStatus.APPROVED, savedHistory.getAction());
        assertEquals("Onaylandı", savedHistory.getComments());
    }

    @Test
    @DisplayName("rejectLeaveRequest - log kaydı REJECTED ile oluşturulmalı")
    void rejectLeaveRequest_ShouldCreateRejectionHistory() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(approver.getEmail());
        when(employeeRepository.findByEmail(approver.getEmail())).thenReturn(Optional.of(approver));
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(leaveRequest));
        when(userRepository.findByEmployeeEmail(approver.getEmail())).thenReturn(Optional.of(approverUser));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<LeaveApprovalHistory> historyCaptor = ArgumentCaptor.forClass(LeaveApprovalHistory.class);
        when(leaveApprovalHistoryRepository.save(historyCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        leaveRequestService.rejectLeaveRequest(1L, "Red edildi");

        // Assert
        LeaveApprovalHistory savedHistory = historyCaptor.getValue();
        assertNotNull(savedHistory);
        assertEquals(leaveRequest, savedHistory.getLeaveRequest());
        assertEquals(approver, savedHistory.getApprover());
        assertEquals(RequestStatus.REJECTED, savedHistory.getAction());
        assertEquals("Red edildi", savedHistory.getComments());
    }

    @Test
    @DisplayName("getLeaveApprovalHistory - sahibi geçmişi kronolojik görebilmeli")
    void getLeaveApprovalHistory_Owner_ShouldReturnChronologicalHistory() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(approver.getEmail());

        LeaveApprovalHistory h1 = new LeaveApprovalHistory();
        h1.setId(1L);
        h1.setLeaveRequest(leaveRequest);
        h1.setApprover(approver);
        h1.setAction(RequestStatus.APPROVED);
        h1.setComments("HR onayladı");
        h1.setCreatedAt(LocalDateTime.now().minusHours(2));

        LeaveApprovalHistory h2 = new LeaveApprovalHistory();
        h2.setId(2L);
        h2.setLeaveRequest(leaveRequest);
        h2.setApprover(approver);
        h2.setAction(RequestStatus.APPROVED);
        h2.setComments("Manager onayladı");
        h2.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(leaveRequestRepository.findById(leaveRequest.getId())).thenReturn(Optional.of(leaveRequest));
        when(leaveApprovalHistoryRepository.findByLeaveRequestIdOrderByCreatedAtAsc(leaveRequest.getId()))
                .thenReturn(List.of(h1, h2));

        // Act
        var responses = leaveRequestService.getLeaveApprovalHistory(leaveRequest.getId());

        // Assert
        assertEquals(2, responses.size());
        assertEquals("HR onayladı", responses.get(0).getComments());
        assertEquals("Manager onayladı", responses.get(1).getComments());
    }

    @Test
    @DisplayName("getLeaveApprovalHistory - yetkisi olmayan erişemez")
    void getLeaveApprovalHistory_Unauthorized_ShouldThrow() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("other@example.com");
        when(leaveRequestRepository.findById(leaveRequest.getId())).thenReturn(Optional.of(leaveRequest));
        when(userRepository.findByEmployeeEmail("other@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> leaveRequestService.getLeaveApprovalHistory(leaveRequest.getId()));
    }
}


