package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.ChangePasswordRequestDto;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Profil içinden şifre değiştirme akışı.
     */
    @Transactional
    public void changePassword(ChangePasswordRequestDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmployeeEmail(email)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı: " + email));

        if (user.getPasswordHash() == null) {
            throw new BusinessException("Şifreniz henüz belirlenmemiş. Lütfen hesabınızı aktifleştirin.");
        }

        boolean matches = passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash());
        if (!matches) {
            throw new BusinessException("Mevcut şifre hatalı");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new BusinessException("Yeni şifre ve tekrarı eşleşmiyor");
        }

        String newPassword = dto.getNewPassword();
        if (newPassword.length() < 8 || newPassword.length() > 30) {
            throw new BusinessException("Şifre en az 8, en fazla 30 karakter olmalıdır");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);

        userRepository.save(user);
    }
}

