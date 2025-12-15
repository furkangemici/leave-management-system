package com.cozumtr.leave_management_system.security;

import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.repository.UserRepository;
import com.cozumtr.leave_management_system.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Google OAuth2 ile başarılı giriş sonrası işlemleri yöneten handler
 * JWT token oluşturur ve frontend'e redirect eder
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        log.info("🎉 OAuth2 authentication successful for: {} ({})", email, name);
        
        try {
            // Kullanıcıyı bul
            User user = userRepository.findByEmployeeEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
            
            // Kullanıcı rollerini al
            java.util.Set<String> roles = user.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .collect(java.util.stream.Collectors.toSet());
            
            // JWT token oluştur
            String token = jwtService.generateToken(email, user.getId(), roles);
            
            // Frontend'e redirect et (token ile)
            String redirectUrl = String.format("%s/oauth2/callback?token=%s", frontendUrl, token);
            
            log.info("✅ Redirecting to frontend: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            
        } catch (Exception e) {
            log.error("❌ Error during OAuth2 success handling: {}", e.getMessage(), e);
            
            // Hata durumunda login sayfasına yönlendir
            String errorUrl = String.format("%s/login?error=oauth_failed", frontendUrl);
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }
}
