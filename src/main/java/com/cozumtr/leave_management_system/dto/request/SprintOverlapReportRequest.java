package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Sprint çakışma raporu için request DTO.
 * Manuel tarih girişi yapıldığında POST body olarak kullanılır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintOverlapReportRequest {
    
    /**
     * Sprint başlangıç tarihi (zorunlu)
     */
    @NotNull(message = "Sprint başlangıç tarihi boş olamaz")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate sprintStart;
    
    /**
     * Sprint bitiş tarihi (zorunlu)
     */
    @NotNull(message = "Sprint bitiş tarihi boş olamaz")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate sprintEnd;
}

