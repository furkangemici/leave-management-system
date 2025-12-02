package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.LoginRequestDto;
import com.cozumtr.leave_management_system.dto.request.RegisterRequestDto;
import com.cozumtr.leave_management_system.dto.response.AuthResponseDto;
import com.cozumtr.leave_management_system.dto.response.EmployeeResponseDto;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.RoleRepository;
import com.cozumtr.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;

    /**
     * 1. KULLANICI GİRİŞİ (login)
     * Spring Security doğrulamasını yapar, başarılı olursa JWT Token üretir
     */
    public AuthResponseDto login(LoginRequestDto request) {
        // Spring Security ile kimlik doğrulama
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Kullanıcıyı bul
        User user = userRepository.findByEmployeeEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + request.getEmail()));

        // Kullanıcı aktif değilse hata ver
        if (!user.getIsActive()) {
            throw new RuntimeException("Hesabınız aktif değil. Lütfen önce hesabınızı aktifleştirin.");
        }

        // Şifre kontrolü (Spring Security zaten yaptı ama ekstra kontrol)
        if (user.getPasswordHash() == null) {
            throw new RuntimeException("Şifreniz henüz belirlenmemiş. Lütfen hesabınızı aktifleştirin.");
        }

        // Rolleri al
        Set<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        // JWT Token üret
        String token = jwtService.generateToken(user.getEmployee().getEmail(), user.getId(), roles);

        // Son giriş zamanını güncelle
        user.setLastLogin(java.time.LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        // AuthResponseDto oluştur ve döndür
        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .userEmail(user.getEmployee().getEmail())
                .roles(roles)
                .build();
    }

    /**
     * 2. İLK YÖNETİCİYİ OLUŞTURMA (seedInitialUser)
     * Sadece uygulama başlangıcında çağrılır; Admin rolünü oluşturur ve ilk kullanıcıyı hash'lenmiş şifre ile kaydeder
     */
    @Transactional
    public void seedInitialUser(String adminEmail, String rawPassword) {
        // HR rolü veritabanında hazır olmalı (InitialDataSeeder tarafından oluşturuluyor)
        Role hrRole = roleRepository.findByRoleName("HR")
                .orElseThrow(() -> new IllegalStateException("HR rolü bulunamadı. InitialDataSeeder kontrol et."));

        // Email'e göre kullanıcı var mı kontrol et
        if (userRepository.findByEmployeeEmail(adminEmail).isPresent()) {
            log.warn("İlk kullanıcı zaten mevcut: {}", adminEmail);
            return;
        }

        // Employee oluştur (basit bir yapı - demo için)
        Employee employee = new Employee();
        employee.setFirstName("Admin");
        employee.setLastName("User");
        employee.setEmail(adminEmail);
        employee.setJobTitle("İnsan Kaynakları");
        employee.setBirthDate(LocalDate.now().minusYears(30)); // Demo için
        employee.setHireDate(LocalDate.now());
        employee.setDailyWorkHours(java.math.BigDecimal.valueOf(8.0));
        employee.setIsActive(true);

        // İlk departmanı al veya oluştur (demo için)
        Department department = departmentRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Department dept = new Department();
                    dept.setName("İnsan Kaynakları");
                    dept.setIsActive(true);
                    return departmentRepository.save(dept);
                });
        employee.setDepartment(department);

        employee = employeeRepository.save(employee);

        // User oluştur
        User user = new User();
        // NOT: @MapsId kullandığımız için ID'yi elle set ETMİYORUZ.
        // JPA, employee'nin ID'sini otomatik olarak user'a kopyalayacak.
        user.setEmployee(employee);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);

        // Admin rolünü ekle
        user.getRoles().add(hrRole);

        userRepository.save(user);
        log.info("İlk admin kullanıcı oluşturuldu: {}", adminEmail);
    }

    /**
     * 3. İK TARAFINDAN DAVET (inviteUser)
     * Admin (IK) tarafından yeni personel kaydını başlatır.
     * Yeni User kaydını aktif değil (isActive=false) ve passwordHash'i NULL olarak kaydeder.
     */
    @Transactional
    public EmployeeResponseDto inviteUser(RegisterRequestDto request) {
        // Email zaten kullanılıyor mu kontrol et
        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Bu email adresi zaten kullanılıyor: " + request.getEmail());
        }

        // Departmanı bul
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Departman bulunamadı: " + request.getDepartmentId()));

        // Employee oluştur
        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setJobTitle(request.getJobTitle());
        employee.setBirthDate(LocalDate.now().minusYears(25)); // Demo için - gerçek sistemde request'ten gelecek
        employee.setHireDate(LocalDate.now());
        employee.setDailyWorkHours(request.getDailyWorkHours());
        employee.setIsActive(false); // Henüz aktif değil

        employee.setDepartment(department);
        employee = employeeRepository.save(employee);

        // User oluştur (aktif değil, şifre yok)
        User user = new User();
        user.setEmployee(employee);
        user.setPasswordHash(null); // Şifre henüz belirlenmedi
        user.setIsActive(false); // Aktif değil
        user.setFailedLoginAttempts(0);

        // Aktivasyon token'ı oluştur ve kaydet
        String activationToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(activationToken); // passwordResetToken alanını aktivasyon token'ı olarak kullanıyoruz
        user.setPasswordResetExpires(java.time.LocalDateTime.now().plusHours(24)); // 24 saat geçerli

        // İK'nın belirttiği rolü ekle
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Rol bulunamadı: " + request.getRoleId()));
        user.getRoles().add(role);

        userRepository.save(user);

        // Email gönder (aktivasyon linki ile)
        emailService.sendActivationEmail(employee.getEmail(), activationToken);

        log.info("Yeni kullanıcı davet edildi: {}", request.getEmail());

        // Rolleri al
        Set<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        // Departman adını al
        String departmentName = employee.getDepartment().getName();

        // Yönetici adını al (departmanın yöneticisi varsa)
        String managerFullName = null;
        if (employee.getDepartment().getManager() != null) {
            Employee manager = employee.getDepartment().getManager();
            managerFullName = manager.getFirstName() + " " + manager.getLastName();
        }

        // EmployeeResponseDto döndür
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .jobTitle(employee.getJobTitle())
                .hireDate(employee.getHireDate())
                .isActive(employee.getIsActive())
                .roles(roles)
                .departmentName(departmentName)
                .managerFullName(managerFullName)
                .dailyWorkHours(employee.getDailyWorkHours())
                .build();
    }

    /**
     * 4. AKTİVASYON VE ŞİFRE BELİRLEME (activateUserAndSetPassword)
     * Kullanıcının token'ı ile hesabını aktif eder, yeni şifresini hash'leyerek kaydeder
     * ve başarılı aktivasyon sonrası kullanıcıyı otomatik login yapar
     */
    @Transactional
    public AuthResponseDto activateUserAndSetPassword(String token, String newPassword, String passwordConfirm) {
        // Şifre tekrarı kontrolü
        if (!newPassword.equals(passwordConfirm)) {
            throw new RuntimeException("Şifre ve şifre tekrarı eşleşmiyor");
        }

        // Şifre karmaşıklığı kontrolü (en az 8 karakter - DTO'da @Size var ama servis katmanında da kontrol)
        if (newPassword.length() < 8 || newPassword.length() > 30) {
            throw new RuntimeException("Şifre en az 8, en fazla 30 karakter olmalıdır");
        }

        // Token'a göre kullanıcıyı bul (optimize edilmiş sorgu)
        User user = userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(
                        token,
                        java.time.LocalDateTime.now()
                )
                .orElseThrow(() -> new RuntimeException("Geçersiz veya süresi dolmuş aktivasyon token'ı"));

        // Şifreyi hash'le ve kaydet
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setIsActive(true);
        user.setPasswordResetToken(null); // Token'ı temizle
        user.setPasswordResetExpires(null);
        userRepository.save(user);

        // Employee'yi de aktif et
        Employee employee = user.getEmployee();
        employee.setIsActive(true);
        employeeRepository.save(employee);

        log.info("Kullanıcı hesabı aktifleştirildi: {}", employee.getEmail());

        // Rolleri al
        Set<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        // JWT Token üret (otomatik login)
        String jwtToken = jwtService.generateToken(employee.getEmail(), user.getId(), roles);

        // Son giriş zamanını güncelle
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        // AuthResponseDto döndür
        return AuthResponseDto.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .userEmail(employee.getEmail())
                .roles(roles)
                .build();
    }
}

