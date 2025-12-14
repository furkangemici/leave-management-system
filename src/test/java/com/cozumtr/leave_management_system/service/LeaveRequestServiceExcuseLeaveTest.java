package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.LeaveEntitlementRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
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
@DisplayName("LeaveRequestService - Mazeret İzni Kuralları Unit Tests")
class LeaveRequestServiceExcuseLeaveTest {

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
    private PublicHolidayRepository publicHolidayRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private Employee testEmployee;
    private LeaveType excuseLeaveType;
    private CreateLeaveRequest createRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");

        // Test Employee
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmail("test@example.com");
        testEmployee.setFirstName("Test");
        testEmployee.setLastName("User");
        testEmployee.setDailyWorkHours(new BigDecimal("8.0"));

        // Mazeret İzni Türü
        excuseLeaveType = new LeaveType();
        excuseLeaveType.setId(2L);
        excuseLeaveType.setName("Mazeret İzni (Saatlik)");
        excuseLeaveType.setIsActive(true);
        excuseLeaveType.setDeductsFromAnnual(false);
        excuseLeaveType.setRequestUnit(RequestUnit.HOUR); // Saatlik izin
        excuseLeaveType.setWorkflowDefinition("MANAGER");

        // Test Request
        createRequest = new CreateLeaveRequest();
        createRequest.setLeaveTypeId(2L);
        createRequest.setStartDate(LocalDateTime.of(2025, 1, 15, 9, 0));
        createRequest.setEndDate(LocalDateTime.of(2025, 1, 15, 11, 0)); // 2 saat
        createRequest.setReason("Doktor randevusu");
    }

    @Test
    @DisplayName("Mazeret izni tam 2 saat olmalı - 2 saat başarılı")
    void createLeaveRequest_ExcuseLeave_2Hours_ShouldSucceed() {
        // Given
        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(2L))
                .thenReturn(Optional.of(excuseLeaveType));
        // Saatlik izinler için PublicHolidayRepository mock'u (hafta sonu ve tatil kontrolü)
        when(publicHolidayRepository.existsByDateInRange(any(LocalDate.class)))
                .thenReturn(false); // Tatil değil
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveRequestRepository.calculateMonthlyUsageByLeaveType(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO); // Henüz kullanılmamış
        when(leaveRequestRepository.countMonthlyUsageByLeaveType(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(0L); // Henüz kullanılmamış
        when(leaveRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertDoesNotThrow(() -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        verify(leaveRequestRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Mazeret izni 2 saatten farklı olursa hata fırlatılmalı")
    void createLeaveRequest_ExcuseLeave_Not2Hours_ShouldThrowException() {
        // Given
        createRequest.setEndDate(LocalDateTime.of(2025, 1, 15, 12, 0)); // 3 saat

        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(2L))
                .thenReturn(Optional.of(excuseLeaveType));
        // Saatlik izinler için PublicHolidayRepository mock'u (hafta sonu ve tatil kontrolü)
        when(publicHolidayRepository.existsByDateInRange(any(LocalDate.class)))
                .thenReturn(false); // Tatil değil
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("sadece 2 saat olarak alınabilir"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mazeret izni ayda 4 kereden fazla alınamaz")
    void createLeaveRequest_ExcuseLeave_MoreThan4Times_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(2L))
                .thenReturn(Optional.of(excuseLeaveType));
        // Saatlik izinler için PublicHolidayRepository mock'u (hafta sonu ve tatil kontrolü)
        when(publicHolidayRepository.existsByDateInRange(any(LocalDate.class)))
                .thenReturn(false); // Tatil değil
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveRequestRepository.countMonthlyUsageByLeaveType(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(4L); // Zaten 4 kere alınmış

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("sayısı limiti aşıldı"));
        assertTrue(exception.getMessage().contains("4 kere"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mazeret izni aylık 8 saat limiti aşıldığında hata fırlatılmalı")
    void createLeaveRequest_ExcuseLeave_Exceeds8HoursLimit_ShouldThrowException() {
        // Given
        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(2L))
                .thenReturn(Optional.of(excuseLeaveType));
        // Saatlik izinler için PublicHolidayRepository mock'u (hafta sonu ve tatil kontrolü)
        when(publicHolidayRepository.existsByDateInRange(any(LocalDate.class)))
                .thenReturn(false); // Tatil değil
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveRequestRepository.countMonthlyUsageByLeaveType(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(3L); // 3 kere alınmış (6 saat kullanılmış)
        when(leaveRequestRepository.calculateMonthlyUsageByLeaveType(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("7.0")); // 7 saat kullanılmış, 1 saat kaldı

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("saat limiti aşıldı"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mazeret izni 4 kere 2 saat = 8 saat toplam başarılı")
    void createLeaveRequest_ExcuseLeave_4Times2Hours_Success() {
        // Given - 4. mazeret izni talebi (önceki 3 tanesi zaten alınmış)
        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(2L))
                .thenReturn(Optional.of(excuseLeaveType));
        // Saatlik izinler için PublicHolidayRepository mock'u (hafta sonu ve tatil kontrolü)
        when(publicHolidayRepository.existsByDateInRange(any(LocalDate.class)))
                .thenReturn(false); // Tatil değil
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveRequestRepository.countMonthlyUsageByLeaveType(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(3L); // 3 kere alınmış
        when(leaveRequestRepository.calculateMonthlyUsageByLeaveType(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("6.0")); // 6 saat kullanılmış, 2 saat kaldı
        when(leaveRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertDoesNotThrow(() -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        verify(leaveRequestRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Mazeret izni bakiye kontrolü - kullanım sayısı ve saat kontrolü birlikte")
    void createLeaveRequest_ExcuseLeave_BothCountAndHoursCheck() {
        // Given - 4. mazeret izni, 8 saat dolmuş
        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(2L))
                .thenReturn(Optional.of(excuseLeaveType));
        // Saatlik izinler için PublicHolidayRepository mock'u (hafta sonu ve tatil kontrolü)
        when(publicHolidayRepository.existsByDateInRange(any(LocalDate.class)))
                .thenReturn(false); // Tatil değil
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveRequestRepository.countMonthlyUsageByLeaveType(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(4L); // Zaten 4 kere alınmış (önce bu kontrol edilir)

        // When & Then - Sayı limiti aşıldığı için hata
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("sayısı limiti"));
        verify(leaveRequestRepository, never()).save(any());
    }
}


