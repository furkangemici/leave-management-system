package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.LeaveTimelineDto;
import com.cozumtr.leave_management_system.entities.LeaveApprovalHistory;
import com.cozumtr.leave_management_system.repository.LeaveApprovalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveApprovalHistoryRepository leaveApprovalHistoryRepository;


     //Bir izin talebinin başından geçen olayları kronolojik sırada getirir.
    @Transactional(readOnly = true)
    public List<LeaveTimelineDto> getRequestTimeline(Long requestId) {

        // veritabanından tarihçeyi çekiyoruz
        List<LeaveApprovalHistory> historyList = leaveApprovalHistoryRepository
                .findByLeaveRequestIdOrderByCreatedAtAsc(requestId);

        //entity listesini dto listesine dönüştürme
        return historyList.stream()
                .map(this::mapToTimelineDto)
                .collect(Collectors.toList());

     }

     //yardımcı dönüştürücü metot
    private LeaveTimelineDto mapToTimelineDto(LeaveApprovalHistory history) {
        //olası null durumları için kontrol
        if (history.getApprover() == null) {
            return LeaveTimelineDto.builder()
                    .actionDate(history.getCreatedAt())
                    .actionType(history.getAction().name())
                    .actorName("Sistem / Bilinmeyen")
                    .build();
        }

        //isim soyisim birleştirme
        String fullName = history.getApprover().getFirstName() + " " + history.getApprover().getLastName();

        return LeaveTimelineDto.builder()
                .actionDate(history.getCreatedAt())
                .actorName(fullName)
                .actorJobTitle(history.getApprover().getJobTitle())
                .actionType(history.getAction().name())
                .comments(history.getComments())
                .build();
    }



}
