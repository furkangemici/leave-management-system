package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDto {

    @NotBlank(message = "Token alanı boş bırakılamaz")
    private String token;

    @NotBlank(message = "Yeni şifre alanı boş bırakılamaz")
    @Size(min = 8, max = 30, message = "Şifre en az 8, en fazla 30 karakter olmalıdır")
    private String newPassword;

    @NotBlank(message = "Şifre tekrarı alanı boş bırakılamaz")
    private String passwordConfirm;
}

