package com.cozumtr.leave_management_system.service;



import com.cozumtr.leave_management_system.dto.request.LoginRequestDto;

import com.cozumtr.leave_management_system.dto.response.AuthResponseDto;

import com.cozumtr.leave_management_system.entities.Employee;

import com.cozumtr.leave_management_system.entities.Role;

import com.cozumtr.leave_management_system.entities.User;

import com.cozumtr.leave_management_system.enums.NotificationChannel;

import com.cozumtr.leave_management_system.exception.BusinessException;

import com.cozumtr.leave_management_system.repository.DepartmentRepository;

import com.cozumtr.leave_management_system.repository.EmployeeRepository;

import com.cozumtr.leave_management_system.repository.RoleRepository;

import com.cozumtr.leave_management_system.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;



import java.time.LocalDateTime;

import java.util.HashSet;

import java.util.Optional;

import java.util.Set;



import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.anySet;

import static org.mockito.Mockito.*;



@ExtendWith(MockitoExtension.class)

@DisplayName("AuthService Unit Tests")

class AuthServiceTest {



    @Mock

    private UserRepository userRepository;



    @Mock

    private EmployeeRepository employeeRepository;



    @Mock

    private RoleRepository roleRepository;



    @Mock

    private DepartmentRepository departmentRepository;



    @Mock

    private PasswordEncoder passwordEncoder;



    @Mock

    private JwtService jwtService;



    @Mock

    private EmailService emailService;

    @Mock

    private SmsService smsService;



    @Mock

    private org.springframework.security.authentication.AuthenticationManager authenticationManager;



    @InjectMocks

    private AuthService authService;



    private User testUser;

    private Employee testEmployee;



    @BeforeEach

    void setUp() {

        // Test Employee oluştur

        testEmployee = new Employee();

        testEmployee.setId(1L);

        testEmployee.setEmail("test@sirket.com");

        testEmployee.setFirstName("Test");

        testEmployee.setLastName("User");

        testEmployee.setIsActive(true);
        testEmployee.setPhoneNumber("+905551112233");



        // Test User oluştur

        testUser = new User();

        testUser.setId(1L);

        testUser.setEmployee(testEmployee);

        testUser.setPasswordHash("$2a$10$encodedPassword");

        testUser.setIsActive(true);

        testUser.setFailedLoginAttempts(0);

    }



    @Test

    @DisplayName("forgotPassword - Kullanıcı varsa token oluşturmalı")

    void testForgotPassword_UserExists_ShouldCreateToken() {

        // Given

        String email = "test@sirket.com";

        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));

        when(userRepository.save(any(User.class))).thenReturn(testUser);



        // When

        authService.forgotPassword(email, NotificationChannel.EMAIL, null);



        // Then

        verify(userRepository, times(1)).findByEmployeeEmail(email);

        verify(userRepository, times(1)).save(any(User.class));

        verify(emailService, times(1)).sendPasswordResetEmail(eq(email), anyString());
        verify(smsService, never()).sendSms(anyString(), anyString());

        assertNotNull(testUser.getPasswordResetToken());

        assertNotNull(testUser.getPasswordResetExpires());

    }



    @Test

    @DisplayName("forgotPassword - Kullanıcı yoksa hata fırlatmamalı (güvenlik)")

    void testForgotPassword_UserNotExists_ShouldNotThrowException() {

        // Given

        String email = "nonexistent@sirket.com";

        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.empty());



        // When & Then

        assertDoesNotThrow(() -> authService.forgotPassword(email, NotificationChannel.EMAIL, null));

        verify(userRepository, times(1)).findByEmployeeEmail(email);

        verify(userRepository, never()).save(any(User.class));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());

        verify(smsService, never()).sendSms(anyString(), anyString());

    }



    @Test

    @DisplayName("forgotPassword - SMS kanalı, telefon eşleşirse token ve SMS gönderir")

    void testForgotPassword_SmsChannel_WithMatchingPhone_SendsSms() {

        // Given

        String email = "test@sirket.com";

        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));

        when(userRepository.save(any(User.class))).thenReturn(testUser);



        // When

        authService.forgotPassword(email, NotificationChannel.SMS, "+905551112233");



        // Then

        verify(userRepository, times(1)).save(any(User.class));

        verify(smsService, times(1)).sendSms(eq("+905551112233"), anyString());

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());

        assertNotNull(testUser.getPasswordResetToken());

        assertNotNull(testUser.getPasswordResetExpires());

    }



    @Test

    @DisplayName("forgotPassword - SMS kanalı, telefon eşleşmezse BusinessException fırlatır")

    void testForgotPassword_SmsChannel_WithMismatchedPhone_Throws() {

        // Given

        String email = "test@sirket.com";

        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));



        // When & Then

        assertThrows(BusinessException.class, () ->

                authService.forgotPassword(email, NotificationChannel.SMS, "+905500000000"));



        verify(userRepository, times(1)).findByEmployeeEmail(email);

        verify(userRepository, never()).save(any(User.class));

        verify(smsService, never()).sendSms(anyString(), anyString());

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());

    }



    @Test

    @DisplayName("forgotPassword - Kullanıcı aktif değilse token oluşturmamalı")

    void testForgotPassword_UserInactive_ShouldNotCreateToken() {

        // Given

        String email = "test@sirket.com";

        testUser.setIsActive(false);

        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));



        // When

        authService.forgotPassword(email, NotificationChannel.EMAIL, null);



        // Then

        verify(userRepository, times(1)).findByEmployeeEmail(email);

        verify(userRepository, never()).save(any(User.class));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());

        verify(smsService, never()).sendSms(anyString(), anyString());

    }



    @Test

    @DisplayName("validateResetToken - Geçerli token döndürmeli")

    void testValidateResetToken_ValidToken_ShouldReturnTrue() {

        // Given

        String token = "valid-token-123";

        testUser.setPasswordResetToken(token);

        testUser.setPasswordResetExpires(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.of(testUser));



        // When

        boolean result = authService.validateResetToken(token);



        // Then

        assertTrue(result);

        verify(userRepository, times(1)).findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class));

    }



    @Test

    @DisplayName("validateResetToken - Geçersiz token döndürmeli")

    void testValidateResetToken_InvalidToken_ShouldReturnFalse() {

        // Given

        String token = "invalid-token";

        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.empty());



        // When

        boolean result = authService.validateResetToken(token);



        // Then

        assertFalse(result);

        verify(userRepository, times(1)).findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class));

    }



    @Test

    @DisplayName("validateResetToken - Süresi dolmuş token döndürmeli")

    void testValidateResetToken_ExpiredToken_ShouldReturnFalse() {

        // Given

        String token = "expired-token";

        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.empty());



        // When

        boolean result = authService.validateResetToken(token);



        // Then

        assertFalse(result);

    }



    @Test

    @DisplayName("validateResetToken - Kullanıcı aktif değilse false döndürmeli")

    void testValidateResetToken_UserInactive_ShouldReturnFalse() {

        // Given

        String token = "valid-token";

        testUser.setIsActive(false);

        testUser.setPasswordResetToken(token);

        testUser.setPasswordResetExpires(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.of(testUser));



        // When

        boolean result = authService.validateResetToken(token);



        // Then

        assertFalse(result);

    }



    @Test

    @DisplayName("resetPassword - Başarılı şifre sıfırlama")

    void testResetPassword_Success_ShouldUpdatePassword() {

        // Given

        String token = "valid-token-123";

        String newPassword = "NewPassword123!";

        String passwordConfirm = "NewPassword123!";

        String encodedPassword = "$2a$10$encodedNewPassword";



        testUser.setPasswordResetToken(token);

        testUser.setPasswordResetExpires(LocalDateTime.now().plusMinutes(15));



        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.of(testUser));

        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        when(userRepository.save(any(User.class))).thenReturn(testUser);



        // When

        authService.resetPassword(token, newPassword, passwordConfirm);



        // Then

        verify(userRepository, times(1)).findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class));

        verify(passwordEncoder, times(1)).encode(newPassword);

        verify(userRepository, times(1)).save(any(User.class));

        assertNull(testUser.getPasswordResetToken());

        assertNull(testUser.getPasswordResetExpires());

        assertEquals(0, testUser.getFailedLoginAttempts());

    }



    @Test

    @DisplayName("resetPassword - Şifreler eşleşmiyorsa hata fırlatmalı")

    void testResetPassword_PasswordsNotMatch_ShouldThrowException() {

        // Given

        String token = "valid-token";

        String newPassword = "NewPassword123!";

        String passwordConfirm = "DifferentPassword123!";



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.resetPassword(token, newPassword, passwordConfirm);

        });



        assertEquals("Şifre ve şifre tekrarı eşleşmiyor", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));

    }



    @Test

    @DisplayName("resetPassword - Şifre çok kısa ise hata fırlatmalı")

    void testResetPassword_PasswordTooShort_ShouldThrowException() {

        // Given

        String token = "valid-token";

        String newPassword = "123";

        String passwordConfirm = "123";



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.resetPassword(token, newPassword, passwordConfirm);

        });



        assertEquals("Şifre en az 8, en fazla 30 karakter olmalıdır", exception.getMessage());

    }



    @Test

    @DisplayName("resetPassword - Geçersiz token ise hata fırlatmalı")

    void testResetPassword_InvalidToken_ShouldThrowException() {

        // Given

        String token = "invalid-token";

        String newPassword = "NewPassword123!";

        String passwordConfirm = "NewPassword123!";



        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.empty());



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.resetPassword(token, newPassword, passwordConfirm);

        });



        assertEquals("Geçersiz veya süresi dolmuş şifre sıfırlama token'ı", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());

    }



    @Test

    @DisplayName("resetPassword - Kullanıcı aktif değilse hata fırlatmalı")

    void testResetPassword_UserInactive_ShouldThrowException() {

        // Given

        String token = "valid-token";

        String newPassword = "NewPassword123!";

        String passwordConfirm = "NewPassword123!";



        testUser.setIsActive(false);

        testUser.setPasswordResetToken(token);

        testUser.setPasswordResetExpires(LocalDateTime.now().plusMinutes(15));



        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.of(testUser));



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.resetPassword(token, newPassword, passwordConfirm);

        });



        assertEquals("Hesabınız aktif değil", exception.getMessage());

    }



    @Test

    @DisplayName("login - Yanlış şifre girilince sayaç artmalı")

    void testLogin_WrongPassword_ShouldIncrementCounter() {

        // 1. GIVEN

        String email = "test@sirket.com";

        String wrongPass = "yanlis123";



        testUser.setFailedLoginAttempts(0);

        testUser.setPasswordHash("$2a$10$encodedPassword"); // Hashlenmiş şifre var sayalım



        LoginRequestDto request = new LoginRequestDto();

        request.setEmail(email);

        request.setPassword(wrongPass);



        // Kullanıcı var

        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));



        // KRİTİK NOKTA BURASI:

        // AuthenticationManager'a diyoruz ki: "authenticate metodunu çağırırlarsa BadCredentialsException fırlat!"

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));



        // 2. WHEN & THEN

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));



        // Mesaj kontrolü (Senin kodunda "Giriş bilgileri hatalı" dönüyor)

        assertTrue(ex.getMessage().contains("Giriş bilgileri hatalı"));



        // 3. VERIFY

        // Sayaç arttı mı?

        assertEquals(1, testUser.getFailedLoginAttempts());

        // DB'ye kaydedildi mi?

        verify(userRepository, times(1)).save(testUser);

    }



    @Test

    @DisplayName("login - Başarılı giriş testi")

    void testLogin_Success_ShouldReturnToken() {

        // Given

        String email = "test@sirket.com";

        String password = "correctPassword123!";

        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";



        LoginRequestDto request = new LoginRequestDto();

        request.setEmail(email);

        request.setPassword(password);



        // Role oluştur

        Role testRole = new Role();

        testRole.setRoleName("HR");

        Set<Role> roles = new HashSet<>();

        roles.add(testRole);

        testUser.setRoles(roles);



        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        when(jwtService.generateToken(eq(email), eq(1L), anySet())).thenReturn(jwtToken);



        // AuthenticationManager başarılı dönecek - Authentication mock objesi döndür

        Authentication mockAuth = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);



        // When

        AuthResponseDto response = authService.login(request);



        // Then

        assertNotNull(response);

        assertEquals(jwtToken, response.getToken());

        assertEquals("Bearer", response.getTokenType());

        assertEquals(1L, response.getUserId());

        assertEquals(email, response.getUserEmail());

        assertNotNull(response.getRoles());

        verify(userRepository, atLeastOnce()).save(any(User.class));

    }



    @Test

    @DisplayName("login - Hesap kilitli ise (failedLoginAttempts >= 5) hata fırlatmalı")

    void testLogin_AccountLocked_ShouldThrowException() {

        // Given

        String email = "test@sirket.com";

        testUser.setFailedLoginAttempts(5); // Hesap kilitli



        LoginRequestDto request = new LoginRequestDto();

        request.setEmail(email);

        request.setPassword("anyPassword");



        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.login(request);

        });



        assertTrue(exception.getMessage().contains("kilitlenmiştir"));

        assertTrue(exception.getMessage().contains("Şifremi Unuttum"));

        // AuthenticationManager hiç çağrılmamalı

        verify(authenticationManager, never()).authenticate(any());

    }



    @Test

    @DisplayName("login - 5. hatadan sonra hesap kilitlenmeli")

    void testLogin_FifthFailedAttempt_ShouldLockAccount() {

        // Given

        String email = "test@sirket.com";

        testUser.setFailedLoginAttempts(4); // 4. hatayı yaptı, 5. hatayı yapacak



        LoginRequestDto request = new LoginRequestDto();

        request.setEmail(email);

        request.setPassword("wrongPassword");



        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        when(userRepository.save(any(User.class))).thenReturn(testUser);



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.login(request);

        });



        assertEquals("Hesabınız kilitlendi. Lütfen şifrenizi sıfırlayın.", exception.getMessage());

        assertEquals(5, testUser.getFailedLoginAttempts());

        verify(userRepository, times(1)).save(testUser);

    }



    @Test

    @DisplayName("login - Kullanıcı aktif değilse hata fırlatmalı")

    void testLogin_UserInactive_ShouldThrowException() {

        // Given

        String email = "test@sirket.com";

        testUser.setIsActive(false);



        LoginRequestDto request = new LoginRequestDto();

        request.setEmail(email);

        request.setPassword("correctPassword");



        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));



        // AuthenticationManager başarılı dönecek

        Authentication mockAuth = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.login(request);

        });



        assertTrue(exception.getMessage().contains("aktif değil"));

    }



    @Test

    @DisplayName("login - Şifre null ise hata fırlatmalı")

    void testLogin_PasswordNull_ShouldThrowException() {

        // Given

        String email = "test@sirket.com";

        testUser.setPasswordHash(null); // Şifre henüz belirlenmemiş



        LoginRequestDto request = new LoginRequestDto();

        request.setEmail(email);

        request.setPassword("anyPassword");



        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));



        // AuthenticationManager başarılı dönecek

        Authentication mockAuth = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.login(request);

        });



        assertTrue(exception.getMessage().contains("henüz belirlenmemiş"));

    }



    @Test

    @DisplayName("login - Başarılı girişte failedLoginAttempts sıfırlanmalı")

    void testLogin_Success_ShouldResetFailedAttempts() {

        // Given

        String email = "test@sirket.com";

        testUser.setFailedLoginAttempts(3); // Önceki başarısız denemeler var



        Role testRole = new Role();

        testRole.setRoleName("HR");

        Set<Role> roles = new HashSet<>();

        roles.add(testRole);

        testUser.setRoles(roles);



        LoginRequestDto request = new LoginRequestDto();

        request.setEmail(email);

        request.setPassword("correctPassword");



        when(userRepository.findByEmployeeEmail(email)).thenReturn(Optional.of(testUser));

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        when(jwtService.generateToken(anyString(), anyLong(), anySet())).thenReturn("token");



        // AuthenticationManager başarılı dönecek

        Authentication mockAuth = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);



        // When

        authService.login(request);



        // Then

        assertEquals(0, testUser.getFailedLoginAttempts());

        verify(userRepository, atLeastOnce()).save(testUser);

    }



    @Test

    @DisplayName("resetPassword - Şifre çok uzun ise (30 karakterden fazla) hata fırlatmalı")

    void testResetPassword_PasswordTooLong_ShouldThrowException() {

        // Given

        String token = "valid-token";

        String newPassword = "A".repeat(31); // 31 karakter

        String passwordConfirm = "A".repeat(31);



        // When & Then

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {

            authService.resetPassword(token, newPassword, passwordConfirm);

        });



        assertEquals("Şifre en az 8, en fazla 30 karakter olmalıdır", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));

    }



    @Test

    @DisplayName("resetPassword - Şifre tam 8 karakter ise başarılı olmalı")

    void testResetPassword_PasswordExactly8Chars_ShouldSucceed() {

        // Given

        String token = "valid-token-123";

        String newPassword = "12345678"; // Tam 8 karakter

        String passwordConfirm = "12345678";

        String encodedPassword = "$2a$10$encodedNewPassword";



        testUser.setPasswordResetToken(token);

        testUser.setPasswordResetExpires(LocalDateTime.now().plusMinutes(15));



        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.of(testUser));

        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        when(userRepository.save(any(User.class))).thenReturn(testUser);



        // When & Then

        assertDoesNotThrow(() -> {

            authService.resetPassword(token, newPassword, passwordConfirm);

        });



        verify(passwordEncoder, times(1)).encode(newPassword);

        verify(userRepository, times(1)).save(any(User.class));

    }



    @Test

    @DisplayName("resetPassword - Şifre tam 30 karakter ise başarılı olmalı")

    void testResetPassword_PasswordExactly30Chars_ShouldSucceed() {

        // Given

        String token = "valid-token-123";

        String newPassword = "A".repeat(30); // Tam 30 karakter

        String passwordConfirm = "A".repeat(30);

        String encodedPassword = "$2a$10$encodedNewPassword";



        testUser.setPasswordResetToken(token);

        testUser.setPasswordResetExpires(LocalDateTime.now().plusMinutes(15));



        when(userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(

                eq(token), any(LocalDateTime.class))).thenReturn(Optional.of(testUser));

        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        when(userRepository.save(any(User.class))).thenReturn(testUser);



        // When & Then

        assertDoesNotThrow(() -> {

            authService.resetPassword(token, newPassword, passwordConfirm);

        });



        verify(passwordEncoder, times(1)).encode(newPassword);

        verify(userRepository, times(1)).save(any(User.class));

    }

}



