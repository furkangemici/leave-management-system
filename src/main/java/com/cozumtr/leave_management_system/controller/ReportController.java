package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.response.SprintOverlapDto;
import com.cozumtr.leave_management_system.service.LeaveReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final LeaveReportService  leaveReportService;

    // URL Örneği:
    // /api/v1/reports/sprint-overlap?start=2025-12-01T09:00:00&end=2025-12-15T18:00:00
    @GetMapping("/sprint-overlap")
    public ResponseEntity<List<SprintOverlapDto>> getSprintOverlapReport(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(leaveReportService.getSprintOverlapReport(start, end));
    }
}
