package com.cozumtr.leave_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverlappingLeaveDetailDTO {
    private String employeeFullName;
    private String leaveTypeName;
    private LocalDateTime leaveStartDate;
    private LocalDateTime leaveEndDate;
    private BigDecimal overlappingHours;
}

