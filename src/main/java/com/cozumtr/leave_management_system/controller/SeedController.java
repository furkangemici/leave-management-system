package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * İlk IK/HR yöneticisini oluşturmak için basit seed endpoint'i.
 * Sadece geliştirme/demo ortamında kullanılmalı.
 */
@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {

    private final AuthService authService;

    @PostMapping("/admin")
    public ResponseEntity<String> createInitialAdmin(
            @RequestParam String email,
            @RequestParam String password
    ) {
        authService.seedInitialUser(email, password);
        return ResponseEntity.ok("İlk admin/HR kullanıcı oluşturma isteği işlendi: " + email);
    }
}


