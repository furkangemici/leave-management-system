package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkHolidayCreateRequest {
    
    @NotNull(message = "Yıl belirtilmelidir")
    @Min(value = 2024, message = "Geçerli bir yıl giriniz")
    @Max(value = 2100, message = "Geçerli bir yıl giriniz")
    private Integer year;
    
    @NotEmpty(message = "En az bir tatil girilmelidir")
    @Valid
    private List<HolidayDateMapping> holidays;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HolidayDateMapping {
        @NotNull(message = "Şablon ID belirtilmelidir")
        private Long templateId;
        
        @NotNull(message = "Başlangıç tarihi belirtilmelidir")
        private LocalDate startDate;
        
        private LocalDate endDate; // Opsiyonel, tek günlük tatiller için null
    }
}
