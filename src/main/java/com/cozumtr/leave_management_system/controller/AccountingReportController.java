package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.AccountingReportRequest;
import com.cozumtr.leave_management_system.dto.response.AccountingLeaveReportResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.service.AccountingReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/accounting")
@RequiredArgsConstructor
public class AccountingReportController {

    private final AccountingReportService accountingReportService;
    private final EmployeeRepository employeeRepository;

    /**
     * Muhasebe raporu endpoint'i.
     * Sadece ACCOUNTING rolü olanlar veya Muhasebe/Finans departmanındaki MANAGER'lar erişebilir.
     */
    @PreAuthorize("hasAnyRole('ACCOUNTING', 'MANAGER')")
    @PostMapping("/leaves")
    public ResponseEntity<AccountingLeaveReportResponse> getReport(@Valid @RequestBody AccountingReportRequest request) {
        // Eğer sadece MANAGER rolü varsa, departman kontrolü yap
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));

        boolean hasAccountingRole = employee.getUser().getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("ACCOUNTING"));

        // Eğer ACCOUNTING rolü yoksa, departman kontrolü yap
        if (!hasAccountingRole) {
            if (employee.getDepartment() == null) {
                throw new BusinessException("Bu rapora erişim yetkiniz yok");
            }
            String deptName = employee.getDepartment().getName();
            if (!deptName.equals("Muhasebe") && !deptName.equals("Finans")) {
                throw new BusinessException("Bu rapora erişim yetkiniz yok. Sadece Muhasebe ve Finans departmanları erişebilir.");
            }
        }

        AccountingLeaveReportResponse response = accountingReportService.getReport(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ACCOUNTING', 'MANAGER')")
    @PostMapping(value = "/leaves/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportReport(@Valid @RequestBody AccountingReportRequest request) {
        // Eğer sadece MANAGER rolü varsa, departman kontrolü yap
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));

        boolean hasAccountingRole = employee.getUser().getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("ACCOUNTING"));

        // Eğer ACCOUNTING rolü yoksa, departman kontrolü yap
        if (!hasAccountingRole) {
            if (employee.getDepartment() == null) {
                throw new BusinessException("Bu rapora erişim yetkiniz yok");
            }
            String deptName = employee.getDepartment().getName();
            if (!deptName.equals("Muhasebe") && !deptName.equals("Finans")) {
                throw new BusinessException("Bu rapora erişim yetkiniz yok. Sadece Muhasebe ve Finans departmanları erişebilir.");
            }
        }

        byte[] content = accountingReportService.exportReport(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leave-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}

