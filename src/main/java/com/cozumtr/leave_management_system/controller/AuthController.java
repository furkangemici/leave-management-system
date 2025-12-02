package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.ActivateRequestDto;
import com.cozumtr.leave_management_system.dto.request.LoginRequestDto;
import com.cozumtr.leave_management_system.dto.request.RegisterRequestDto;
import com.cozumtr.leave_management_system.dto.response.AuthResponseDto;
import com.cozumtr.leave_management_system.dto.response.EmployeeResponseDto;
import com.cozumtr.leave_management_system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 1. Login (Giriş)
     * POST /api/auth/login
     * Girdi: LoginRequestDto (email, password)
     * Çıktı: AuthResponseDto (token, tokenType, userId, userEmail, roles)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            AuthResponseDto response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 2. Invite (Davet Et)
     * POST /api/auth/invite
     * Girdi: RegisterRequestDto (firstName, lastName, email, jobTitle, departmentId, dailyWorkHours)
     * Çıktı: EmployeeResponseDto
     * Not: password burada gönderilmez, inviteUser metodu password'u null olarak kaydeder
     */
    @PostMapping("/invite")
    public ResponseEntity<EmployeeResponseDto> invite(@Valid @RequestBody RegisterRequestDto request) {
        try {
            EmployeeResponseDto response = authService.inviteUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 3. Activate (Aktif Et)
     * POST /api/auth/activate
     * Girdi: ActivateRequestDto (token, password, passwordConfirm)
     * Çıktı: AuthResponseDto (otomatik login sonrası token döner)
     */
    @PostMapping("/activate")
    public ResponseEntity<AuthResponseDto> activate(@Valid @RequestBody ActivateRequestDto request) {
        try {
            AuthResponseDto response = authService.activateUserAndSetPassword(
                    request.getToken(),
                    request.getPassword(),
                    request.getPasswordConfirm()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
