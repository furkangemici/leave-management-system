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
@DisplayName("LeaveRequestService - Saatlik İzin Hafta Sonu ve Tatil Kontrolü Unit Tests")
class LeaveRequestServiceHourlyLeaveValidationTest {

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
    private LeaveType hourlyLeaveType;
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

        // Saatlik İzin Türü
        hourlyLeaveType = new LeaveType();
        hourlyLeaveType.setId(3L);
        hourlyLeaveType.setName("Saatlik İzin");
        hourlyLeaveType.setIsActive(true);
        hourlyLeaveType.setDeductsFromAnnual(false);
        hourlyLeaveType.setRequestUnit(RequestUnit.HOUR); // Saatlik izin
        hourlyLeaveType.setWorkflowDefinition("MANAGER");

        // Test Request
        createRequest = new CreateLeaveRequest();
        createRequest.setLeaveTypeId(3L);
        createRequest.setStartDate(LocalDateTime.of(2025, 1, 15, 9, 0)); // Çarşamba
        createRequest.setEndDate(LocalDateTime.of(2025, 1, 15, 13, 0)); // 4 saat
        createRequest.setReason("Kişisel işler");
    }

    @Test
    @DisplayName("createLeaveRequest_HourlyLeave_WeekendStartDate_ShouldThrowException - Saatlik izin başlangıç tarihi hafta sonu ise hata fırlatılmalı")
    void createLeaveRequest_HourlyLeave_WeekendStartDate_ShouldThrowException() {
        // Given: Cumartesi günü başlangıç tarihi
        createRequest.setStartDate(LocalDateTime.of(2025, 1, 18, 9, 0)); // Cumartesi
        createRequest.setEndDate(LocalDateTime.of(2025, 1, 18, 13, 0)); // Aynı gün

        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(3L))
                .thenReturn(Optional.of(hourlyLeaveType));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("hafta sonu günlerinde alınamaz"));
        verify(publicHolidayRepository, never()).existsByDateInRange(any());
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLeaveRequest_HourlyLeave_WeekendEndDate_ShouldThrowException - Saatlik izin bitiş tarihi hafta sonu ise hata fırlatılmalı")
    void createLeaveRequest_HourlyLeave_WeekendEndDate_ShouldThrowException() {
        // Given: Pazar günü bitiş tarihi
        createRequest.setStartDate(LocalDateTime.of(2025, 1, 17, 9, 0)); // Cuma
        createRequest.setEndDate(LocalDateTime.of(2025, 1, 19, 13, 0)); // Pazar

        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(3L))
                .thenReturn(Optional.of(hourlyLeaveType));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("hafta sonu günlerinde alınamaz"));
        verify(publicHolidayRepository, never()).existsByDateInRange(any());
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLeaveRequest_HourlyLeave_PublicHolidayStartDate_ShouldThrowException - Saatlik izin başlangıç tarihi resmi tatil ise hata fırlatılmalı")
    void createLeaveRequest_HourlyLeave_PublicHolidayStartDate_ShouldThrowException() {
        // Given: Resmi tatil günü başlangıç tarihi
        LocalDate holidayDate = LocalDate.of(2025, 1, 15); // Çarşamba (örnek tatil günü)
        createRequest.setStartDate(LocalDateTime.of(2025, 1, 15, 9, 0));
        createRequest.setEndDate(LocalDateTime.of(2025, 1, 15, 13, 0));

        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(3L))
                .thenReturn(Optional.of(hourlyLeaveType));
        when(publicHolidayRepository.existsByDateInRange(holidayDate))
                .thenReturn(true); // Resmi tatil

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("resmi tatil günlerinde alınamaz"));
        verify(publicHolidayRepository, atLeastOnce()).existsByDateInRange(holidayDate);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLeaveRequest_HourlyLeave_PublicHolidayEndDate_ShouldThrowException - Saatlik izin bitiş tarihi resmi tatil ise hata fırlatılmalı")
    void createLeaveRequest_HourlyLeave_PublicHolidayEndDate_ShouldThrowException() {
        // Given: Farklı günler, bitiş tarihi resmi tatil
        LocalDate startDate = LocalDate.of(2025, 1, 14); // Salı
        LocalDate holidayDate = LocalDate.of(2025, 1, 15); // Çarşamba (resmi tatil)
        createRequest.setStartDate(LocalDateTime.of(2025, 1, 14, 9, 0));
        createRequest.setEndDate(LocalDateTime.of(2025, 1, 15, 13, 0));

        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(3L))
                .thenReturn(Optional.of(hourlyLeaveType));
        when(publicHolidayRepository.existsByDateInRange(startDate))
                .thenReturn(false); // Başlangıç tatil değil
        when(publicHolidayRepository.existsByDateInRange(holidayDate))
                .thenReturn(true); // Bitiş resmi tatil

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        assertTrue(exception.getMessage().contains("resmi tatil günlerinde alınamaz"));
        verify(publicHolidayRepository).existsByDateInRange(startDate);
        verify(publicHolidayRepository).existsByDateInRange(holidayDate);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLeaveRequest_HourlyLeave_ValidWorkingDay_ShouldSucceed - Geçerli çalışma gününde saatlik izin başarılı olmalı")
    void createLeaveRequest_HourlyLeave_ValidWorkingDay_ShouldSucceed() {
        // Given: Normal çalışma günü (Çarşamba)
        LocalDate workingDate = LocalDate.of(2025, 1, 15); // Çarşamba
        createRequest.setStartDate(LocalDateTime.of(2025, 1, 15, 9, 0));
        createRequest.setEndDate(LocalDateTime.of(2025, 1, 15, 13, 0)); // 4 saat

        when(employeeRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findById(3L))
                .thenReturn(Optional.of(hourlyLeaveType));
        when(publicHolidayRepository.existsByDateInRange(workingDate))
                .thenReturn(false); // Tatil değil
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(false);
        when(leaveRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertDoesNotThrow(() -> {
            leaveRequestService.createLeaveRequest(createRequest);
        });

        verify(publicHolidayRepository).existsByDateInRange(workingDate);
        verify(leaveRequestRepository, times(1)).save(any());
    }
}

