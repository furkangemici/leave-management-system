package com.cozumtr.leave_management_system.dto.response;

import com.cozumtr.leave_management_system.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApprovalHistoryDTO {
    private String approverFullName;
    private RequestStatus action;
    private String comments;
    private LocalDateTime actionDate;
}

