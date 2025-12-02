package com.cozumtr.leave_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String jobTitle;
    private LocalDate hireDate;
    private Boolean isActive;
    private Set<String> roles; // RBAC için: Frontend hangi menüleri göstereceğini bilir
    private String departmentName;
    private String managerFullName;
    private BigDecimal dailyWorkHours;
}

