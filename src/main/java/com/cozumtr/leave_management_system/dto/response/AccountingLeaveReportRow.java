package com.cozumtr.leave_management_system.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AccountingLeaveReportRow {
    private Long leaveRequestId;
    private String employeeFullName;
    private String departmentName;
    private String leaveTypeName;
    private boolean paid;
    private boolean deductsFromAnnual;
    private boolean documentRequired;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal durationHours;
    private String status;
    private int attachmentCount;
}

