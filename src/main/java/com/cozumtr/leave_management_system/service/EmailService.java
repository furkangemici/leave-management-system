package com.cozumtr.leave_management_system.service;

/**
 * Email gönderme servisi interface'i
 */
public interface EmailService {
    
    void sendActivationEmail(String email, String activationToken);
    
    void sendPasswordResetEmail(String email, String resetToken);
}
