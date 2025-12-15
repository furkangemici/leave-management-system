package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Google OAuth2 ile giriş yapan kullanıcıları yöneten servis
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Google'dan kullanıcı bilgilerini al
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        log.info("🔐 Google OAuth2 login attempt: {} ({})", email, name);
        
        // Kullanıcı sistemde kayıtlı mı kontrol et
        User user = userRepository.findByEmployeeEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ User not found in system: {}", email);
                    return new OAuth2AuthenticationException(
                        "Bu email adresi sistemde kayıtlı değil. Lütfen yöneticinizle iletişime geçin."
                    );
                });
        
        // Kullanıcı aktif mi kontrol et
        if (!user.getIsActive()) {
            log.error("❌ User account is inactive: {}", email);
            throw new OAuth2AuthenticationException(
                "Hesabınız aktif değil. Lütfen yöneticinizle iletişime geçin."
            );
        }
        
        log.info("✅ Google OAuth2 login successful: {}", email);
        return oAuth2User;
    }
}
