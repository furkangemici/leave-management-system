package com.cozumtr.leave_management_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email gönderme servisi
 * Şu an için placeholder - gerçek email gönderme mantığı buraya eklenecek
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Kullanıcıya aktivasyon linki gönderir
     * @param email Kullanıcı email'i
     * @param activationToken Aktivasyon token'ı
     */
    public void sendActivationEmail(String email, String activationToken) {
        // Token ve link'i her durumda log'layabilmek için metodun başında tanımla
        String activationLink = baseUrl + "/api/auth/activate?token=" + activationToken;
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Hesap Aktivasyonu - İzin Takip Sistemi");
            message.setText(
                "Merhaba,\n\n" +
                "İzin Takip Sistemine hoş geldiniz!\n\n" +
                "Hesabınızı aktifleştirmek için aşağıdaki linke tıklayın ve şifrenizi belirleyin:\n\n" +
                activationLink + "\n\n" +
                "Bu link 24 saat geçerlidir.\n\n" +
                "İyi çalışmalar!"
            );
            
            mailSender.send(message);
            log.info("Aktivasyon email'i gönderildi: {}", email);
            log.info("🔑 TEST İÇİN TOKEN: {} | Aktivasyon Linki: {}", activationToken, activationLink);
        } catch (Exception e) {
            log.error("Email gönderme hatası: {}", e.getMessage());
            // Email gönderilemese bile işlem devam etsin (demo için)
            // Gerçek sistemde exception fırlatılabilir
            log.info("🔑 TEST İÇİN TOKEN (Email gönderilemedi ama token burada): {} | Aktivasyon Linki: {}", activationToken, activationLink);
        }
    }
}

