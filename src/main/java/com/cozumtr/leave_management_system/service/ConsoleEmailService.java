package com.cozumtr.leave_management_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Console'a log basan mock email servisi
 * Gerçek mail servisi kurulana kadar kullanılır
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ConsoleEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
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

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        // Token ve link'i her durumda log'layabilmek için metodun başında tanımla
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Şifre Sıfırlama - İzin Takip Sistemi");
            message.setText(
                "Merhaba,\n\n" +
                "Şifre sıfırlama talebiniz alınmıştır.\n\n" +
                "Şifrenizi sıfırlamak için aşağıdaki linke tıklayın:\n\n" +
                resetLink + "\n\n" +
                "Bu link 15 dakika geçerlidir.\n\n" +
                "Eğer bu talebi siz yapmadıysanız, lütfen bu e-postayı görmezden gelin.\n\n" +
                "İyi çalışmalar!"
            );
            
            mailSender.send(message);
            log.info("Şifre sıfırlama email'i gönderildi: {}", email);
            log.info("🔑 TEST İÇİN TOKEN: {} | Şifre Sıfırlama Linki: {}", resetToken, resetLink);
        } catch (Exception e) {
            log.error("Email gönderme hatası: {}", e.getMessage());
            // Email gönderilemese bile işlem devam etsin (demo için)
            // Gerçek sistemde exception fırlatılabilir
            log.info("🔑 TEST İÇİN TOKEN (Email gönderilemedi ama token burada): {} | Şifre Sıfırlama Linki: {}", resetToken, resetLink);
        }
    }
}

