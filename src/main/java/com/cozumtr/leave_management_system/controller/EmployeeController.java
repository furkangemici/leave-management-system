package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.UpdateProfileRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveBalanceResponse;
import com.cozumtr.leave_management_system.dto.response.UserResponse;
import com.cozumtr.leave_management_system.service.EmployeeService;
import com.cozumtr.leave_management_system.service.LeaveEntitlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final LeaveEntitlementService leaveEntitlementService;

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getMyProfile() {
        UserResponse userResponse = employeeService.getMyProfile();
        return ResponseEntity.ok(userResponse);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {

        // ID parametresi GÖNDERMİYORUZ. Servis katmanı, token'dan (SecurityContext)
        // kimin giriş yaptığını bulup sadece onun profilini güncelleyecek.

        UserResponse updatedEmployee = employeeService.updateProfile(request);

        // Geriye sadece "Başarılı" mesajı değil, güncellenmiş verinin kendisini dönüyoruz.
        return ResponseEntity.ok(updatedEmployee);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/me/balance")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyLeaveBalances() {
        // SecurityContextHolder'dan giriş yapan kullanıcının Employee ID'sini al
        Long employeeId = employeeService.getCurrentEmployeeId();
        
        // Tüm izin türleri için bakiyeleri getir
        List<LeaveBalanceResponse> balances = leaveEntitlementService.getAllEmployeeLeaveBalances(employeeId);
        
        return ResponseEntity.ok(balances);
    }
}