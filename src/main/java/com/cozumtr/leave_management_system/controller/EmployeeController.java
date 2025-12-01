package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.UpdateProfileRequest;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;



    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeService.save(employee);
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.findAll();
    }

    // --- YENİ EKLENEN METOT (TASK 6 - PROFİL GÜNCELLEME) ---
    // Bu metot, dışarıdan gelen güncelleme isteğini karşılar.
    // Erişim Adresi: PUT http://localhost:8080/api/employees/profile

    @PutMapping("/profile")
    public ResponseEntity<String> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {

        // NOT: İleride "Auth Slice" (Faz 1) tamamlandığında, buradaki ID'yi
        // giriş yapmış olan kullanıcının Token'ından otomatik alacağız.
        // Şimdilik testi yapabilmen için SABİT (Mock) bir ID veriyoruz: 1 numara.
        // Veritabanında ID'si 1 olan bir Employee olduğundan emin olmalısın.
        Long mockLoggedUserId = 1L;

        employeeService.updateProfile(mockLoggedUserId, request);

        return ResponseEntity.ok("Profil bilgileriniz başarıyla güncellendi.");
    }
}
