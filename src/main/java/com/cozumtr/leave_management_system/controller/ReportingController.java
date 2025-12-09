package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.SprintOverlapReportRequest;
import com.cozumtr.leave_management_system.dto.response.SprintOverlapReportDTO;
import com.cozumtr.leave_management_system.dto.response.SprintResponse;
import com.cozumtr.leave_management_system.entities.Sprint;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.SprintRepository;
import com.cozumtr.leave_management_system.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final LeaveRequestService leaveRequestService;
    private final SprintRepository sprintRepository;

    /**
     * Sprint çakışma raporu endpoint'i (GET - Sprint ID ile).
     * Kullanıcı dropdown'dan sprint seçtiğinde GET ile çağrılır.
     *
     * @param sprintId Sprint ID (zorunlu)
     * @return SprintOverlapReportDTO
     */
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'CEO')")
    @GetMapping("/sprint-overlap")
    public ResponseEntity<SprintOverlapReportDTO> getSprintOverlapReport(
            @RequestParam(required = true) Long sprintId) {
        
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new BusinessException("Sprint bulunamadı: " + sprintId));
        
        LocalDateTime sprintStartDateTime = sprint.getStartDate().atStartOfDay();
        LocalDateTime sprintEndDateTime = sprint.getEndDate().atTime(23, 59, 59);
        
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(
                sprintStartDateTime, sprintEndDateTime);
        return ResponseEntity.ok(report);
    }

    /**
     * Sprint çakışma raporu endpoint'i (POST - Manuel tarih girişi için).
     * Kullanıcı tarihleri manuel girdiğinde POST body ile gönderilir.
     *
     * @param request SprintOverlapReportRequest (sprintStart, sprintEnd)
     * @return SprintOverlapReportDTO
     */
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'CEO')")
    @PostMapping("/sprint-overlap")
    public ResponseEntity<SprintOverlapReportDTO> getSprintOverlapReportWithDates(
            @Valid @RequestBody SprintOverlapReportRequest request) {
        
        // Manuel girilen tarihleri LocalDateTime'a çevir
        LocalDateTime sprintStartDateTime = request.getSprintStart().atStartOfDay();
        LocalDateTime sprintEndDateTime = request.getSprintEnd().atTime(23, 59, 59);
        
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(
                sprintStartDateTime, sprintEndDateTime);
        return ResponseEntity.ok(report);
    }

    /**
     * Tüm sprint'leri listeler (Frontend dropdown için).
     *
     * @return Sprint listesi
     */
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'CEO')")
    @GetMapping("/sprints")
    public ResponseEntity<List<SprintResponse>> getAllSprints() {
        List<Sprint> sprints = sprintRepository.findAll();
        List<SprintResponse> sprintResponses = sprints.stream()
                .map(sprint -> SprintResponse.builder()
                        .id(sprint.getId())
                        .name(sprint.getName())
                        .startDate(sprint.getStartDate())
                        .endDate(sprint.getEndDate())
                        .durationWeeks(sprint.getDurationWeeks())
                        .departmentName(sprint.getDepartment() != null ? sprint.getDepartment().getName() : null)
                        .departmentId(sprint.getDepartment() != null ? sprint.getDepartment().getId() : null)
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(sprintResponses);
    }

    /**
     * Sprint çakışma raporunu Excel formatında export eder (GET - Sprint ID ile).
     * Kullanıcı dropdown'dan sprint seçtiğinde GET ile çağrılır.
     *
     * @param sprintId Sprint ID (zorunlu)
     * @return Excel dosyası
     */
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'CEO')")
    @GetMapping("/sprint-overlap/export")
    public ResponseEntity<byte[]> exportSprintOverlapReportToExcel(
            @RequestParam(required = true) Long sprintId) throws IOException {
        
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new BusinessException("Sprint bulunamadı: " + sprintId));
        
        LocalDateTime sprintStartDateTime = sprint.getStartDate().atStartOfDay();
        LocalDateTime sprintEndDateTime = sprint.getEndDate().atTime(23, 59, 59);
        String sprintName = sprint.getName();
        
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(
                sprintStartDateTime, sprintEndDateTime);
        
        // Excel dosyası oluştur
        byte[] excelBytes = createExcelFile(report, sprintName, sprintStartDateTime.toLocalDate(), sprintEndDateTime.toLocalDate());
        
        // Dosya adı oluştur
        String fileName = "Sprint_Cakisma_Raporu_" + sprintName.replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }

    /**
     * Sprint çakışma raporunu Excel formatında export eder (POST - Manuel tarih girişi ile).
     * Kullanıcı tarihleri manuel girdiğinde POST body ile gönderilir.
     *
     * @param request SprintOverlapReportRequest (sprintStart, sprintEnd)
     * @return Excel dosyası
     */
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'CEO')")
    @PostMapping("/sprint-overlap/export")
    public ResponseEntity<byte[]> exportSprintOverlapReportToExcelPost(
            @Valid @RequestBody SprintOverlapReportRequest request) throws IOException {
        
        LocalDateTime sprintStartDateTime = request.getSprintStart().atStartOfDay();
        LocalDateTime sprintEndDateTime = request.getSprintEnd().atTime(23, 59, 59);
        String sprintName = request.getSprintStart() + "_" + request.getSprintEnd();
        
        SprintOverlapReportDTO report = leaveRequestService.generateSprintOverlapReport(
                sprintStartDateTime, sprintEndDateTime);
        
        // Excel dosyası oluştur
        byte[] excelBytes = createExcelFile(report, sprintName, request.getSprintStart(), request.getSprintEnd());
        
        // Dosya adı oluştur
        String fileName = "Sprint_Cakisma_Raporu_" + sprintName.replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }

    /**
     * Excel dosyası oluşturur.
     */
    private byte[] createExcelFile(SprintOverlapReportDTO report, String sprintName, 
                                   LocalDate startDate, LocalDate endDate) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sprint Çakışma Raporu");
        
        // Stil tanımlamaları
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        
        CellStyle totalStyle = workbook.createCellStyle();
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderBottom(BorderStyle.THIN);
        totalStyle.setBorderTop(BorderStyle.THIN);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);
        
        int rowNum = 0;
        
        // Başlık satırı
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Sprint Çakışma Raporu");
        titleCell.setCellStyle(headerStyle);
        
        // Sprint bilgileri
        Row sprintInfoRow = sheet.createRow(rowNum++);
        sprintInfoRow.createCell(0).setCellValue("Sprint:");
        sprintInfoRow.createCell(1).setCellValue(sprintName);
        
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Tarih Aralığı:");
        dateRow.createCell(1).setCellValue(startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                                          " - " + endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        
        rowNum++; // Boş satır
        
        // Toplam kayıp saati
        Row totalRow = sheet.createRow(rowNum++);
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("Toplam Kapasite Kaybı:");
        totalLabelCell.setCellStyle(totalStyle);
        Cell totalValueCell = totalRow.createCell(1);
        totalValueCell.setCellValue(report.getTotalLossHours().doubleValue());
        totalValueCell.setCellStyle(totalStyle);
        totalRow.createCell(2).setCellValue("saat");
        totalRow.getCell(2).setCellStyle(totalStyle);
        
        rowNum++; // Boş satır
        
        // Tablo başlıkları
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Çalışan Adı", "İzin Türü", "İzin Başlangıç", "İzin Bitiş", "Çakışan Saat"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Veri satırları
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        for (var leave : report.getOverlappingLeaves()) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(leave.getEmployeeFullName());
            row.createCell(1).setCellValue(leave.getLeaveTypeName());
            row.createCell(2).setCellValue(leave.getLeaveStartDate().format(dateFormatter));
            row.createCell(3).setCellValue(leave.getLeaveEndDate().format(dateFormatter));
            row.createCell(4).setCellValue(leave.getOverlappingHours().doubleValue());
            
            // Stil uygula
            for (int i = 0; i < headers.length; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }
        
        // Sütun genişliklerini ayarla
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // Biraz ekstra boşluk
        }
        
        // Excel'i byte array'e çevir
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream.toByteArray();
    }
}

