package com.cozumtr.leave_management_system.config;

import com.cozumtr.leave_management_system.service.EmailService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test profilinde email gönderimini gerçek SMTP üzerinden yapmamak için
 * basit bir stub EmailService tanımı.
 */
@Configuration
@Profile("test")
public class TestEmailConfig {

    @Bean
    public EmailService emailService() {
        return new EmailService() {
            @Override
            public void sendActivationEmail(String email, String activationToken) {
                // no-op - test ortamında gerçek mail gönderilmez
            }

            @Override
            public void sendPasswordResetEmail(String email, String resetToken) {
                // no-op - test ortamında gerçek mail gönderilmez
            }
        };
    }
}


