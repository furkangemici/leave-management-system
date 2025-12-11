package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.AccountingReportRequest;
import com.cozumtr.leave_management_system.enums.ReportType;
import com.cozumtr.leave_management_system.dto.response.AccountingLeaveReportResponse;
import com.cozumtr.leave_management_system.dto.response.AccountingLeaveReportRow;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.LeaveAttachmentRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountingReportService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveAttachmentRepository leaveAttachmentRepository;

    @Transactional(readOnly = true)
    public AccountingLeaveReportResponse getReport(AccountingReportRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        FilterFlags flags = flagsFor(request.getType());

        List<LeaveRequest> leaveRequests = leaveRequestRepository.findForAccountingReport(
                approvedStatuses(),
                request.getStartDate(),
                request.getEndDate(),
                flags.onlyUnpaid,
                flags.documentRequired,
                request.getDepartmentId(),
                request.getEmployeeId()
        );

        List<AccountingLeaveReportRow> rows = leaveRequests.stream()
                .map(lr -> AccountingLeaveReportRow.builder()
                        .leaveRequestId(lr.getId())
                        .employeeFullName(lr.getEmployee().getFirstName() + " " + lr.getEmployee().getLastName())
                        .departmentName(lr.getEmployee().getDepartment() != null ? lr.getEmployee().getDepartment().getName() : null)
                        .leaveTypeName(lr.getLeaveType().getName())
                        .paid(lr.getLeaveType().isPaid())
                        .deductsFromAnnual(lr.getLeaveType().isDeductsFromAnnual())
                        .documentRequired(lr.getLeaveType().isDocumentRequired())
                        .startDate(lr.getStartDateTime())
                        .endDate(lr.getEndDateTime())
                        .durationHours(lr.getDurationHours())
                        .status(lr.getRequestStatus().name())
                        .attachmentCount(safeCountAttachments(lr.getId()))
                        .build())
                .collect(Collectors.toList());

        return AccountingLeaveReportResponse.builder()
                .rows(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportReport(AccountingReportRequest request) {
        AccountingLeaveReportResponse data = getReport(request);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Leave Report");
            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            String[] headers = new String[]{
                    "Talep ID", "Çalışan", "Departman", "İzin Türü", "Ücretli", "Yıllık İzin Düşer",
                    "Belge Zorunlu", "Başlangıç", "Bitiş", "Süre (saat)", "Durum", "Ek Sayısı"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (AccountingLeaveReportRow row : data.getRows()) {
                Row r = sheet.createRow(rowIdx++);
                int c = 0;
                r.createCell(c++).setCellValue(row.getLeaveRequestId());
                r.createCell(c++).setCellValue(nonNull(row.getEmployeeFullName()));
                r.createCell(c++).setCellValue(nonNull(row.getDepartmentName()));
                r.createCell(c++).setCellValue(nonNull(row.getLeaveTypeName()));
                r.createCell(c++).setCellValue(row.isPaid());
                r.createCell(c++).setCellValue(row.isDeductsFromAnnual());
                r.createCell(c++).setCellValue(row.isDocumentRequired());
                r.createCell(c++).setCellValue(row.getStartDate().toString());
                r.createCell(c++).setCellValue(row.getEndDate().toString());
                r.createCell(c++).setCellValue(row.getDurationHours().toString());
                r.createCell(c++).setCellValue(row.getStatus());
                r.createCell(c++).setCellValue(row.getAttachmentCount());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Rapor oluşturulamadı: " + e.getMessage());
        }
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BusinessException("Başlangıç ve bitiş tarihleri zorunludur.");
        }
        if (end.isBefore(start)) {
            throw new BusinessException("Bitiş tarihi başlangıçtan önce olamaz.");
        }
    }

    private FilterFlags flagsFor(ReportType type) {
        return switch (type) {
            case UNPAID -> new FilterFlags(true, false);
            case DOCUMENT_REQUIRED -> new FilterFlags(false, true);
            case ALL -> new FilterFlags(false, false);
        };
    }

    private List<RequestStatus> approvedStatuses() {
        return List.of(RequestStatus.APPROVED, RequestStatus.APPROVED_HR, RequestStatus.APPROVED_MANAGER);
    }

    private int safeCountAttachments(Long leaveRequestId) {
        return leaveAttachmentRepository.findByLeaveRequestId(leaveRequestId).size();
    }

    private String nonNull(String val) {
        return StringUtils.hasText(val) ? val : "";
    }

    private record FilterFlags(boolean onlyUnpaid, boolean documentRequired) {}
}

