package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequestDto {

    @NotBlank(message = "Mevcut şifre boş olamaz")
    private String currentPassword;

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Size(min = 8, max = 30, message = "Şifre en az 8, en fazla 30 karakter olmalıdır")
    private String newPassword;

    @NotBlank(message = "Yeni şifre tekrarı boş olamaz")
    private String confirmNewPassword;
}

