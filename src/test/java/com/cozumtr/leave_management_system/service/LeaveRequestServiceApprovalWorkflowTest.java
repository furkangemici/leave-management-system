package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.entities.Role;
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
@DisplayName("LeaveRequestService - Onay Akışı Testleri")
class LeaveRequestServiceApprovalWorkflowTest {

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
    private Employee hrUser;
    private Employee managerUser;
    private Employee ceoUser;
    private User hrUserEntity;
    private User managerUserEntity;
    private User ceoUserEntity;
    private LeaveType annualLeaveType;
    private LeaveRequest testLeaveRequest;
    private LeaveEntitlement testEntitlement;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // Test Employee
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmail("employee@example.com");
        testEmployee.setFirstName("Test");
        testEmployee.setLastName("Employee");
        testEmployee.setHireDate(LocalDate.now().minusYears(3));
        testEmployee.setDailyWorkHours(new BigDecimal("8.0"));

        // HR User
        hrUser = new Employee();
        hrUser.setId(2L);
        hrUser.setEmail("hr@example.com");
        Role hrRole = new Role();
        hrRole.setRoleName("HR");
        hrUserEntity = new User();
        hrUserEntity.setEmployee(hrUser);
        hrUserEntity.setRoles(java.util.Set.of(hrRole));

        // Manager User
        managerUser = new Employee();
        managerUser.setId(3L);
        managerUser.setEmail("manager@example.com");
        Role managerRole = new Role();
        managerRole.setRoleName("MANAGER");
        managerUserEntity = new User();
        managerUserEntity.setEmployee(managerUser);
        managerUserEntity.setRoles(java.util.Set.of(managerRole));

        // CEO User
        ceoUser = new Employee();
        ceoUser.setId(4L);
        ceoUser.setEmail("ceo@example.com");
        Role ceoRole = new Role();
        ceoRole.setRoleName("CEO");
        ceoUserEntity = new User();
        ceoUserEntity.setEmployee(ceoUser);
        ceoUserEntity.setRoles(java.util.Set.of(ceoRole));

        // Yıllık İzin Türü - 3 aşamalı workflow
        annualLeaveType = new LeaveType();
        annualLeaveType.setId(1L);
        annualLeaveType.setName("Yıllık İzin");
        annualLeaveType.setDeductsFromAnnual(true);
        annualLeaveType.setWorkflowDefinition("HR,MANAGER,CEO");

        // Test Leave Request
        testLeaveRequest = new LeaveRequest();
        testLeaveRequest.setId(100L);
        testLeaveRequest.setEmployee(testEmployee);
        testLeaveRequest.setLeaveType(annualLeaveType);
        testLeaveRequest.setStartDateTime(LocalDateTime.now().plusDays(1));
        testLeaveRequest.setEndDateTime(LocalDateTime.now().plusDays(3));
        testLeaveRequest.setDurationHours(new BigDecimal("24.0")); // 3 gün
        testLeaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        testLeaveRequest.setWorkflowNextApproverRole("HR");
        testLeaveRequest.setReason("Yıllık izin");

        // Test Leave Entitlement
        testEntitlement = new LeaveEntitlement();
        testEntitlement.setId(1L);
        testEntitlement.setEmployee(testEmployee);
        testEntitlement.setYear(LocalDate.now().getYear());
        testEntitlement.setTotalHoursEntitled(new BigDecimal("112.0")); // 14 gün
        testEntitlement.setHoursUsed(new BigDecimal("40.0")); // 5 gün kullanılmış
        testEntitlement.setCarriedForwardHours(BigDecimal.ZERO);
    }

    // ========== ONAY AKIŞI TESTLERİ ==========

    @Test
    @DisplayName("approveLeaveRequest - HR onayı: PENDING_APPROVAL -> APPROVED_HR, sıradaki MANAGER")
    void approveLeaveRequest_HRApproval_ShouldUpdateToApprovedHR() {
        // Given
        when(authentication.getName()).thenReturn("hr@example.com");
        when(employeeRepository.findByEmail("hr@example.com"))
                .thenReturn(Optional.of(hrUser));
        when(userRepository.findByEmployeeEmail("hr@example.com"))
                .thenReturn(Optional.of(hrUserEntity));
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveApprovalHistoryRepository.save(any(LeaveApprovalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(100L, "HR onayladı");

        // Then
        assertNotNull(response);
        assertEquals(RequestStatus.APPROVED_HR, testLeaveRequest.getRequestStatus());
        assertEquals("MANAGER", testLeaveRequest.getWorkflowNextApproverRole());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
        verify(leaveApprovalHistoryRepository, times(1)).save(any(LeaveApprovalHistory.class));
        // Bakiye düşmemeli (henüz final approval değil)
        verify(leaveEntitlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveLeaveRequest - MANAGER onayı: APPROVED_HR -> APPROVED_MANAGER, sıradaki CEO")
    void approveLeaveRequest_ManagerApproval_ShouldUpdateToApprovedManager() {
        // Given
        testLeaveRequest.setRequestStatus(RequestStatus.APPROVED_HR);
        testLeaveRequest.setWorkflowNextApproverRole("MANAGER");

        when(authentication.getName()).thenReturn("manager@example.com");
        when(employeeRepository.findByEmail("manager@example.com"))
                .thenReturn(Optional.of(managerUser));
        when(userRepository.findByEmployeeEmail("manager@example.com"))
                .thenReturn(Optional.of(managerUserEntity));
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveApprovalHistoryRepository.save(any(LeaveApprovalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(100L, "Manager onayladı");

        // Then
        assertNotNull(response);
        assertEquals(RequestStatus.APPROVED_MANAGER, testLeaveRequest.getRequestStatus());
        assertEquals("CEO", testLeaveRequest.getWorkflowNextApproverRole());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
        verify(leaveApprovalHistoryRepository, times(1)).save(any(LeaveApprovalHistory.class));
        // Bakiye düşmemeli (henüz final approval değil)
        verify(leaveEntitlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveLeaveRequest - CEO onayı: APPROVED_MANAGER -> APPROVED, bakiye düşmeli")
    void approveLeaveRequest_CEOApproval_ShouldUpdateToApprovedAndDeductBalance() {
        // Given
        testLeaveRequest.setRequestStatus(RequestStatus.APPROVED_MANAGER);
        testLeaveRequest.setWorkflowNextApproverRole("CEO");

        when(authentication.getName()).thenReturn("ceo@example.com");
        when(employeeRepository.findByEmail("ceo@example.com"))
                .thenReturn(Optional.of(ceoUser));
        when(userRepository.findByEmployeeEmail("ceo@example.com"))
                .thenReturn(Optional.of(ceoUserEntity));
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(
                eq(1L), eq(LocalDate.now().getYear())))
                .thenReturn(Optional.of(testEntitlement));
        when(leaveEntitlementRepository.save(any(LeaveEntitlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveApprovalHistoryRepository.save(any(LeaveApprovalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(100L, "CEO onayladı");

        // Then
        assertNotNull(response);
        assertEquals(RequestStatus.APPROVED, testLeaveRequest.getRequestStatus());
        assertEquals("", testLeaveRequest.getWorkflowNextApproverRole()); // Son onaycı (boş string)
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
        verify(leaveApprovalHistoryRepository, times(1)).save(any(LeaveApprovalHistory.class));
        
        // Bakiye düşmeli (final approval)
        verify(leaveEntitlementRepository, times(1)).save(any(LeaveEntitlement.class));
        // hoursUsed artmış olmalı: 40.0 + 24.0 = 64.0
        assertEquals(0, testEntitlement.getHoursUsed().compareTo(new BigDecimal("64.0")));
    }

    @Test
    @DisplayName("approveLeaveRequest - Yanlış rolle onaylamaya çalışırsa hata")
    void approveLeaveRequest_WrongRole_ShouldThrowException() {
        // Given
        testLeaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        testLeaveRequest.setWorkflowNextApproverRole("HR");

        when(authentication.getName()).thenReturn("manager@example.com"); // MANAGER, ama sırada HR var
        when(employeeRepository.findByEmail("manager@example.com"))
                .thenReturn(Optional.of(managerUser));
        when(userRepository.findByEmployeeEmail("manager@example.com"))
                .thenReturn(Optional.of(managerUserEntity));
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.approveLeaveRequest(100L, "Onay");
        });

        assertTrue(exception.getMessage().contains("yetkiniz yok"));
        assertTrue(exception.getMessage().contains("HR"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveLeaveRequest - İzin talebi bulunamazsa hata")
    void approveLeaveRequest_RequestNotFound_ShouldThrowException() {
        // Given
        when(authentication.getName()).thenReturn("hr@example.com");
        when(employeeRepository.findByEmail("hr@example.com"))
                .thenReturn(Optional.of(hrUser));
        when(leaveRequestRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        jakarta.persistence.EntityNotFoundException exception = assertThrows(
                jakarta.persistence.EntityNotFoundException.class, () -> {
                    leaveRequestService.approveLeaveRequest(999L, "Onay");
                });

        assertTrue(exception.getMessage().contains("bulunamadı"));
        verify(leaveRequestRepository, never()).save(any());
    }

    // ========== RED AKIŞI TESTLERİ ==========

    @Test
    @DisplayName("rejectLeaveRequest - HR reddi: PENDING_APPROVAL -> REJECTED")
    void rejectLeaveRequest_HRRejection_ShouldUpdateToRejected() {
        // Given
        testLeaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        testLeaveRequest.setWorkflowNextApproverRole("HR");

        when(authentication.getName()).thenReturn("hr@example.com");
        when(employeeRepository.findByEmail("hr@example.com"))
                .thenReturn(Optional.of(hrUser));
        when(userRepository.findByEmployeeEmail("hr@example.com"))
                .thenReturn(Optional.of(hrUserEntity));
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveApprovalHistoryRepository.save(any(LeaveApprovalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(100L, "Reddedildi");

        // Then
        assertNotNull(response);
        assertEquals(RequestStatus.REJECTED, testLeaveRequest.getRequestStatus());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
        verify(leaveApprovalHistoryRepository, times(1)).save(any(LeaveApprovalHistory.class));
        // Bakiye zaten düşmemişti, geri alınmasına gerek yok
        verify(leaveEntitlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejectLeaveRequest - APPROVED durumundaki izin reddedilirse bakiye geri alınmalı")
    void rejectLeaveRequest_AfterApproved_ShouldRestoreBalance() {
        // Given
        testLeaveRequest.setRequestStatus(RequestStatus.APPROVED); // Zaten onaylanmış
        testLeaveRequest.setWorkflowNextApproverRole(""); // APPROVED durumunda boş olmalı
        testEntitlement.setHoursUsed(new BigDecimal("64.0")); // İzin kullanılmış (40 + 24)

        when(authentication.getName()).thenReturn("ceo@example.com");
        when(employeeRepository.findByEmail("ceo@example.com"))
                .thenReturn(Optional.of(ceoUser));
        // NOT: APPROVED durumunda workflow kontrolü yapılmadığı için userRepository mock'una gerek yok
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(
                eq(1L), eq(LocalDate.now().getYear())))
                .thenReturn(Optional.of(testEntitlement));
        when(leaveEntitlementRepository.save(any(LeaveEntitlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveApprovalHistoryRepository.save(any(LeaveApprovalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(100L, "Geri alındı");

        // Then
        assertNotNull(response);
        assertEquals(RequestStatus.REJECTED, testLeaveRequest.getRequestStatus());
        
        // Bakiye geri alınmalı: 64.0 - 24.0 = 40.0
        verify(leaveEntitlementRepository, times(1)).save(any(LeaveEntitlement.class));
        assertEquals(0, testEntitlement.getHoursUsed().compareTo(new BigDecimal("40.0")));
    }

    @Test
    @DisplayName("rejectLeaveRequest - Yanlış rolle reddetmeye çalışırsa hata")
    void rejectLeaveRequest_WrongRole_ShouldThrowException() {
        // Given
        testLeaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        testLeaveRequest.setWorkflowNextApproverRole("HR");

        when(authentication.getName()).thenReturn("manager@example.com"); // MANAGER, ama sırada HR var
        when(employeeRepository.findByEmail("manager@example.com"))
                .thenReturn(Optional.of(managerUser));
        when(userRepository.findByEmployeeEmail("manager@example.com"))
                .thenReturn(Optional.of(managerUserEntity));
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.rejectLeaveRequest(100L, "Red");
        });

        assertTrue(exception.getMessage().contains("yetkiniz yok"));
        verify(leaveRequestRepository, never()).save(any());
    }

    // ========== İPTAL AKIŞI TESTLERİ ==========

    @Test
    @DisplayName("cancelLeaveRequest - APPROVED durumundaki izin iptal edilirse bakiye geri alınmalı")
    void cancelLeaveRequest_ApprovedLeave_ShouldRestoreBalance() {
        // Given
        testLeaveRequest.setRequestStatus(RequestStatus.APPROVED);
        testEntitlement.setHoursUsed(new BigDecimal("64.0")); // İzin kullanılmış

        when(authentication.getName()).thenReturn("employee@example.com");
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(
                eq(1L), eq(LocalDate.now().getYear())))
                .thenReturn(Optional.of(testEntitlement));
        when(leaveEntitlementRepository.save(any(LeaveEntitlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        leaveRequestService.cancelLeaveRequest(100L);

        // Then
        assertEquals(RequestStatus.CANCELLED, testLeaveRequest.getRequestStatus());
        
        // Bakiye geri alınmalı: 64.0 - 24.0 = 40.0
        verify(leaveEntitlementRepository, times(1)).save(any(LeaveEntitlement.class));
        assertEquals(0, testEntitlement.getHoursUsed().compareTo(new BigDecimal("40.0")));
    }

    @Test
    @DisplayName("cancelLeaveRequest - APPROVED_HR durumundaki izin iptal edilirse bakiye geri alınmamalı")
    void cancelLeaveRequest_ApprovedHRLeave_ShouldNotRestoreBalance() {
        // Given
        testLeaveRequest.setRequestStatus(RequestStatus.APPROVED_HR);
        // hoursUsed zaten artmamış (final approval değil)

        when(authentication.getName()).thenReturn("employee@example.com");
        when(leaveRequestRepository.findById(100L))
                .thenReturn(Optional.of(testLeaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        leaveRequestService.cancelLeaveRequest(100L);

        // Then
        assertEquals(RequestStatus.CANCELLED, testLeaveRequest.getRequestStatus());
        
        // Bakiye geri alınmamalı (zaten düşmemişti)
        verify(leaveEntitlementRepository, never()).save(any());
    }
}

