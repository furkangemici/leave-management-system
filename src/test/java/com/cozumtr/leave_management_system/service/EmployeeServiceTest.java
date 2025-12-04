package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.UpdateProfileRequest;
import com.cozumtr.leave_management_system.dto.response.UserResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    @DisplayName("updateProfile - Kullanıcı varsa telefon ve adres güncellenmeli")
    void updateProfile_ShouldUpdatePhoneAndAddress_WhenUserExists() {
        // 1. HAZIRLIK (GIVEN)
        String email = "test@sirket.com";

        // Mock: SecurityContext'i ayarla (Sanki giriş yapılmış gibi)
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);

        // Mock: Veritabanından dönecek eski çalışan verisi
        Employee existingEmployee = new Employee();
        existingEmployee.setId(1L);
        existingEmployee.setEmail(email);
        existingEmployee.setPhoneNumber("555-0000");
        existingEmployee.setAddress("Eski Adres");

        // Repository'ye "Bu mail sorulursa bu adamı döndür" diyoruz
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(existingEmployee));

        // Repository'ye "Kaydet denirse, kaydedilen şeyi geri dön" diyoruz
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArguments()[0]);

        // İstek (Request) verisini hazırla
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("532-9999"); // Yeni Numara
        request.setAddress("Yeni Adres");   // Yeni Adres

        // 2. EYLEM (WHEN) -> Metodu çalıştır
        UserResponse response = employeeService.updateProfile(request);

        // 3. KONTROL (THEN) -> Sonuç doğru mu?
        assertNotNull(response);
        assertEquals("532-9999", response.getPhoneNumber()); // Telefon değişmiş mi?
        assertEquals("Yeni Adres", response.getAddress());   // Adres değişmiş mi?

        // Repository'nin save metodu gerçekten çağrıldı mı?
        verify(employeeRepository).save(any(Employee.class));

        System.out.println(" TEST BAŞARILI: Profil başarıyla güncellendi!");
    }

    @Test
    @DisplayName("getMyProfile - Giriş yapan kullanıcının profili dönmeli")
    void getMyProfile_ShouldReturnCurrentUser_WhenUserExists() {
        // GIVEN
        String email = "me@sirket.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);

        Employee existingEmployee = new Employee();
        existingEmployee.setId(2L);
        existingEmployee.setEmail(email);
        existingEmployee.setFirstName("Me");
        existingEmployee.setLastName("User");

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(existingEmployee));

        // WHEN
        UserResponse response = employeeService.getMyProfile();

        // THEN
        assertNotNull(response);
        assertEquals(email, response.getEmail());
        assertEquals("Me", response.getFirstName());
        assertEquals("User", response.getLastName());
    }

    @Test
    @DisplayName("updateProfile - Null alanlar mevcut veriyi ezmemeli")
    void updateProfile_ShouldNotOverwriteFields_WhenNullsProvided() {
        // GIVEN
        String email = "nulltest@sirket.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);

        Employee existingEmployee = new Employee();
        existingEmployee.setId(3L);
        existingEmployee.setEmail(email);
        existingEmployee.setPhoneNumber("111-1111");
        existingEmployee.setAddress("Eski Adres");

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber(null);        // null -> değiştirmemeli
        request.setAddress("Yeni Adres");    // sadece adres güncellenecek

        // WHEN
        UserResponse response = employeeService.updateProfile(request);

        // THEN
        assertNotNull(response);
        assertEquals("111-1111", response.getPhoneNumber()); // eski numara korunur
        assertEquals("Yeni Adres", response.getAddress());   // adres güncellenir
    }

    @Test
    @DisplayName("getMyProfile - Kullanıcı bulunamazsa EntityNotFoundException fırlatmalı")
    void getMyProfile_ShouldThrow_WhenUserNotFound() {
        // GIVEN
        String email = "notfound@sirket.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(EntityNotFoundException.class, () -> employeeService.getMyProfile());
    }
}