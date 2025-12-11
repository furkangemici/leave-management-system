package com.cozumtr.leave_management_system.dto.request;

import com.cozumtr.leave_management_system.enums.NotificationChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDto {

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @NotBlank(message = "E-posta alanı boş bırakılamaz")
    private String email;

    private NotificationChannel channel = NotificationChannel.EMAIL;

    // SMS seçildiğinde kullanıcıdan gelen telefon doğrulaması için
    private String phoneNumber;
}

