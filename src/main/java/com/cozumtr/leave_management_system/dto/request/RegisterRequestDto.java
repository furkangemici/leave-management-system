package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegisterRequestDto {

    @NotBlank private String firstName;
    @NotBlank private String lastName;

    @Email @NotBlank private String email;
    private String password;

    @NotBlank private String jobTitle;
    @NotNull
    private Long departmentId;
    @NotNull private BigDecimal dailyWorkHours;

    private Long roleId;
}