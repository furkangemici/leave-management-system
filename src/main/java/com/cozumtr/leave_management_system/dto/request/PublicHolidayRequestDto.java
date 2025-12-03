package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PublicHolidayRequestDto {

    @NotBlank
    private String name;

    @NotNull
    private LocalDate date;

    private Boolean isHalfDay = false; // yarım gün mü
}
