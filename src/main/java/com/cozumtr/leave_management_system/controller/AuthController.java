package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.ActivateRequestDto;
import com.cozumtr.leave_management_system.dto.request.ForgotPasswordRequestDto;
import com.cozumtr.leave_management_system.dto.request.LoginRequestDto;
import com.cozumtr.leave_management_system.dto.request.RegisterRequestDto;
import com.cozumtr.leave_management_system.dto.request.ResetPasswordRequestDto;
import com.cozumtr.leave_management_system.dto.response.AuthResponseDto;
import com.cozumtr.leave_management_system.dto.response.EmployeeResponseDto;
import com.cozumtr.leave_management_system.dto.response.MessageResponseDto;
import com.cozumtr.leave_management_system.dto.response.TokenValidationResponseDto;
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

    /**
     * 4. Forgot Password (Şifremi Unuttum - Talep Et)
     * POST /api/auth/forgot-password
     * Girdi: ForgotPasswordRequestDto (email)
     * Çıktı: MessageResponseDto (Güvenlik: Kullanıcı yoksa bile başarılı döner)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        try {
            authService.forgotPassword(request.getEmail());
            // Güvenlik: Kullanıcı yoksa bile "Email gönderildi" mesajı döner
            MessageResponseDto response = MessageResponseDto.builder()
                    .message("Eğer böyle bir kullanıcı varsa, şifre sıfırlama linki email adresinize gönderilmiştir.")
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            MessageResponseDto response = MessageResponseDto.builder()
                    .message("Bir hata oluştu. Lütfen daha sonra tekrar deneyin.")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 5. Validate Reset Token (Token Doğrulama)
     * GET /api/auth/validate-reset-token?token=xyz
     * Girdi: Query parameter (token)
     * Çıktı: TokenValidationResponseDto (valid: true/false, message)
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<TokenValidationResponseDto> validateResetToken(@RequestParam String token) {
        try {
            boolean isValid = authService.validateResetToken(token);
            TokenValidationResponseDto response = TokenValidationResponseDto.builder()
                    .valid(isValid)
                    .message(isValid ? "Token geçerli" : "Geçersiz veya süresi dolmuş token")
                    .build();
            
            if (isValid) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            TokenValidationResponseDto response = TokenValidationResponseDto.builder()
                    .valid(false)
                    .message("Token doğrulama hatası")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 6. Reset Password (Şifreyi Sıfırla)
     * POST /api/auth/reset-password
     * Girdi: ResetPasswordRequestDto (token, newPassword, passwordConfirm)
     * Çıktı: MessageResponseDto (başarılı veya hata mesajı)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword(), request.getPasswordConfirm());
            MessageResponseDto response = MessageResponseDto.builder()
                    .message("Şifreniz başarıyla sıfırlandı")
                    .build();
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            MessageResponseDto response = MessageResponseDto.builder()
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            MessageResponseDto response = MessageResponseDto.builder()
                    .message("Şifre sıfırlama hatası")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
