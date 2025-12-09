package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.ManagerLeaveResponseDTO;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveApprovalHistory;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.exception.BusinessException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestService Manager Dashboard Unit Tests")
class LeaveRequestServiceManagerDashboardTest {

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
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private Employee requester;
    private LeaveType leaveType;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");

        requester = new Employee();
        requester.setId(101L);
        requester.setFirstName("Alice");
        requester.setLastName("Employee");
        requester.setDepartment(department);

        leaveType = new LeaveType();
        leaveType.setName("Yıllık İzin");
        leaveType.setWorkflowDefinition("HR,MANAGER,CEO");
    }

    @Test
    @DisplayName("HR rolü şirket geneli talepleri ve tarihçe listesini döndürür")
    void getManagerDashboardRequests_HR_ShouldReturnAllWithHistory() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("hr@example.com");

        Employee hrEmployee = new Employee();
        hrEmployee.setEmail("hr@example.com");
        hrEmployee.setDepartment(requester.getDepartment());

        Role hrRole = new Role();
        hrRole.setRoleName("HR");

        User hrUser = new User();
        hrUser.setEmployee(hrEmployee);
        hrUser.setRoles(Set.of(hrRole));

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(10L);
        leaveRequest.setEmployee(requester);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDateTime(LocalDateTime.now().plusDays(1));
        leaveRequest.setEndDateTime(LocalDateTime.now().plusDays(2));
        leaveRequest.setDurationHours(java.math.BigDecimal.ONE);
        leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        leaveRequest.setWorkflowNextApproverRole("HR");

        LeaveApprovalHistory h1 = new LeaveApprovalHistory();
        h1.setApprover(hrEmployee);
        h1.setAction(RequestStatus.APPROVED_HR);
        h1.setComments("HR onayladı");
        h1.setCreatedAt(LocalDateTime.now().minusHours(3));
        h1.setLeaveRequest(leaveRequest);

        LeaveApprovalHistory h2 = new LeaveApprovalHistory();
        h2.setApprover(hrEmployee);
        h2.setAction(RequestStatus.APPROVED_MANAGER);
        h2.setComments("Manager onayladı");
        h2.setCreatedAt(LocalDateTime.now().minusHours(1));
        h2.setLeaveRequest(leaveRequest);

        leaveRequest.setApprovalHistories(List.of(h1, h2));

        when(userRepository.findByEmployeeEmail("hr@example.com")).thenReturn(Optional.of(hrUser));
        when(leaveRequestRepository.findByWorkflowNextApproverRoleIn(org.mockito.ArgumentMatchers.<List<String>>any()))
                .thenReturn(List.of(leaveRequest));

        // Act
        List<ManagerLeaveResponseDTO> result = leaveRequestService.getManagerDashboardRequests();

        // Assert
        assertEquals(1, result.size());
        ManagerLeaveResponseDTO dto = result.get(0);
        assertEquals(leaveRequest.getId(), dto.getLeaveRequestId());
        assertEquals("Yıllık İzin", dto.getLeaveTypeName());
        assertEquals(RequestStatus.PENDING_APPROVAL, dto.getCurrentStatus());
        assertThat(dto.getApprovalHistory()).hasSize(2);
        assertThat(dto.getApprovalHistory().get(0).getActionDate()).isBefore(dto.getApprovalHistory().get(1).getActionDate());
    }

    @Test
    @DisplayName("Manager rolü sadece kendi departmanındaki talepleri çeker")
    void getManagerDashboardRequests_Manager_ShouldFilterByDepartment() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");

        Department managersDept = new Department();
        managersDept.setId(5L);
        managersDept.setName("R&D");

        Employee managerEmployee = new Employee();
        managerEmployee.setEmail("manager@example.com");
        managerEmployee.setDepartment(managersDept);

        Role managerRole = new Role();
        managerRole.setRoleName("MANAGER");

        User managerUser = new User();
        managerUser.setEmployee(managerEmployee);
        managerUser.setRoles(Set.of(managerRole));

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(20L);
        leaveRequest.setEmployee(requester);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDateTime(LocalDateTime.now().plusDays(2));
        leaveRequest.setEndDateTime(LocalDateTime.now().plusDays(3));
        leaveRequest.setDurationHours(java.math.BigDecimal.ONE);
        leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        leaveRequest.setWorkflowNextApproverRole("MANAGER");

        when(userRepository.findByEmployeeEmail("manager@example.com")).thenReturn(Optional.of(managerUser));
        when(leaveRequestRepository.findByWorkflowNextApproverRoleInAndDepartmentId(
                org.mockito.ArgumentMatchers.<List<String>>any(), any(Long.class))
        ).thenReturn(List.of(leaveRequest));

        // Act
        List<ManagerLeaveResponseDTO> result = leaveRequestService.getManagerDashboardRequests();

        // Assert
        assertEquals(1, result.size());
        verify(leaveRequestRepository).findByWorkflowNextApproverRoleInAndDepartmentId(
                org.mockito.ArgumentMatchers.<List<String>>any(), any(Long.class));
        assertEquals("MANAGER", result.get(0).getWorkflowNextApproverRole());
    }

    @Test
    @DisplayName("Yetkisiz rol dashboard isteğinde hata alır")
    void getManagerDashboardRequests_UnauthorizedRole_ShouldThrow() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("employee@example.com");

        Role employeeRole = new Role();
        employeeRole.setRoleName("EMPLOYEE");

        Employee employee = new Employee();
        employee.setEmail("employee@example.com");

        User employeeUser = new User();
        employeeUser.setEmployee(employee);
        employeeUser.setRoles(Set.of(employeeRole));

        when(userRepository.findByEmployeeEmail("employee@example.com")).thenReturn(Optional.of(employeeUser));

        // Act & Assert
        assertThrows(BusinessException.class, () -> leaveRequestService.getManagerDashboardRequests());
    }

    @Test
    @DisplayName("REJECTED veya CANCELLED talepler dashboard listesine girmez")
    void getManagerDashboardRequests_ShouldExcludeRejectedAndCancelled() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("manager@example.com");

        Department dept = new Department();
        dept.setId(7L);
        dept.setName("Ops");

        Employee managerEmployee = new Employee();
        managerEmployee.setEmail("manager@example.com");
        managerEmployee.setDepartment(dept);

        Role managerRole = new Role();
        managerRole.setRoleName("MANAGER");

        User managerUser = new User();
        managerUser.setEmployee(managerEmployee);
        managerUser.setRoles(Set.of(managerRole));

        LeaveRequest rejected = new LeaveRequest();
        rejected.setId(30L);
        rejected.setEmployee(requester);
        rejected.setLeaveType(leaveType);
        rejected.setStartDateTime(LocalDateTime.now().plusDays(1));
        rejected.setEndDateTime(LocalDateTime.now().plusDays(2));
        rejected.setDurationHours(java.math.BigDecimal.ONE);
        rejected.setRequestStatus(RequestStatus.REJECTED);
        rejected.setWorkflowNextApproverRole(""); // service zaten boşlayacak; savunma amaçlı

        LeaveRequest cancelled = new LeaveRequest();
        cancelled.setId(31L);
        cancelled.setEmployee(requester);
        cancelled.setLeaveType(leaveType);
        cancelled.setStartDateTime(LocalDateTime.now().plusDays(3));
        cancelled.setEndDateTime(LocalDateTime.now().plusDays(4));
        cancelled.setDurationHours(java.math.BigDecimal.ONE);
        cancelled.setRequestStatus(RequestStatus.CANCELLED);
        cancelled.setWorkflowNextApproverRole("");
        cancelled.setCreatedAt(null);

        when(userRepository.findByEmployeeEmail("manager@example.com")).thenReturn(Optional.of(managerUser));
        when(leaveRequestRepository.findByWorkflowNextApproverRoleInAndDepartmentId(
                org.mockito.ArgumentMatchers.<List<String>>any(), any(Long.class))
        ).thenReturn(List.of(rejected, cancelled));

        List<ManagerLeaveResponseDTO> result = leaveRequestService.getManagerDashboardRequests();

        assertThat(result).isEmpty();
    }
}

