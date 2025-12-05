package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.UpdateProfileRequest;
import com.cozumtr.leave_management_system.dto.response.UserResponse;
import com.cozumtr.leave_management_system.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getMyProfile() {
        // Kimlik bilgisi JWT'den okunur, kullanıcı sadece kendi profilini görebilir.
        UserResponse userResponse = employeeService.getMyProfile();
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {

        // ID parametresi GÖNDERMİYORUZ. Servis katmanı, token'dan (SecurityContext)
        // kimin giriş yaptığını bulup sadece onun profilini güncelleyecek.

        UserResponse updatedEmployee = employeeService.updateProfile(request);

        // Geriye sadece "Başarılı" mesajı değil, güncellenmiş verinin kendisini dönüyoruz.
        return ResponseEntity.ok(updatedEmployee);
    }
}