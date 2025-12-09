package com.cozumtr.leave_management_system.dto.response;

import com.cozumtr.leave_management_system.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerLeaveResponseDTO {
    private Long leaveRequestId;
    private String employeeFullName;
    private String employeeDepartmentName;
    private String leaveTypeName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal duration;
    private RequestStatus currentStatus;
    private String workflowNextApproverRole;
    private List<ApprovalHistoryDTO> approvalHistory;
}

