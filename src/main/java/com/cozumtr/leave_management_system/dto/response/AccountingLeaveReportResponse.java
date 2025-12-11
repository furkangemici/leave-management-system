package com.cozumtr.leave_management_system.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AccountingLeaveReportResponse {
    private List<AccountingLeaveReportRow> rows;
}

