package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.ChangePasswordRequestDto;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService changePassword Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthEmail(String email) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }

    private User buildUser(String email, String passwordHash) {
        Employee employee = new Employee();
        employee.setEmail(email);

        User user = new User();
        user.setEmployee(employee);
        user.setPasswordHash(passwordHash);
        user.setFailedLoginAttempts(3);
        user.setPasswordResetToken("old-token");
        user.setPasswordResetExpires(LocalDateTime.now().plusMinutes(5));
        user.setIsActive(true);
        return user;
    }

    @Test
    @DisplayName("Başarılı şifre değişimi: mevcut şifre doğru, yeni şifre kurallara uygun")
    void changePassword_success() {
        String email = "test@example.com";
        mockAuthEmail(email);

        User user = buildUser(email, "encoded-old");
        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword("old");
        dto.setNewPassword("NewPassword1");
        dto.setConfirmNewPassword("NewPassword1");

        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("encoded-new");

        userService.changePassword(dto);

        verify(userRepository, times(1)).save(user);
        assertEquals("encoded-new", user.getPasswordHash());
        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(null, user.getPasswordResetToken());
        assertEquals(null, user.getPasswordResetExpires());
    }

    @Test
    @DisplayName("Kullanıcı bulunamazsa BusinessException fırlatır")
    void changePassword_userNotFound() {
        String email = "missing@example.com";
        mockAuthEmail(email);

        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword("old");
        dto.setNewPassword("NewPassword1");
        dto.setConfirmNewPassword("NewPassword1");

        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.changePassword(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mevcut şifre hash yoksa (aktivasyon yapılmamış) BusinessException fırlatır")
    void changePassword_passwordNotSet() {
        String email = "test@example.com";
        mockAuthEmail(email);

        User user = buildUser(email, null); // passwordHash null
        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(user));

        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword("old");
        dto.setNewPassword("NewPassword1");
        dto.setConfirmNewPassword("NewPassword1");

        assertThrows(BusinessException.class, () -> userService.changePassword(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mevcut şifre eşleşmezse BusinessException fırlatır")
    void changePassword_currentPasswordMismatch() {
        String email = "test@example.com";
        mockAuthEmail(email);

        User user = buildUser(email, "encoded-old");
        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword("wrong");
        dto.setNewPassword("NewPassword1");
        dto.setConfirmNewPassword("NewPassword1");

        assertThrows(BusinessException.class, () -> userService.changePassword(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Yeni şifre ve tekrarı uyuşmazsa BusinessException fırlatır")
    void changePassword_newPasswordMismatch() {
        String email = "test@example.com";
        mockAuthEmail(email);

        User user = buildUser(email, "encoded-old");
        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);

        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword("old");
        dto.setNewPassword("NewPassword1");
        dto.setConfirmNewPassword("NewPassword2");

        assertThrows(BusinessException.class, () -> userService.changePassword(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Yeni şifre 8 karakterden kısa ise BusinessException fırlatır")
    void changePassword_newPasswordTooShort() {
        String email = "test@example.com";
        mockAuthEmail(email);

        User user = buildUser(email, "encoded-old");
        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);

        ChangePasswordRequestDto dto = new ChangePasswordRequestDto();
        dto.setCurrentPassword("old");
        dto.setNewPassword("short");
        dto.setConfirmNewPassword("short");

        assertThrows(BusinessException.class, () -> userService.changePassword(dto));
        verify(userRepository, never()).save(any());
    }
}


