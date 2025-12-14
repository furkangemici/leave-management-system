package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveEntitlement;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestService - Yıllık İzin Testleri")
class LeaveRequestServiceAnnualLeaveTest {

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
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private Employee testEmployee;
    private LeaveType annualLeaveType;
    private LeaveEntitlement testEntitlement;
    private CreateLeaveRequest createRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");

        // Test Employee - 3 yıl kıdem
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmail("test@example.com");
        testEmployee.setFirstName("Test");
        testEmployee.setLastName("User");
        testEmployee.setHireDate(LocalDate.now().minusYears(3));
        testEmployee.setDailyWorkHours(new BigDecimal("8.0"));

        // Yıllık İzin Türü
        annualLeaveType = new LeaveType();
        annualLeaveType.setId(1L);
        annualLeaveType.setName("Yıllık İzin");
        annualLeaveType.setIsActive(true);
        annualLeaveType.setDeductsFromAnnual(true);
        annualLeaveType.setWorkflowDefinition("HR,MANAGER,CEO");

        // Test LeaveEntitlement - Yeterli bakiye var
        testEntitlement = new LeaveEntitlement();
        testEntitlement.setId(1L);
        testEntitlement.setEmployee(testEmployee);
        testEntitlement.setYear(LocalDate.now().getYear());
        testEntitlement.setTotalHoursEntitled(new BigDecimal("112.0")); // 14 gün × 8 saat
        testEntitlement.setHoursUsed(new BigDecimal("40.0")); // 5 gün kullanılmış
        testEntitlement.setCarriedForwardHours(BigDecimal.ZERO);

        // Test Request
        createRequest = new CreateLeaveRequest();
        createRequest.setLeaveTypeId(1L);
        createRequest.setStartDate(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));
        createRequest.setEndDate(LocalDateTime.now().plusDays(3).withHour(17).withMinute(0)); // 3 gün
        createRequest.setReason("Yıllık izin talebi");
    }

    @Test
    @DisplayName("Yıllık izin talebi oluşturma - Başarılı (yeterli bakiye)")
    void createLeaveRequest_AnnualLeave_SufficientBalance_ShouldSucceed() {
        // Given
        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L))
                .thenReturn(Optional.of(annualLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveCalculationService.calculateDuration(
                any(LocalDate.class), any(LocalDate.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("24.0")); // 3 gün × 8 saat = 24 saat
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(
                anyLong(), anyInt()))
                .thenReturn(Optional.of(testEntitlement));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> {
                    LeaveRequest req = invocation.getArgument(0);
                    req.setId(100L);
                    return req;
                });

        // When
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(createRequest);

        // Then
        assertNotNull(response);
        assertEquals("Yıllık İzin", response.getLeaveTypeName());
        assertEquals(RequestStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals("HR", response.getWorkflowNextApproverRole());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Yıllık izin talebi - Yetersiz bakiye hatası")
    void createLeaveRequest_AnnualLeave_InsufficientBalance_ShouldThrowException() {
        // Given - Tüm hak kullanılmış
        testEntitlement.setHoursUsed(new BigDecimal("112.0")); // Tüm hak kullanılmış

        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L))
                .thenReturn(Optional.of(annualLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveCalculationService.calculateDuration(
                any(LocalDate.class), any(LocalDate.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("24.0")); // 3 gün × 8 saat = 24 saat
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(
                anyLong(), anyInt()))
                .thenReturn(Optional.of(testEntitlement));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("Yetersiz"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Yıllık izin talebi - Aktarılan izin ile birlikte hesaplama")
    void createLeaveRequest_AnnualLeave_WithCarriedForward_ShouldUseTotalBalance() {
        // Given - Aktarılan izin var
        testEntitlement.setTotalHoursEntitled(new BigDecimal("144.0")); // 14 gün + 4 gün aktarılan (112 + 32)
        testEntitlement.setHoursUsed(new BigDecimal("40.0")); // 5 gün kullanılmış
        testEntitlement.setCarriedForwardHours(new BigDecimal("32.0")); // 4 gün aktarılmış
        // Kalan: 144 - 40 = 104 saat (13 gün)

        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L))
                .thenReturn(Optional.of(annualLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveCalculationService.calculateDuration(
                any(LocalDate.class), any(LocalDate.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("40.0")); // 5 gün × 8 saat = 40 saat
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(
                anyLong(), anyInt()))
                .thenReturn(Optional.of(testEntitlement));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> {
                    LeaveRequest req = invocation.getArgument(0);
                    req.setId(100L);
                    return req;
                });

        // When
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(createRequest);

        // Then
        assertNotNull(response);
        assertEquals("Yıllık İzin", response.getLeaveTypeName());
        assertEquals(RequestStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals("HR", response.getWorkflowNextApproverRole());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Yıllık izin talebi - Entitlement yoksa hata")
    void createLeaveRequest_AnnualLeave_NoEntitlement_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L))
                .thenReturn(Optional.of(annualLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveCalculationService.calculateDuration(
                any(LocalDate.class), any(LocalDate.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("24.0")); // 3 gün × 8 saat = 24 saat
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(
                anyLong(), anyInt()))
                .thenReturn(Optional.empty()); // Entitlement yok

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        String msg = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        assertTrue(msg.contains("izin bakiyesi"), "Mesaj izin bakiyesi eksikliğini belirtmeli");
        verify(leaveRequestRepository, never()).save(any());
    }
}


