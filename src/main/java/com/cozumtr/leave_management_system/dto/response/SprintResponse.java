package com.cozumtr.leave_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintResponse {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String departmentName;
}

