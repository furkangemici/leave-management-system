package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(max = 20, message = "Telefon numarası en fazla 20 karakter olabilir.")
    private String phoneNumber;

    @Size(max = 250, message = "Adres en fazla 250 karakter olabilir.")
    private String address;

    // Buraya 'department' veya 'salary' koymuyoruz.
    // Böylece kullanıcı istese de bunları değiştiremez.
}
