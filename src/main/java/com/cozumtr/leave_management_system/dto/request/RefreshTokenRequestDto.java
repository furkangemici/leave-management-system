package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequestDto {
    @NotBlank(message = "Token boş olamaz")
    private String token;
}

