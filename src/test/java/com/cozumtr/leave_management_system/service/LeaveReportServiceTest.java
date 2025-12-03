package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.SprintOverlapDto;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LeaveReportServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private LeaveReportService leaveReportService;

    @Test
    public void getSprintOverlapReport_ShouldCalculatePartialOverlapCorrectly() {
        // 1. HAZIRLIK (Senaryo)

        // Sprint: 10 Haziran 09:00 - 20 Haziran 18:00
        LocalDateTime sprintStart = LocalDateTime.of(2025, 6, 10, 9, 0);
        LocalDateTime sprintEnd = LocalDateTime.of(2025, 6, 20, 18, 0);

        // İzin: 8 Haziran (Sprint'ten önce) başlar, 12 Haziran'da biter.
        // Beklenti: Sadece 10 Haziran 09:00 ile 12 Haziran 18:00 arasını almalı.
        // Fark: 2 Gün + 9 Saat = 57 Saat (Basit hesap)
        LocalDateTime leaveStart = LocalDateTime.of(2025, 6, 8, 9, 0);
        LocalDateTime leaveEnd = LocalDateTime.of(2025, 6, 12, 18, 0);

        // Dummy Verileri Oluştur (Null Pointer Yememek için)
        Department dept = new Department(); dept.setName("Yazılım");
        Employee emp = new Employee(); emp.setFirstName("Furkan"); emp.setLastName("Gemici"); emp.setDepartment(dept);
        LeaveType type = new LeaveType(); type.setName("Yıllık İzin");

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(emp);
        request.setLeaveType(type);
        request.setStartDateTime(leaveStart);
        request.setEndDateTime(leaveEnd);
        request.setRequestStatus(RequestStatus.APPROVED);

        // Repository Mock'la
        when(leaveRequestRepository.findOverlappingLeavesForReport(sprintStart, sprintEnd, RequestStatus.APPROVED))
                .thenReturn(Collections.singletonList(request));

        // 2. EYLEM
        List<SprintOverlapDto> result = leaveReportService.getSprintOverlapReport(sprintStart, sprintEnd);

        // 3. KONTROL
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());

        SprintOverlapDto dto = result.get(0);

        // İsim kontrolü
        Assertions.assertEquals("Furkan Gemici", dto.getEmployeeName());

        // En Önemli Kısım: Saat Hesabı
        // 10 Haziran 09:00 -> 12 Haziran 18:00 arası kaç saat?
        // 10->11 (24s), 11->12 (24s), 12 09:00 -> 12 18:00 (9s) = 57 saat.
        Assertions.assertEquals(57, dto.getOverlapHours());

        // Tarihlerin kırpıldığını (veya orijinalinin korunduğunu) kontrol edebilirsin
        // Bizim servis kodumuzda DTO'ya orijinal tarihleri basmıştık, hesaplamayı kırpmıştık.
        // Bu yüzden overlapHours kritik.
    }
}