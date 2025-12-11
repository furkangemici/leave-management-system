package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.ActivateRequestDto;
import com.cozumtr.leave_management_system.dto.request.ForgotPasswordRequestDto;
import com.cozumtr.leave_management_system.dto.request.LoginRequestDto;
import com.cozumtr.leave_management_system.dto.request.RegisterRequestDto;
import com.cozumtr.leave_management_system.dto.request.ResetPasswordRequestDto;
import com.cozumtr.leave_management_system.dto.response.AuthResponseDto;
import com.cozumtr.leave_management_system.dto.response.DepartmentResponse;
import com.cozumtr.leave_management_system.dto.response.EmployeeResponseDto;
import com.cozumtr.leave_management_system.dto.response.MessageResponseDto;
import com.cozumtr.leave_management_system.dto.response.RoleResponse;
import com.cozumtr.leave_management_system.dto.response.TokenValidationResponseDto;
import com.cozumtr.leave_management_system.service.AuthService;
import com.cozumtr.leave_management_system.service.DepartmentService;
import com.cozumtr.leave_management_system.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RoleService roleService;
    private final DepartmentService departmentService;

    /**
     * 1. Login (Giriş)
     * POST /api/auth/login
     * Girdi: LoginRequestDto (email, password)
     * Çıktı: AuthResponseDto (token, tokenType, userId, userEmail, roles)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Rolleri Listele (İK yeni eleman eklerken rol seçimi için)
     * GET /api/auth/roles
     * Çıktı: List<RoleResponse> (id, roleName)
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllActiveRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * 2.5. Departmanları Listele (İK yeni eleman eklerken departman seçimi için)
     * GET /api/auth/departments
     * Çıktı: List<DepartmentResponse> (id, name)
     */
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        List<DepartmentResponse> departments = departmentService.getAllActiveDepartments();
        return ResponseEntity.ok(departments);
    }

    /**
     * 3. Invite (Davet Et)
     * POST /api/auth/invite
     * Girdi: RegisterRequestDto (firstName, lastName, email, jobTitle, departmentId, dailyWorkHours, roleId)
     * Çıktı: EmployeeResponseDto
     * Not: password burada gönderilmez, inviteUser metodu password'u null olarak kaydeder
     */
    @PostMapping("/invite")
    public ResponseEntity<EmployeeResponseDto> invite(@Valid @RequestBody RegisterRequestDto request) {
        EmployeeResponseDto response = authService.inviteUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 4. Activate (Aktif Et)
     * POST /api/auth/activate
     * Girdi: ActivateRequestDto (token, password, passwordConfirm)
     * Çıktı: AuthResponseDto (otomatik login sonrası token döner)
     */
    @PostMapping("/activate")
    public ResponseEntity<AuthResponseDto> activate(@Valid @RequestBody ActivateRequestDto request) {
        AuthResponseDto response = authService.activateUserAndSetPassword(
                request.getToken(),
                request.getPassword(),
                request.getPasswordConfirm()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 5. Forgot Password (Şifremi Unuttum - Talep Et)
     * POST /api/auth/forgot-password
     * Girdi: ForgotPasswordRequestDto (email)
     * Çıktı: MessageResponseDto (Güvenlik: Kullanıcı yoksa bile başarılı döner)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.forgotPassword(request.getEmail(), request.getChannel(), request.getPhoneNumber());
        // Güvenlik: Kullanıcı yoksa bile "Email gönderildi" mesajı döner
        MessageResponseDto response = MessageResponseDto.builder()
                .message("Eğer böyle bir kullanıcı varsa, şifre sıfırlama linki email adresinize gönderilmiştir.")
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * 6. Validate Reset Token (Token Doğrulama)
     * GET /api/auth/validate-reset-token?token=xyz
     * Girdi: Query parameter (token)
     * Çıktı: TokenValidationResponseDto (valid: true/false, message)
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<TokenValidationResponseDto> validateResetToken(@RequestParam String token) {
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
    }

    /**
     * 7. Reset Password (Şifreyi Sıfırla)
     * POST /api/auth/reset-password
     * Girdi: ResetPasswordRequestDto (token, newPassword, passwordConfirm)
     * Çıktı: MessageResponseDto (başarılı veya hata mesajı)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request.getToken(), request.getNewPassword(), request.getPasswordConfirm());
        MessageResponseDto response = MessageResponseDto.builder()
                .message("Şifreniz başarıyla sıfırlandı")
                .build();
        return ResponseEntity.ok(response);
    }
}
