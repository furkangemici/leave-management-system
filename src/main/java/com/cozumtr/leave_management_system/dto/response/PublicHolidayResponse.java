package com.cozumtr.leave_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicHolidayResponse {
    private Long id;
    private LocalDate date;
    private String name;
    private Boolean isHalfDay;
}

