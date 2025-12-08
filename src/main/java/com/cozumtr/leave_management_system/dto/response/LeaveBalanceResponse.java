package com.cozumtr.leave_management_system.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class LeaveBalanceResponse {
    private Long leaveTypeId; 
    private String leaveTypeName; 
    private Integer year; 
    private BigDecimal totalHours;
    private BigDecimal hoursUsed; 
    private BigDecimal remainingHours;
    private Integer totalDays;
    private Integer daysUsed;
    private Integer remainingDays;
}

