package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import com.cozumtr.leave_management_system.enums.ReportType;

@Getter
@Setter
public class AccountingReportRequest {
    @NotNull
    private LocalDateTime startDate;
    @NotNull
    private LocalDateTime endDate;
    // UNPAID, DOCUMENT_REQUIRED, ALL
    @NotNull
    private ReportType type;
    private Long departmentId;
    private Long employeeId;
}

