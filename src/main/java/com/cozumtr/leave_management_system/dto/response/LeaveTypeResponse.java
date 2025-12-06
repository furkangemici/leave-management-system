package com.cozumtr.leave_management_system.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LeaveTypeResponse {
    private Long id;
    private String name;
    private boolean isPaid;
    private String requestUnit; 
}
