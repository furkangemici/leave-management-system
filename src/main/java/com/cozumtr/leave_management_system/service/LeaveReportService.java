package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.SprintOverlapDto;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveReportService {

    private final LeaveRequestRepository leaveRequestRepository;

    // Sprint Başlangıç ve Bitiş tarihlerini alıp rapor döner
    public List<SprintOverlapDto> getSprintOverlapReport(LocalDateTime sprintStart,  LocalDateTime sprintEnd ) {
        // Veritabanından, bu tarih aralığına denk gelen ONAYLI izinleri çek
        List<LeaveRequest> overlappingLeaves = leaveRequestRepository.findOverlappingLeavesForReport(
           sprintStart, sprintEnd, RequestStatus.APPROVED
        );

        //Her bir izin kaydını, DTO'ya dönüştür (Hesaplama yaparak)
        return overlappingLeaves.stream()
                .map(request -> calculateAndMapToDto(request, sprintStart,sprintEnd))
                .collect(Collectors.toList());
    }

    private SprintOverlapDto calculateAndMapToDto(LeaveRequest request, LocalDateTime sprintStart, LocalDateTime sprintEnd) {

        // Kesişim Başlangıcı: İzin mi daha geç başlıyor, Sprint mi? (Hangisi büyükse onu al)
        LocalDateTime overlapStart = request.getStartDateTime().isAfter(sprintStart)
                ? request.getStartDateTime()
                : sprintStart;

        // Kesişim Bitişi: İzin mi daha erken bitiyor, Sprint mi? (Hangisi küçükse onu al)
        LocalDateTime overlapEnd = request.getEndDateTime().isBefore(sprintEnd)
                ? request.getEndDateTime()
                : sprintEnd;

        // İki tarih arasındaki saati bul
        long hoursLost = Duration.between(overlapStart, overlapEnd).toHours();

        // Güvenlik önlemi: Eğer aynı dakikaya denk gelirse en az 1 saat göster
        if (hoursLost <= 0) hoursLost = 1;

        // İsim birleştirme
        String fullName = request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName();

        return SprintOverlapDto.builder()
                .employeeName(fullName)
                // NullPointerException yememek için departman kontrolü yapılabilir ama şimdilik varsayıyoruz
                .departmentName(request.getEmployee().getDepartment().getName())
                .leaveType(request.getLeaveType().getName()) // İzin türü adı
                .startDate(request.getStartDateTime())
                .endDate(request.getEndDateTime())
                .overlapHours(hoursLost)
                .build();
    }
}
