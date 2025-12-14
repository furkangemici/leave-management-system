package com.cozumtr.leave_management_system.service;


import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.dto.response.TeamLeaveResponseDTO;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveEntitlement;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.enums.RequestUnit;
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
import org.springframework.mock.web.MockMultipartFile;


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
    private LeaveAttachmentService leaveAttachmentService;
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
        testEmployee.setDailyWorkHours(new BigDecimal("8.0")); // Günlük çalışma saati


        // Test LeaveType
        testLeaveType = new LeaveType();
        testLeaveType.setId(1L);
        testLeaveType.setName("Yıllık İzin");
        testLeaveType.setDeductsFromAnnual(true);
        testLeaveType.setWorkflowDefinition("HR,MANAGER,CEO");
        testLeaveType.setRequestUnit(RequestUnit.DAY); // Günlük izin türü


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
        // calculateDuration saat döndürür, 2 gün * 8 saat = 16 saat
        when(leaveCalculationService.calculateDuration(any(), any(), any())).thenReturn(new BigDecimal("16.0"));
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
    @DisplayName("createLeaveRequest - Belge zorunlu tür için dosya yoksa BusinessException fırlatmalı")
    void createLeaveRequest_DocumentRequired_NoFile_ShouldThrowBusinessException() {
        // Arrange
        testLeaveType.setDocumentRequired(true);
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveRequestService.createLeaveRequest(createRequest, null));
        assertTrue(ex.getMessage().contains("belge yüklemek zorunludur"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLeaveRequest - Belge zorunlu tür için dosya varsa başarıyla oluşturmalı ve upload çağrılmalı")
    void createLeaveRequest_DocumentRequired_WithFile_ShouldCreateAndUpload() {
        // Arrange
        testLeaveType.setDocumentRequired(true);
        String email = "test@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(anyLong(), any(), any(), anyList())).thenReturn(false);
        when(leaveCalculationService.calculateDuration(any(), any(), any())).thenReturn(new BigDecimal("16.0"));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), anyInt())).thenReturn(Optional.of(testEntitlement));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest req = invocation.getArgument(0);
            req.setId(200L);
            return req;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "rapor.pdf", "application/pdf", "dummy".getBytes()
        );

        // Act
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(createRequest, file);

        // Assert
        assertNotNull(response);
        assertEquals(200L, response.getId());
        verify(leaveAttachmentService).uploadAttachment(200L, file);
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
        when(leaveCalculationService.calculateDuration(any(), any(), any())).thenReturn(BigDecimal.ZERO);


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("Hesaplanabilir süre bulunamadı"));
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
        // calculateDuration saat döndürür, 2 gün * 8 saat = 16 saat
        when(leaveCalculationService.calculateDuration(any(), any(), any())).thenReturn(new BigDecimal("16.0"));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), anyInt()))
                .thenReturn(Optional.empty());


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("Yıllık izin bakiyesi bulunamadı"));
        verify(leaveRequestRepository, never()).save(any());
    }


    @Test
    @DisplayName("createLeaveRequest - Yetersiz bakiye olduğunda BusinessException fırlatmalı")
    void createLeaveRequest_InsufficientBalance_ShouldThrowBusinessException() {
        // Arrange
        String email = "test@example.com";
        // Kalan bakiye: 5 saat, Talep: 16 saat (2.0 gün * 8 saat)
        testEntitlement.setTotalHoursEntitled(new BigDecimal("20.0"));
        testEntitlement.setHoursUsed(new BigDecimal("15.0")); // Kalan: 5 saat
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(), any(), anyList())).thenReturn(false);
        // calculateDuration saat döndürür, 2 gün * 8 saat = 16 saat
        when(leaveCalculationService.calculateDuration(any(), any(), any())).thenReturn(new BigDecimal("16.0"));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), anyInt()))
                .thenReturn(Optional.of(testEntitlement));


        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });
        assertTrue(exception.getMessage().contains("Yetersiz yıllık izin bakiyesi"));
        assertTrue(exception.getMessage().contains("16.0") || exception.getMessage().contains("16"));
        assertTrue(exception.getMessage().contains("5.0") || exception.getMessage().contains("5"));
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
        // calculateDuration saat döndürür, 2 gün * 8 saat = 16 saat - bakiye kontrolünden geçmesi için yeterli
        when(leaveCalculationService.calculateDuration(any(), any(), any())).thenReturn(new BigDecimal("16.0"));
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
        // calculateDuration saat döndürür, 2 gün * 8 saat = 16 saat
        when(leaveCalculationService.calculateDuration(any(), any(), any())).thenReturn(new BigDecimal("16.0"));
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
    @DisplayName("cancelLeaveRequest - APPROVED durumundaki izin iptal edilebilmeli ve bakiye geri alınmalı")
    void cancelLeaveRequest_ApprovedStatus_ShouldCancelAndRestoreBalance() {
        // Arrange
        Long requestId = 100L;
        String email = "test@example.com";
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(requestId);
        leaveRequest.setEmployee(testEmployee);
        leaveRequest.setLeaveType(testLeaveType); // LeaveType set edilmeli (restoreLeaveBalance için)
        leaveRequest.setRequestStatus(RequestStatus.APPROVED);
        leaveRequest.setDurationHours(new BigDecimal("24.0"));

        testEntitlement.setHoursUsed(new BigDecimal("44.0")); // 40 + 24 kullanılmış

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveRequest));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(
                eq(1L), eq(LocalDate.now().getYear())))
                .thenReturn(Optional.of(testEntitlement));
        when(leaveEntitlementRepository.save(any(LeaveEntitlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);


        // Act
        leaveRequestService.cancelLeaveRequest(requestId);


        // Assert
        assertEquals(RequestStatus.CANCELLED, leaveRequest.getRequestStatus());
        verify(leaveRequestRepository, times(1)).save(leaveRequest);
        verify(leaveEntitlementRepository, times(1)).save(any(LeaveEntitlement.class));
        // Bakiye geri alınmalı: 44.0 - 24.0 = 20.0
        assertEquals(0, testEntitlement.getHoursUsed().compareTo(new BigDecimal("20.0")));
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
        assertTrue(exception.getMessage().contains("zaten iptal edilmiş veya reddedilmiş"));
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
        assertTrue(exception.getMessage().contains("zaten iptal edilmiş veya reddedilmiş"));
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

    // ========== EKİP İZİN TAKİBİ (TEAM VISIBILITY) TESTLERİ ==========

    @Test
    @DisplayName("getTeamApprovedLeaves - Başarılı senaryo: Departmandaki onaylanmış izinleri getirmeli")
    void getTeamApprovedLeaves_Success_ShouldReturnApprovedLeaves() {
        // Arrange
        Long employeeId = 1L;
        Long departmentId = 10L;
        
        // Department oluştur
        Department department = new Department();
        department.setId(departmentId);
        department.setName("IT Department");
        
        // Employee oluştur ve department'e bağla
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setDepartment(department);
        
        // Aynı departmandan başka bir çalışan
        Employee teamMember1 = new Employee();
        teamMember1.setId(2L);
        teamMember1.setFirstName("Jane");
        teamMember1.setLastName("Smith");
        teamMember1.setDepartment(department);
        
        // Farklı departmandan çalışan
        Department otherDepartment = new Department();
        otherDepartment.setId(20L);
        otherDepartment.setName("HR Department");
        Employee otherDeptEmployee = new Employee();
        otherDeptEmployee.setId(3L);
        otherDeptEmployee.setFirstName("Bob");
        otherDeptEmployee.setLastName("Johnson");
        otherDeptEmployee.setDepartment(otherDepartment);
        
        // LeaveType oluştur
        LeaveType leaveType = new LeaveType();
        leaveType.setId(1L);
        leaveType.setName("Yıllık İzin");
        
        // Gelecekteki onaylanmış izin (gösterilmeli)
        LocalDateTime futureStart = LocalDateTime.now().plusDays(5);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(7);
        LeaveRequest approvedFutureLeave = new LeaveRequest();
        approvedFutureLeave.setId(1L);
        approvedFutureLeave.setEmployee(teamMember1);
        approvedFutureLeave.setLeaveType(leaveType);
        approvedFutureLeave.setRequestStatus(RequestStatus.APPROVED);
        approvedFutureLeave.setStartDateTime(futureStart);
        approvedFutureLeave.setEndDateTime(futureEnd);
        approvedFutureLeave.setDurationHours(new BigDecimal("16.0"));
        
        // Şu anda devam eden onaylanmış izin (gösterilmeli)
        LocalDateTime currentStart = LocalDateTime.now().minusDays(1);
        LocalDateTime currentEnd = LocalDateTime.now().plusDays(1);
        LeaveRequest approvedCurrentLeave = new LeaveRequest();
        approvedCurrentLeave.setId(2L);
        approvedCurrentLeave.setEmployee(teamMember1);
        approvedCurrentLeave.setLeaveType(leaveType);
        approvedCurrentLeave.setRequestStatus(RequestStatus.APPROVED);
        approvedCurrentLeave.setStartDateTime(currentStart);
        approvedCurrentLeave.setEndDateTime(currentEnd);
        approvedCurrentLeave.setDurationHours(new BigDecimal("16.0"));
        
        // Geçmişteki onaylanmış izin (gösterilmemeli - endDateTime < now)
        LocalDateTime pastStart = LocalDateTime.now().minusDays(10);
        LocalDateTime pastEnd = LocalDateTime.now().minusDays(8);
        LeaveRequest approvedPastLeave = new LeaveRequest();
        approvedPastLeave.setId(3L);
        approvedPastLeave.setEmployee(teamMember1);
        approvedPastLeave.setLeaveType(leaveType);
        approvedPastLeave.setRequestStatus(RequestStatus.APPROVED);
        approvedPastLeave.setStartDateTime(pastStart);
        approvedPastLeave.setEndDateTime(pastEnd);
        approvedPastLeave.setDurationHours(new BigDecimal("16.0"));
        
        // Bekleyen izin (gösterilmemeli - status != APPROVED)
        LeaveRequest pendingLeave = new LeaveRequest();
        pendingLeave.setId(4L);
        pendingLeave.setEmployee(teamMember1);
        pendingLeave.setLeaveType(leaveType);
        pendingLeave.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        pendingLeave.setStartDateTime(futureStart);
        pendingLeave.setEndDateTime(futureEnd);
        pendingLeave.setDurationHours(new BigDecimal("16.0"));
        
        // Farklı departmandan onaylanmış izin (gösterilmemeli)
        LeaveRequest otherDeptLeave = new LeaveRequest();
        otherDeptLeave.setId(5L);
        otherDeptLeave.setEmployee(otherDeptEmployee);
        otherDeptLeave.setLeaveType(leaveType);
        otherDeptLeave.setRequestStatus(RequestStatus.APPROVED);
        otherDeptLeave.setStartDateTime(futureStart);
        otherDeptLeave.setEndDateTime(futureEnd);
        otherDeptLeave.setDurationHours(new BigDecimal("16.0"));
        
        List<LeaveRequest> approvedLeaves = List.of(approvedFutureLeave, approvedCurrentLeave);
        
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findApprovedLeavesByDepartment(eq(departmentId), any(LocalDateTime.class)))
                .thenReturn(approvedLeaves);
        
        // Act
        List<TeamLeaveResponseDTO> result = leaveRequestService.getTeamApprovedLeaves(employeeId);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // İlk izin kontrolü
        TeamLeaveResponseDTO firstLeave = result.get(0);
        assertEquals("Jane Smith", firstLeave.getEmployeeFullName());
        assertEquals("IT Department", firstLeave.getDepartmentName());
        assertEquals("Yıllık İzin", firstLeave.getLeaveTypeName());
        assertEquals(new BigDecimal("16.0"), firstLeave.getTotalHours());
        
        // Repository'nin doğru parametrelerle çağrıldığını kontrol et
        verify(employeeRepository, times(1)).findById(employeeId);
        verify(leaveRequestRepository, times(1)).findApprovedLeavesByDepartment(eq(departmentId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getTeamApprovedLeaves - Employee bulunamadığında EntityNotFoundException fırlatmalı")
    void getTeamApprovedLeaves_EmployeeNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange
        Long employeeId = 999L;
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());
        
        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            leaveRequestService.getTeamApprovedLeaves(employeeId);
        });
        
        assertTrue(exception.getMessage().contains("Çalışan bulunamadı ID: " + employeeId));
        verify(employeeRepository, times(1)).findById(employeeId);
        verify(leaveRequestRepository, never()).findApprovedLeavesByDepartment(any(), any());
    }

    @Test
    @DisplayName("getTeamApprovedLeaves - Departmanda onaylanmış izin yoksa boş liste döndürmeli")
    void getTeamApprovedLeaves_NoApprovedLeaves_ShouldReturnEmptyList() {
        // Arrange
        Long employeeId = 1L;
        Long departmentId = 10L;
        
        Department department = new Department();
        department.setId(departmentId);
        department.setName("IT Department");
        
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setDepartment(department);
        
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findApprovedLeavesByDepartment(eq(departmentId), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        
        // Act
        List<TeamLeaveResponseDTO> result = leaveRequestService.getTeamApprovedLeaves(employeeId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(employeeRepository, times(1)).findById(employeeId);
        verify(leaveRequestRepository, times(1)).findApprovedLeavesByDepartment(eq(departmentId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getTeamApprovedLeaves - DTO mapping doğru çalışmalı")
    void getTeamApprovedLeaves_DTOMapping_ShouldMapCorrectly() {
        // Arrange
        Long employeeId = 1L;
        Long departmentId = 10L;
        
        Department department = new Department();
        department.setId(departmentId);
        department.setName("Software Development");
        
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setDepartment(department);
        
        Employee teamMember = new Employee();
        teamMember.setId(2L);
        teamMember.setFirstName("Alice");
        teamMember.setLastName("Williams");
        teamMember.setDepartment(department);
        
        LeaveType leaveType = new LeaveType();
        leaveType.setId(1L);
        leaveType.setName("Mazeret İzni");
        
        LocalDateTime startDate = LocalDateTime.of(2024, 12, 25, 9, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 25, 17, 0);
        BigDecimal duration = new BigDecimal("8.0");
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(1L);
        leaveRequest.setEmployee(teamMember);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setRequestStatus(RequestStatus.APPROVED);
        leaveRequest.setStartDateTime(startDate);
        leaveRequest.setEndDateTime(endDate);
        leaveRequest.setDurationHours(duration);
        
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findApprovedLeavesByDepartment(eq(departmentId), any(LocalDateTime.class)))
                .thenReturn(List.of(leaveRequest));
        
        // Act
        List<TeamLeaveResponseDTO> result = leaveRequestService.getTeamApprovedLeaves(employeeId);
        
        // Assert
        assertEquals(1, result.size());
        TeamLeaveResponseDTO dto = result.get(0);
        assertEquals("Alice Williams", dto.getEmployeeFullName());
        assertEquals("Software Development", dto.getDepartmentName());
        assertEquals("Mazeret İzni", dto.getLeaveTypeName());
        assertEquals(startDate, dto.getStartDate());
        assertEquals(endDate, dto.getEndDate());
        assertEquals(duration, dto.getTotalHours());
    }
}


