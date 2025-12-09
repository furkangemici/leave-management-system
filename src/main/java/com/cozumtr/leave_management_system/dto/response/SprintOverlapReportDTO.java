package com.cozumtr.leave_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintOverlapReportDTO {
    private BigDecimal totalLossHours;
    private List<OverlappingLeaveDetailDTO> overlappingLeaves;
}

