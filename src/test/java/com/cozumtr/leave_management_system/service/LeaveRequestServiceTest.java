package com.cozumtr.leave_management_system.service;


import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveEntitlement;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.LeaveEntitlementRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestService Unit Tests")
class LeaveRequestServiceTest {


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
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;


    @InjectMocks
    private LeaveRequestService leaveRequestService;


    private Employee testEmployee;
    private LeaveType testLeaveType;
    private LeaveEntitlement testEntitlement;
    private CreateLeaveRequest createRequest;


    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);


        // Test Employee
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmail("test@example.com");
        testEmployee.setFirstName("Test");
        testEmployee.setLastName("User");


        // Test LeaveType
        testLeaveType = new LeaveType();
        testLeaveType.setId(1L);
        testLeaveType.setName("Yıllık İzin");
        testLeaveType.setWorkflowDefinition("HR,MANAGER,CEO");


        // Test LeaveEntitlement
        testEntitlement = new LeaveEntitlement();
        testEntitlement.setId(1L);
        testEntitlement.setEmployee(testEmployee);
        testEntitlement.setYear(LocalDate.now().getYear());
        testEntitlement.setTotalHoursEntitled(new BigDecimal("100.0"));
        testEntitlement.setHoursUsed(new BigDecimal("20.0")); // Kalan: 80 saat


        // Test CreateLeaveRequest
        createRequest = new CreateLeaveRequest();
        createRequest.setLeaveTypeId(1L);
        createRequest.setStartDate(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));
        createRequest.setEndDate(LocalDateTime.now().plusDays(3).withHour(17).withMinute(0));
        createRequest.setReason("Aile ziyareti");
    }


    // ========== CREATE LEAVE REQUEST TESTS ==========


    @Test
    @DisplayName("createLeaveRequest - Başarılı izin talebi oluşturma")
    void createLeaveRequest_Success_ShouldCreateAndReturnResponse() {
        // Arrange
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(), any(), anyList())).thenReturn(false);
        when(leaveCalculationService.calculateDuration(any(), any())).thenReturn(new BigDecimal("16.0"));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), anyInt()))
                .thenReturn(Optional.of(testEntitlement));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });


        // Act
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(createRequest);


        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Yıllık İzin", response.getLeaveTypeName());
        assertEquals(RequestStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals("HR", response.getWorkflowNextApproverRole());
        assertEquals("Aile ziyareti", response.getReason());
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }


    @Test
    @DisplayName("createLeaveRequest - Kullanıcı bulunamadığında EntityNotFoundException fırlatmalı")
    void createLeaveRequest_UserNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());


        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("Kullanıcı bulunamadı"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - Bitiş tarihi başlangıçtan önce olduğunda BusinessException fırlatmalı")
    void createLeaveRequest_EndDateBeforeStartDate_ShouldThrowBusinessException() {
        // Arrange
        String email = "test@example.com";
        createRequest.setEndDate(createRequest.getStartDate().minusDays(1));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("Bitiş tarihi başlangıç tarihinden önce olamaz"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - Tarih çakışması olduğunda BusinessException fırlatmalı")
    void createLeaveRequest_DateOverlap_ShouldThrowBusinessException() {
        // Arrange
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        // Çakışma kontrolü izin türü kontrolünden önce yapıldığı için leaveTypeRepository stub'ına gerek yok
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(), any(), anyList())).thenReturn(true);


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("mevcut bir izin kaydınız var"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - Geçersiz izin türü ID'si için EntityNotFoundException fırlatmalı")
    void createLeaveRequest_InvalidLeaveTypeId_ShouldThrowEntityNotFoundException() {
        // Arrange
        String email = "test@example.com";
        createRequest.setLeaveTypeId(999L);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(999L)).thenReturn(Optional.empty());


        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("Geçersiz İzin Türü ID"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - Hesaplanan süre 0 veya negatif olduğunda BusinessException fırlatmalı")
    void createLeaveRequest_ZeroOrNegativeDuration_ShouldThrowBusinessException() {
        // Arrange
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(), any(), anyList())).thenReturn(false);
        when(leaveCalculationService.calculateDuration(any(), any())).thenReturn(BigDecimal.ZERO);


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("Hesaplanabilir iş günü bulunamadı"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - İzin bakiyesi bulunamadığında BusinessException fırlatmalı")
    void createLeaveRequest_EntitlementNotFound_ShouldThrowBusinessException() {
        // Arrange
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(), any(), anyList())).thenReturn(false);
        when(leaveCalculationService.calculateDuration(any(), any())).thenReturn(new BigDecimal("16.0"));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), anyInt()))
                .thenReturn(Optional.empty());


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("İzin bakiyesi bulunamadı"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - Yetersiz bakiye olduğunda BusinessException fırlatmalı")
    void createLeaveRequest_InsufficientBalance_ShouldThrowBusinessException() {
        // Arrange
        String email = "test@example.com";
        // Kalan bakiye: 5 saat, Talep: 16 saat
        testEntitlement.setTotalHoursEntitled(new BigDecimal("20.0"));
        testEntitlement.setHoursUsed(new BigDecimal("15.0")); // Kalan: 5 saat
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(), any(), anyList())).thenReturn(false);
        when(leaveCalculationService.calculateDuration(any(), any())).thenReturn(new BigDecimal("16.0"));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), anyInt()))
                .thenReturn(Optional.of(testEntitlement));


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("Yetersiz izin bakiyesi"));
        assertTrue(exception.getMessage().contains("16.0"));
        assertTrue(exception.getMessage().contains("5.0"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - Workflow tanımı boş olduğunda BusinessException fırlatmalı")
    void createLeaveRequest_EmptyWorkflowDefinition_ShouldThrowBusinessException() {
        // Arrange
        String email = "test@example.com";
        testLeaveType.setWorkflowDefinition("");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(), any(), anyList())).thenReturn(false);
        when(leaveCalculationService.calculateDuration(any(), any())).thenReturn(new BigDecimal("16.0"));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), anyInt()))
                .thenReturn(Optional.of(testEntitlement));


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("onay akışı tanımlanmamış"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - Reason null olduğunda boş string olarak kaydedilmeli")
    void createLeaveRequest_NullReason_ShouldSaveAsEmptyString() {
        // Arrange
        String email = "test@example.com";
        createRequest.setReason(null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(), any(), anyList())).thenReturn(false);
        when(leaveCalculationService.calculateDuration(any(), any())).thenReturn(new BigDecimal("16.0"));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), anyInt()))
                .thenReturn(Optional.of(testEntitlement));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });


        // Act
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(createRequest);


        // Assert
        assertNotNull(response);
        assertEquals("", response.getReason());
        verify(leaveRequestRepository).save(argThat(req -> req.getReason().equals("")));
    }


    // ========== CANCEL LEAVE REQUEST TESTS ==========


    @Test
    @DisplayName("cancelLeaveRequest - Başarılı iptal işlemi")
    void cancelLeaveRequest_Success_ShouldUpdateStatusToCancelled() {
        // Arrange
        Long requestId = 100L;
        String email = "test@example.com";
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setEmployee(testEmployee);
        leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);


        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);


        // Act
        leaveRequestService.cancelLeaveRequest(requestId);


        // Assert
        assertEquals(RequestStatus.CANCELLED, leaveRequest.getRequestStatus());
        verify(leaveRequestRepository).save(leaveRequest);
    }


    @Test
    @DisplayName("cancelLeaveRequest - İzin talebi bulunamadığında EntityNotFoundException fırlatmalı")
    void cancelLeaveRequest_RequestNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange
        Long requestId = 999L;
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.empty());


        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            leaveRequestService.cancelLeaveRequest(requestId);
        });
        assertTrue(exception.getMessage().contains("İzin talebi bulunamadı"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("cancelLeaveRequest - Başkasının iznini iptal etmeye çalıştığında BusinessException fırlatmalı")
    void cancelLeaveRequest_UnauthorizedUser_ShouldThrowBusinessException() {
        // Arrange
        Long requestId = 100L;
        String currentEmail = "current@example.com";
        String ownerEmail = "owner@example.com";


        Employee owner = new Employee();
        owner.setEmail(ownerEmail);


        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setEmployee(owner);
        leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);


        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(currentEmail);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.cancelLeaveRequest(requestId);
        });
        assertTrue(exception.getMessage().contains("yetkiniz yok"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("cancelLeaveRequest - APPROVED durumundaki izin iptal edilememeli")
    void cancelLeaveRequest_ApprovedStatus_ShouldThrowBusinessException() {
        // Arrange
        Long requestId = 100L;
        String email = "test@example.com";
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setEmployee(testEmployee);
        leaveRequest.setRequestStatus(RequestStatus.APPROVED);


        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.cancelLeaveRequest(requestId);
        });
        assertTrue(exception.getMessage().contains("Sadece onay bekleyen izin talepleri iptal edilebilir"));
        assertTrue(exception.getMessage().contains("APPROVED"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("cancelLeaveRequest - REJECTED durumundaki izin iptal edilememeli")
    void cancelLeaveRequest_RejectedStatus_ShouldThrowBusinessException() {
        // Arrange
        Long requestId = 100L;
        String email = "test@example.com";
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setEmployee(testEmployee);
        leaveRequest.setRequestStatus(RequestStatus.REJECTED);


        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.cancelLeaveRequest(requestId);
        });
        assertTrue(exception.getMessage().contains("Sadece onay bekleyen izin talepleri iptal edilebilir"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("cancelLeaveRequest - CANCELLED durumundaki izin tekrar iptal edilememeli")
    void cancelLeaveRequest_AlreadyCancelled_ShouldThrowBusinessException() {
        // Arrange
        Long requestId = 100L;
        String email = "test@example.com";
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setEmployee(testEmployee);
        leaveRequest.setRequestStatus(RequestStatus.CANCELLED);


        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.cancelLeaveRequest(requestId);
        });
        assertTrue(exception.getMessage().contains("Sadece onay bekleyen izin talepleri iptal edilebilir"));
        verify(leaveRequestRepository, never()).save(any());
    }


    // ========== GET MY LEAVE REQUESTS TESTS ==========


    @Test
    @DisplayName("getMyLeaveRequests - Başarılı liste döndürmeli")
    void getMyLeaveRequests_Success_ShouldReturnList() {
        // Arrange
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));


        LeaveRequest request1 = new LeaveRequest();
        request1.setId(1L);
        request1.setEmployee(testEmployee);
        request1.setLeaveType(testLeaveType);
        request1.setStartDateTime(LocalDateTime.now().plusDays(1));
        request1.setEndDateTime(LocalDateTime.now().plusDays(3));
        request1.setDurationHours(new BigDecimal("16.0"));
        request1.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        request1.setWorkflowNextApproverRole("HR");
        request1.setReason("Test reason");


        LeaveRequest request2 = new LeaveRequest();
        request2.setId(2L);
        request2.setEmployee(testEmployee);
        request2.setLeaveType(testLeaveType);
        request2.setStartDateTime(LocalDateTime.now().plusDays(10));
        request2.setEndDateTime(LocalDateTime.now().plusDays(12));
        request2.setDurationHours(new BigDecimal("16.0"));
        request2.setRequestStatus(RequestStatus.APPROVED);
        request2.setWorkflowNextApproverRole("MANAGER");
        request2.setReason("Test reason 2");


        List<LeaveRequest> requests = List.of(request1, request2);
        when(leaveRequestRepository.findByEmployeeId(testEmployee.getId())).thenReturn(requests);


        // Act
        List<LeaveRequestResponse> responses = leaveRequestService.getMyLeaveRequests();


        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(2L, responses.get(1).getId());
        assertEquals(RequestStatus.PENDING_APPROVAL, responses.get(0).getStatus());
        assertEquals(RequestStatus.APPROVED, responses.get(1).getStatus());
    }


    @Test
    @DisplayName("getMyLeaveRequests - Kullanıcı bulunamadığında EntityNotFoundException fırlatmalı")
    void getMyLeaveRequests_UserNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());


        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            leaveRequestService.getMyLeaveRequests();
        });
        assertTrue(exception.getMessage().contains("Kullanıcı bulunamadı"));
    }


    @Test
    @DisplayName("getMyLeaveRequests - İzin talebi yoksa boş liste döndürmeli")
    void getMyLeaveRequests_NoRequests_ShouldReturnEmptyList() {
        // Arrange
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveRequestRepository.findByEmployeeId(testEmployee.getId())).thenReturn(new ArrayList<>());


        // Act
        List<LeaveRequestResponse> responses = leaveRequestService.getMyLeaveRequests();


        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }
}

