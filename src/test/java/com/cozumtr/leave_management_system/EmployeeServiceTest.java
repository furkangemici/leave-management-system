package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.UpdateProfileRequest;
import com.cozumtr.leave_management_system.dto.response.UserResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}