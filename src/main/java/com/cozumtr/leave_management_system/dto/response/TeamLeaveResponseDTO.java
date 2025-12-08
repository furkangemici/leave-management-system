package com.cozumtr.leave_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamLeaveResponseDTO {
    private String employeeFullName;
    private String departmentName;
    private String leaveTypeName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalHours;
}
