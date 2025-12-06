package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.LoginRequestDto;
import com.cozumtr.leave_management_system.dto.request.RegisterRequestDto;
import com.cozumtr.leave_management_system.dto.response.AuthResponseDto;
import com.cozumtr.leave_management_system.dto.response.EmployeeResponseDto;
import com.cozumtr.leave_management_system.exception.BusinessException;
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
import java.util.Optional;
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
     * Brute Force Protection: 5 başarısız denemeden sonra hesap kilitlenir
     */
    public AuthResponseDto login(LoginRequestDto request) {
        // Önce kullanıcıyı bul (kilit kontrolü için)
        User user = userRepository.findByEmployeeEmail(request.getEmail())
                .orElse(null);

        // 1. KİLİT KONTROLÜ: Kullanıcı varsa ve hesap kilitliyse direkt hata fırlat
        if (user != null && user.getFailedLoginAttempts() >= 5) {
            throw new BusinessException("Hesabınız güvenlik nedeniyle kilitlenmiştir. Lütfen 'Şifremi Unuttum' ile şifrenizi sıfırlayın.");
        }

        try {
            // Spring Security ile kimlik doğrulama
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Kullanıcıyı tekrar bul (authentication başarılı oldu)
            user = userRepository.findByEmployeeEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + request.getEmail()));

            // Kullanıcı aktif değilse hata ver
            if (!user.getIsActive()) {
                throw new BusinessException("Hesabınız aktif değil. Lütfen önce hesabınızı aktifleştirin.");
            }

            // Şifre kontrolü (Spring Security zaten yaptı ama ekstra kontrol)
            if (user.getPasswordHash() == null) {
                throw new BusinessException("Şifreniz henüz belirlenmemiş. Lütfen hesabınızı aktifleştirin.");
            }

            // 3. ŞİFRE KONTROLÜ BAŞARILI İSE: Eğer failedLoginAttempts > 0 ise sıfırla
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }

            // Rolleri al
            Set<String> roles = user.getRoles().stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toSet());

            // JWT Token üret
            String token = jwtService.generateToken(user.getEmployee().getEmail(), user.getId(), roles);

            // Son giriş zamanını güncelle
            user.setLastLogin(java.time.LocalDateTime.now());
            userRepository.save(user);

            // AuthResponseDto oluştur ve döndür
            return AuthResponseDto.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .userEmail(user.getEmployee().getEmail())
                    .roles(roles)
                    .build();

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // 2. ŞİFRE KONTROLÜ BAŞARISIZ İSE: Deneme sayacını artır
            if (user != null) {
                int failedAttempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(failedAttempts);
                user.setLastLogin(java.time.LocalDateTime.now());
                userRepository.save(user);

                // Kalan hakkı hesapla (kullanıcıya GÖSTERME)
                int remainingAttempts = 5 - failedAttempts;

                if (remainingAttempts <= 0) {
                    // 5. hatayı yaptı, hesap kilitlendi
                    log.warn("Hesap kilitlendi: {} (5 başarısız deneme)", request.getEmail());
                    throw new BusinessException("Hesabınız kilitlendi. Lütfen şifrenizi sıfırlayın.");
                } else {
                    // Genel hata mesajı (kalan hak bilgisi gösterilmez)
                    log.warn("Başarısız giriş denemesi: {} (Deneme: {}/5, Kalan: {})", 
                            request.getEmail(), failedAttempts, remainingAttempts);
                    throw new BusinessException("Giriş bilgileri hatalı. Lütfen tekrar deneyin.");
                }
            } else {
                // Kullanıcı bulunamadı - genel hata mesajı
                throw new BusinessException("Giriş bilgileri hatalı. Lütfen tekrar deneyin.");
            }
        }
    }

    /**
     * 2. İLK YÖNETİCİYİ OLUŞTURMA (seedInitialUser)
     * Sadece uygulama başlangıcında çağrılır; Admin rolünü oluşturur ve ilk kullanıcıyı hash'lenmiş şifre ile kaydeder
     */
    @Transactional
    public void seedInitialUser(String adminEmail, String rawPassword) {
        // HR rolü veritabanında hazır olmalı (InitialDataSeeder tarafından oluşturuluyor)
        Role hrRole = roleRepository.findByRoleName("HR")
                .orElseThrow(() -> new BusinessException("HR rolü bulunamadı. InitialDataSeeder kontrol et."));

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
            throw new BusinessException("Bu email adresi zaten kullanılıyor: " + request.getEmail());
        }

        // Departmanı bul
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new BusinessException("Departman bulunamadı: " + request.getDepartmentId()));

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
                .orElseThrow(() -> new BusinessException("Rol bulunamadı: " + request.getRoleId()));
        user.getRoles().add(role);

        // Default EMPLOYEE rolünü ekle (her kullanıcı aynı zamanda EMPLOYEE'dir)
        // Eğer seçilen rol zaten EMPLOYEE ise, Set duplicate'ı engelleyecektir
        Role employeeRole = roleRepository.findByRoleName("EMPLOYEE")
                .orElseThrow(() -> new BusinessException("EMPLOYEE rolü bulunamadı. InitialDataSeeder kontrol et."));
        user.getRoles().add(employeeRole);

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
            throw new BusinessException("Şifre ve şifre tekrarı eşleşmiyor");
        }

        // Şifre karmaşıklığı kontrolü (en az 8 karakter - DTO'da @Size var ama servis katmanında da kontrol)
        if (newPassword.length() < 8 || newPassword.length() > 30) {
            throw new BusinessException("Şifre en az 8, en fazla 30 karakter olmalıdır");
        }

        // Token'a göre kullanıcıyı bul (optimize edilmiş sorgu)
        User user = userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(
                        token,
                        java.time.LocalDateTime.now()
                )
                .orElseThrow(() -> new BusinessException("Geçersiz veya süresi dolmuş aktivasyon token'ı"));

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

    /**
     * 5. ŞİFREMİ UNUTTUM - TALEP ETME (forgotPassword)
     * Kullanıcı email adresini girer, sistem token oluşturur ve email gönderir
     * Güvenlik: Kullanıcı yoksa bile "Email gönderildi" mesajı döner
     */
    @Transactional
    public void forgotPassword(String email) {
        // Kullanıcıyı bul
        Optional<User> userOptional = userRepository.findByEmployeeEmail(email);

        // Güvenlik: Kullanıcı yoksa bile işlemi devam ettir (kötü niyetli kişiler kimin üye olduğunu anlayamaz)
        if (userOptional.isEmpty()) {
            log.warn("Şifre sıfırlama talebi - Kullanıcı bulunamadı: {}", email);
            // Kullanıcıya bilgi vermeden çık (güvenlik için)
            return;
        }

        User user = userOptional.get();

        // Kullanıcı aktif değilse işlem yapma
        if (!user.getIsActive()) {
            log.warn("Şifre sıfırlama talebi - Kullanıcı aktif değil: {}", email);
            return;
        }

        // Rastgele, benzersiz bir token üret
        String resetToken = UUID.randomUUID().toString();

        // Token'ı ve bitiş süresini (15 dakika) veritabanına kaydet
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpires(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // Email gönder
        emailService.sendPasswordResetEmail(email, resetToken);

        log.info("Şifre sıfırlama token'ı oluşturuldu ve email gönderildi: {}", email);
    }

    /**
     * 6. ŞİFREMİ UNUTTUM - TOKEN DOĞRULAMA (validateResetToken)
     * Frontend, sayfa yüklenirken token'ın geçerli olup olmadığını kontrol eder
     */
    public boolean validateResetToken(String token) {
        // Token'a göre kullanıcıyı bul (süresi dolmamış olmalı)
        Optional<User> userOptional = userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(
                token,
                java.time.LocalDateTime.now()
        );

        if (userOptional.isEmpty()) {
            log.warn("Geçersiz veya süresi dolmuş şifre sıfırlama token'ı: {}", token);
            return false;
        }

        User user = userOptional.get();

        // Kullanıcı aktif değilse token geçersiz
        if (!user.getIsActive()) {
            log.warn("Token geçerli ama kullanıcı aktif değil: {}", user.getEmployee().getEmail());
            return false;
        }

        log.info("Şifre sıfırlama token'ı geçerli: {}", token);
        return true;
    }

    /**
     * 7. ŞİFREMİ UNUTTUM - ŞİFREYİ SIFIRLAMA (resetPassword)
     * Kullanıcı yeni şifresini girer, sistem token'ı kontrol eder ve şifreyi günceller
     */
    @Transactional
    public void resetPassword(String token, String newPassword, String passwordConfirm) {
        // Şifre tekrarı kontrolü
        if (!newPassword.equals(passwordConfirm)) {
            throw new BusinessException("Şifre ve şifre tekrarı eşleşmiyor");
        }

        // Şifre karmaşıklığı kontrolü
        if (newPassword.length() < 8 || newPassword.length() > 30) {
            throw new BusinessException("Şifre en az 8, en fazla 30 karakter olmalıdır");
        }

        // Token'a göre kullanıcıyı bul (süresi dolmamış olmalı)
        User user = userRepository.findByPasswordResetTokenAndPasswordResetExpiresAfter(
                        token,
                        java.time.LocalDateTime.now()
                )
                .orElseThrow(() -> new BusinessException("Geçersiz veya süresi dolmuş şifre sıfırlama token'ı"));

        // Kullanıcı aktif değilse hata ver
        if (!user.getIsActive()) {
            throw new BusinessException("Hesabınız aktif değil");
        }

        // Yeni şifreyi hash'le ve kaydet
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // Kritik: Kullanılan token'ı ve süresini veritabanından sil (Token'ı NULL yap)
        // Böylece o link bir daha kullanılamaz
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);

        // Başarısız giriş denemelerini sıfırla
        user.setFailedLoginAttempts(0);

        userRepository.save(user);

        log.info("Şifre başarıyla sıfırlandı: {}", user.getEmployee().getEmail());
    }
}

