package com.cozumtr.leave_management_system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AuthResponseDto {
    private String token;
    private String tokenType = "Bearer"; // JWT standardı
    private Long userId;
    private String userEmail;
    private Set<String> roles;
}