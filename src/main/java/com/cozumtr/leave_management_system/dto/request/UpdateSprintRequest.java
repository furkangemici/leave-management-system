package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Sprint güncelleme request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSprintRequest {

    @NotBlank(message = "Sprint adı boş olamaz")
    private String name;

    @NotNull(message = "Başlangıç tarihi boş olamaz")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Bitiş tarihi boş olamaz")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /**
     * Sprint süresi (hafta cinsinden).
     * Otomatik planlama için referans olarak kullanılır.
     */
    @NotNull(message = "Sprint süresi (hafta) boş olamaz")
    @Positive(message = "Sprint süresi pozitif bir sayı olmalıdır")
    private Integer durationWeeks;
}

