package com.cozumtr.leave_management_system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LeaveTimelineDto {

    private LocalDateTime actionDate; //işlem zamanı=created_at
    private String actorName; //işlemi yapan kişi
    private String actorJobTitle; // işlemi yapanın unvanı
    private String actionType; //işlem tipi
    private String comments; // varsa yazılan açıklama/not
}
