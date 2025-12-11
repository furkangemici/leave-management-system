package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.AccountingReportRequest;
import com.cozumtr.leave_management_system.dto.response.AccountingLeaveReportResponse;
import com.cozumtr.leave_management_system.service.AccountingReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/accounting")
@RequiredArgsConstructor
public class AccountingReportController {

    private final AccountingReportService accountingReportService;

    @PreAuthorize("hasRole('ACCOUNTING')")
    @PostMapping("/leaves")
    public ResponseEntity<AccountingLeaveReportResponse> getReport(@Valid @RequestBody AccountingReportRequest request) {
        AccountingLeaveReportResponse response = accountingReportService.getReport(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ACCOUNTING')")
    @PostMapping(value = "/leaves/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportReport(@Valid @RequestBody AccountingReportRequest request) {
        byte[] content = accountingReportService.exportReport(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leave-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}

