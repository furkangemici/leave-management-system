package com.cozumtr.leave_management_system.dto.response;

import com.cozumtr.leave_management_system.enums.RequestStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder // Nesne oluşturmayı kolaylaştırır
public class LeaveRequestResponse {
    private Long id;
    private String leaveTypeName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal duration; // Hesaplanan gün sayısı
    private RequestStatus status; // PENDING, APPROVED vb.
    private LocalDateTime createdAt;
}