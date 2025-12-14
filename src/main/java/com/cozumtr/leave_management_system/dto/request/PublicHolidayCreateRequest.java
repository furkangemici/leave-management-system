package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicHolidayCreateRequest {

    @NotNull(message = "Başlangıç tarihi boş olamaz")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate; // Opsiyonel - null ise startDate kullanılır

    @NotBlank(message = "Tatil adı boş olamaz")
    private String name;

    @NotNull(message = "Yarım gün durumu belirtilmelidir")
    private Boolean isHalfDay;
}

