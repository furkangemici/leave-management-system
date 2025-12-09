package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.OverlappingLeaveDetailDTO;
import com.cozumtr.leave_management_system.dto.response.SprintOverlapReportDTO;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveRequestService - Sprint Overlap Report Unit Tests")
class LeaveRequestServiceSprintOverlapTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveCalculationService leaveCalculationService;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private Employee employee1;
    private Employee employee2;
    private LeaveType annualLeaveType;
    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Test Department");

        employee1 = new Employee();
        employee1.setId(1L);
        employee1.setFirstName("Ahmet");
        employee1.setLastName("Yılmaz");
        employee1.setEmail("ahmet@example.com");
        employee1.setDailyWorkHours(new BigDecimal("8.0"));
        employee1.setDepartment(department);

        employee2 = new Employee();
        employee2.setId(2L);
        employee2.setFirstName("Mehmet");
        employee2.setLastName("Demir");
        employee2.setEmail("mehmet@example.com");
        employee2.setDailyWorkHours(new BigDecimal("8.0"));
        employee2.setDepartment(department);

        annualLeaveType = new LeaveType();
        annualLeaveType.setId(1L);
        annualLeaveType.setName("Yıllık İzin");
        annualLeaveType.setRequestUnit(RequestUnit.DAY);
    }

    @Test
    @DisplayName("generateSprintOverlapReport - Çakışan izin bulunduğunda rapor oluşturulmalı")
    void generateSprintOverlapReport_WithOverlappingLeaves_ShouldReturnReport() {
        // Arrange
        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

        LeaveRequest leaveRequest = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 20, 23, 59),
                new BigDecimal("40.0")
        );

        List<LeaveRequest> overlappingLeaves = List.of(leaveRequest);

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(overlappingLeaves);

        // Çakışma aralığı: 15-20 Ocak (sprint içinde tamamen)
        LocalDate overlapStart = LocalDate.of(2024, 1, 15);
        LocalDate overlapEnd = LocalDate.of(2024, 1, 20);
        BigDecimal overlappingHours = new BigDecimal("40.0");

        when(leaveCalculationService.calculateDuration(
                eq(overlapStart),
                eq(overlapEnd),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(overlappingHours);

        // Act
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(overlappingHours, report.getTotalLossHours());
        assertEquals(1, report.getOverlappingLeaves().size());

        OverlappingLeaveDetailDTO detail = report.getOverlappingLeaves().get(0);
        assertEquals("Ahmet Yılmaz", detail.getEmployeeFullName());
        assertEquals("Yıllık İzin", detail.getLeaveTypeName());
        assertEquals(overlappingHours, detail.getOverlappingHours());
    }

    @Test
    @DisplayName("generateSprintOverlapReport - Çakışan izin yoksa boş rapor dönmeli")
    void generateSprintOverlapReport_NoOverlappingLeaves_ShouldReturnEmptyReport() {
        // Arrange
        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(new ArrayList<>());

        // Act
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(BigDecimal.ZERO, report.getTotalLossHours());
        assertTrue(report.getOverlappingLeaves().isEmpty());
    }

    @Test
    @DisplayName("generateSprintOverlapReport - Birden fazla çalışanın izinleri toplanmalı")
    void generateSprintOverlapReport_MultipleEmployees_ShouldSumTotalHours() {
        // Arrange
        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

        LeaveRequest leave1 = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 10, 0, 0),
                LocalDateTime.of(2024, 1, 12, 23, 59),
                new BigDecimal("24.0")
        );

        LeaveRequest leave2 = createLeaveRequest(
                employee2,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 17, 23, 59),
                new BigDecimal("24.0")
        );

        List<LeaveRequest> overlappingLeaves = List.of(leave1, leave2);

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(overlappingLeaves);

        // Her izin için çakışma saatini hesapla
        when(leaveCalculationService.calculateDuration(
                eq(LocalDate.of(2024, 1, 10)),
                eq(LocalDate.of(2024, 1, 12)),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(new BigDecimal("24.0"));

        when(leaveCalculationService.calculateDuration(
                eq(LocalDate.of(2024, 1, 15)),
                eq(LocalDate.of(2024, 1, 17)),
                eq(employee2.getDailyWorkHours())))
                .thenReturn(new BigDecimal("24.0"));

        // Act
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(new BigDecimal("48.0"), report.getTotalLossHours());
        assertEquals(2, report.getOverlappingLeaves().size());
    }

    @Test
    @DisplayName("generateSprintOverlapReport - Kısmen çakışan izin için doğru çakışma aralığı hesaplanmalı")
    void generateSprintOverlapReport_PartiallyOverlapping_ShouldCalculateCorrectOverlap() {
        // Arrange
        // Sprint: 1-31 Ocak
        // İzin: 25 Ocak - 5 Şubat → Çakışma: 25-31 Ocak
        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

        LeaveRequest leaveRequest = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 25, 0, 0),
                LocalDateTime.of(2024, 2, 5, 23, 59),
                new BigDecimal("80.0")
        );

        List<LeaveRequest> overlappingLeaves = List.of(leaveRequest);

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(overlappingLeaves);

        // Çakışma aralığı: MAX(25 Ocak, 1 Ocak) = 25 Ocak, MIN(5 Şubat, 31 Ocak) = 31 Ocak
        LocalDate overlapStart = LocalDate.of(2024, 1, 25);
        LocalDate overlapEnd = LocalDate.of(2024, 1, 31);
        BigDecimal overlappingHours = new BigDecimal("40.0"); // 25-31 Ocak arası

        when(leaveCalculationService.calculateDuration(
                eq(overlapStart),
                eq(overlapEnd),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(overlappingHours);

        // Act
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(overlappingHours, report.getTotalLossHours());
        assertEquals(1, report.getOverlappingLeaves().size());

        OverlappingLeaveDetailDTO detail = report.getOverlappingLeaves().get(0);
        assertEquals(overlappingHours, detail.getOverlappingHours());
    }

    @Test
    @DisplayName("generateSprintOverlapReport - İzin sprint'ten önce başlıyorsa çakışma başlangıcı sprint başlangıcı olmalı")
    void generateSprintOverlapReport_LeaveStartsBeforeSprint_ShouldUseSprintStart() {
        // Arrange
        // Sprint: 10-20 Ocak
        // İzin: 5-15 Ocak → Çakışma: 10-15 Ocak
        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 10, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 20, 23, 59);

        LeaveRequest leaveRequest = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 5, 0, 0),
                LocalDateTime.of(2024, 1, 15, 23, 59),
                new BigDecimal("80.0")
        );

        List<LeaveRequest> overlappingLeaves = List.of(leaveRequest);

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(overlappingLeaves);

        // Çakışma: MAX(5 Ocak, 10 Ocak) = 10 Ocak, MIN(15 Ocak, 20 Ocak) = 15 Ocak
        LocalDate overlapStart = LocalDate.of(2024, 1, 10);
        LocalDate overlapEnd = LocalDate.of(2024, 1, 15);
        BigDecimal overlappingHours = new BigDecimal("40.0");

        when(leaveCalculationService.calculateDuration(
                eq(overlapStart),
                eq(overlapEnd),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(overlappingHours);

        // Act
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(overlappingHours, report.getTotalLossHours());
    }

    @Test
    @DisplayName("generateSprintOverlapReport - İzin sprint'ten sonra bitiyorsa çakışma bitişi sprint bitişi olmalı")
    void generateSprintOverlapReport_LeaveEndsAfterSprint_ShouldUseSprintEnd() {
        // Arrange
        // Sprint: 10-20 Ocak
        // İzin: 15-25 Ocak → Çakışma: 15-20 Ocak
        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 10, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 20, 23, 59);

        LeaveRequest leaveRequest = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 25, 23, 59),
                new BigDecimal("80.0")
        );

        List<LeaveRequest> overlappingLeaves = List.of(leaveRequest);

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(overlappingLeaves);

        // Çakışma: MAX(15 Ocak, 10 Ocak) = 15 Ocak, MIN(25 Ocak, 20 Ocak) = 20 Ocak
        LocalDate overlapStart = LocalDate.of(2024, 1, 15);
        LocalDate overlapEnd = LocalDate.of(2024, 1, 20);
        BigDecimal overlappingHours = new BigDecimal("40.0");

        when(leaveCalculationService.calculateDuration(
                eq(overlapStart),
                eq(overlapEnd),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(overlappingHours);

        // Act
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(overlappingHours, report.getTotalLossHours());
    }

    @Test
    @DisplayName("generateSprintOverlapReport - Her çalışanın dailyWorkHours değeri kullanılmalı")
    void generateSprintOverlapReport_DifferentDailyWorkHours_ShouldUseEmployeeSpecificHours() {
        // Arrange
        employee1.setDailyWorkHours(new BigDecimal("8.0"));
        employee2.setDailyWorkHours(new BigDecimal("6.0")); // Farklı çalışma saati

        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 31, 23, 59);

        LeaveRequest leave1 = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 10, 0, 0),
                LocalDateTime.of(2024, 1, 12, 23, 59),
                new BigDecimal("24.0")
        );

        LeaveRequest leave2 = createLeaveRequest(
                employee2,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 17, 23, 59),
                new BigDecimal("18.0")
        );

        List<LeaveRequest> overlappingLeaves = List.of(leave1, leave2);

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(overlappingLeaves);

        // Employee1 için 8 saat/gün
        when(leaveCalculationService.calculateDuration(
                eq(LocalDate.of(2024, 1, 10)),
                eq(LocalDate.of(2024, 1, 12)),
                eq(new BigDecimal("8.0"))))
                .thenReturn(new BigDecimal("24.0"));

        // Employee2 için 6 saat/gün
        when(leaveCalculationService.calculateDuration(
                eq(LocalDate.of(2024, 1, 15)),
                eq(LocalDate.of(2024, 1, 17)),
                eq(new BigDecimal("6.0"))))
                .thenReturn(new BigDecimal("18.0"));

        // Act
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(new BigDecimal("42.0"), report.getTotalLossHours());
        
        // Her çalışanın kendi dailyWorkHours değeri kullanıldığını doğrula
        verify(leaveCalculationService).calculateDuration(
                any(LocalDate.class),
                any(LocalDate.class),
                eq(new BigDecimal("8.0")));
        verify(leaveCalculationService).calculateDuration(
                any(LocalDate.class),
                any(LocalDate.class),
                eq(new BigDecimal("6.0")));
    }

    @Test
    @DisplayName("generateSprintOverlapReport - İzin iki sprint'e yayılıyorsa her sprint için sadece o sprint ile çakışan kısım hesaplanmalı")
    void generateSprintOverlapReport_LeaveSpansTwoSprints_ShouldCalculateOnlyOverlappingPart() {
        // Senaryo: İzin Sprint 1'in son haftası + Sprint 2'nin ikinci haftası
        // Sprint 1: 1-31 Ocak
        // Sprint 2: 1-29 Şubat
        // İzin: 25 Ocak - 10 Şubat

        // SPRINT 1 İÇİN RAPOR
        LocalDateTime sprint1Start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime sprint1End = LocalDateTime.of(2024, 1, 31, 23, 59);

        LeaveRequest leaveRequest = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 25, 0, 0),  // İzin başlangıcı: 25 Ocak
                LocalDateTime.of(2024, 2, 10, 23, 59), // İzin bitişi: 10 Şubat
                new BigDecimal("120.0") // Toplam izin süresi
        );

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprint1Start, sprint1End))
                .thenReturn(List.of(leaveRequest));

        // Sprint 1 için çakışma: MAX(25 Ocak, 1 Ocak) = 25 Ocak, MIN(10 Şubat, 31 Ocak) = 31 Ocak
        // Sadece 25-31 Ocak arası hesaplanmalı (7 gün)
        LocalDate overlapStartSprint1 = LocalDate.of(2024, 1, 25);
        LocalDate overlapEndSprint1 = LocalDate.of(2024, 1, 31);
        BigDecimal overlappingHoursSprint1 = new BigDecimal("56.0"); // 7 gün × 8 saat

        when(leaveCalculationService.calculateDuration(
                eq(overlapStartSprint1),
                eq(overlapEndSprint1),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(overlappingHoursSprint1);

        SprintOverlapReportDTO reportSprint1 = leaveRequestService.generateSprintOverlapReport(sprint1Start, sprint1End);

        // Assert Sprint 1
        assertNotNull(reportSprint1);
        assertEquals(overlappingHoursSprint1, reportSprint1.getTotalLossHours());
        assertEquals(1, reportSprint1.getOverlappingLeaves().size());
        assertEquals(overlappingHoursSprint1, reportSprint1.getOverlappingLeaves().get(0).getOverlappingHours());

        // SPRINT 2 İÇİN RAPOR
        LocalDateTime sprint2Start = LocalDateTime.of(2024, 2, 1, 0, 0);
        LocalDateTime sprint2End = LocalDateTime.of(2024, 2, 29, 23, 59);

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprint2Start, sprint2End))
                .thenReturn(List.of(leaveRequest));

        // Sprint 2 için çakışma: MAX(25 Ocak, 1 Şubat) = 1 Şubat, MIN(10 Şubat, 29 Şubat) = 10 Şubat
        // Sadece 1-10 Şubat arası hesaplanmalı (10 gün)
        LocalDate overlapStartSprint2 = LocalDate.of(2024, 2, 1);
        LocalDate overlapEndSprint2 = LocalDate.of(2024, 2, 10);
        BigDecimal overlappingHoursSprint2 = new BigDecimal("80.0"); // 10 gün × 8 saat

        when(leaveCalculationService.calculateDuration(
                eq(overlapStartSprint2),
                eq(overlapEndSprint2),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(overlappingHoursSprint2);

        SprintOverlapReportDTO reportSprint2 = leaveRequestService.generateSprintOverlapReport(sprint2Start, sprint2End);

        // Assert Sprint 2
        assertNotNull(reportSprint2);
        assertEquals(overlappingHoursSprint2, reportSprint2.getTotalLossHours());
        assertEquals(1, reportSprint2.getOverlappingLeaves().size());
        assertEquals(overlappingHoursSprint2, reportSprint2.getOverlappingLeaves().get(0).getOverlappingHours());

        // ÖNEMLİ: Her sprint için sadece kendi çakışan kısmı hesaplanmalı
        // Sprint 1: 56 saat (25-31 Ocak)
        // Sprint 2: 80 saat (1-10 Şubat)
        // Toplam: 136 saat (ama her sprint kendi kısmını gösterir)
        assertNotEquals(reportSprint1.getTotalLossHours(), reportSprint2.getTotalLossHours());
    }

    @Test
    @DisplayName("generateSprintOverlapReport - İzin sprint'ten önce başlayıp sprint içinde bitiyorsa sadece sprint içindeki kısım hesaplanmalı")
    void generateSprintOverlapReport_LeaveStartsBeforeSprint_ShouldCalculateOnlySprintPart() {
        // Senaryo: İzin sprint'ten önce başlıyor
        // Sprint: 10-20 Ocak
        // İzin: 5-15 Ocak → Çakışma: 10-15 Ocak (sadece sprint içindeki kısım)

        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 10, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 20, 23, 59);

        LeaveRequest leaveRequest = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 5, 0, 0),   // İzin başlangıcı: 5 Ocak (sprint'ten önce)
                LocalDateTime.of(2024, 1, 15, 23, 59), // İzin bitişi: 15 Ocak (sprint içinde)
                new BigDecimal("80.0")
        );

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(List.of(leaveRequest));

        // Çakışma: MAX(5 Ocak, 10 Ocak) = 10 Ocak, MIN(15 Ocak, 20 Ocak) = 15 Ocak
        // Sadece 10-15 Ocak arası hesaplanmalı (6 gün)
        LocalDate overlapStart = LocalDate.of(2024, 1, 10);
        LocalDate overlapEnd = LocalDate.of(2024, 1, 15);
        BigDecimal overlappingHours = new BigDecimal("48.0"); // 6 gün × 8 saat

        when(leaveCalculationService.calculateDuration(
                eq(overlapStart),
                eq(overlapEnd),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(overlappingHours);

        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(overlappingHours, report.getTotalLossHours());
        // ÖNEMLİ: İzin 5-15 Ocak ama sadece 10-15 Ocak (6 gün) hesaplanmalı
        assertEquals(new BigDecimal("48.0"), report.getTotalLossHours());
    }

    @Test
    @DisplayName("generateSprintOverlapReport - İzin sprint içinde başlayıp sprint'ten sonra bitiyorsa sadece sprint içindeki kısım hesaplanmalı")
    void generateSprintOverlapReport_LeaveEndsAfterSprint_ShouldCalculateOnlySprintPart() {
        // Senaryo: İzin sprint'ten sonra bitiyor
        // Sprint: 10-20 Ocak
        // İzin: 15-25 Ocak → Çakışma: 15-20 Ocak (sadece sprint içindeki kısım)

        LocalDateTime sprintStart = LocalDateTime.of(2024, 1, 10, 0, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2024, 1, 20, 23, 59);

        LeaveRequest leaveRequest = createLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 15, 0, 0),   // İzin başlangıcı: 15 Ocak (sprint içinde)
                LocalDateTime.of(2024, 1, 25, 23, 59), // İzin bitişi: 25 Ocak (sprint'ten sonra)
                new BigDecimal("80.0")
        );

        when(leaveRequestRepository.findOverlappingApprovedLeaves(sprintStart, sprintEnd))
                .thenReturn(List.of(leaveRequest));

        // Çakışma: MAX(15 Ocak, 10 Ocak) = 15 Ocak, MIN(25 Ocak, 20 Ocak) = 20 Ocak
        // Sadece 15-20 Ocak arası hesaplanmalı (6 gün)
        LocalDate overlapStart = LocalDate.of(2024, 1, 15);
        LocalDate overlapEnd = LocalDate.of(2024, 1, 20);
        BigDecimal overlappingHours = new BigDecimal("48.0"); // 6 gün × 8 saat

        when(leaveCalculationService.calculateDuration(
                eq(overlapStart),
                eq(overlapEnd),
                eq(employee1.getDailyWorkHours())))
                .thenReturn(overlappingHours);

        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(sprintStart, sprintEnd);

        // Assert
        assertNotNull(report);
        assertEquals(overlappingHours, report.getTotalLossHours());
        // ÖNEMLİ: İzin 15-25 Ocak ama sadece 15-20 Ocak (6 gün) hesaplanmalı
        assertEquals(new BigDecimal("48.0"), report.getTotalLossHours());
    }

    // ========== HELPER METODLAR ==========

    private LeaveRequest createLeaveRequest(Employee employee, LocalDateTime start, LocalDateTime end, BigDecimal duration) {
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(1L);
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(annualLeaveType);
        leaveRequest.setStartDateTime(start);
        leaveRequest.setEndDateTime(end);
        leaveRequest.setDurationHours(duration);
        leaveRequest.setRequestStatus(RequestStatus.APPROVED);
        leaveRequest.setWorkflowNextApproverRole("");
        leaveRequest.setReason("Test");
        return leaveRequest;
    }
}

