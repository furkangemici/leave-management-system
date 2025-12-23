package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Console'a log basan mock email servisi
 * Gerçek mail servisi kurulana kadar kullanılır
 * app.email.enabled=false veya tanımlı değilse aktif olur
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void sendActivationEmail(String email, String activationToken) {
        // Frontend'in aktivasyon sayfasına yönlendiren URL
        String activationLink = frontendUrl + "/activate-account?token=" + activationToken;
        
        log.info("📧 [MOCK EMAIL] Aktivasyon Email'i");
        log.info("   Alıcı: {}", email);
        log.info("   Konu: Hesap Aktivasyonu - İzin Takip Sistemi");
        log.info("🔑 TEST İÇİN TOKEN: {}", activationToken);
        log.info("� Aktivasyon Linki: {}", activationLink);
        log.info("   Bu link 24 saat geçerlidir.");
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        // Frontend'in şifre sıfırlama sayfasına yönlendiren URL
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        log.info("📧 [MOCK EMAIL] Şifre Sıfırlama Email'i");
        log.info("   Alıcı: {}", email);
        log.info("   Konu: Şifre Sıfırlama - İzin Takip Sistemi");
        log.info("🔑 TEST İÇİN TOKEN: {}", resetToken);
        log.info("🔗 Şifre Sıfırlama Linki: {}", resetLink);
        log.info("   Bu link 15 dakika geçerlidir.");
    }

    @Override
    public void sendApprovalNotification(String approverEmail, LeaveRequest leaveRequest, String approverRole) {
        Employee employee = leaveRequest.getEmployee();
        String employeeName = employee.getFirstName() + " " + employee.getLastName();
        String leaveType = leaveRequest.getLeaveType().getName();
        String reason = leaveRequest.getReason() != null && !leaveRequest.getReason().isEmpty() 
            ? leaveRequest.getReason() : "Belirtilmemiş";
        
        log.info("📧 [MOCK EMAIL] Onay Bildirimi");
        log.info("   Alıcı: {} (Rol: {})", approverEmail, approverRole);
        log.info("   Çalışan: {}", employeeName);
        log.info("   İzin Türü: {}", leaveType);
        log.info("   Açıklama: {}", reason);
        log.info("   Link: {}/manager/dashboard?requestId={}", frontendUrl, leaveRequest.getId());
    }

    @Override
    public void sendProgressNotification(LeaveRequest leaveRequest, String approverName, String nextApproverRole) {
        Employee employee = leaveRequest.getEmployee();
        
        log.info("📧 [MOCK EMAIL] İlerleme Bildirimi");
        log.info("   Alıcı: {}", employee.getEmail());
        log.info("   Onaylayan: {}", approverName);
        log.info("   Sıradaki Onayıcı: {}", nextApproverRole);
        log.info("   Link: {}/my-leaves?requestId={}", frontendUrl, leaveRequest.getId());
    }

    @Override
    public void sendFinalDecisionNotification(LeaveRequest leaveRequest, boolean isApproved, String finalApproverName) {
        Employee employee = leaveRequest.getEmployee();
        String statusText = isApproved ? "ONAYLANDI ✅" : "REDDEDİLDİ ❌";
        
        log.info("📧 [MOCK EMAIL] Nihai Karar Bildirimi");
        log.info("   Alıcı: {}", employee.getEmail());
        log.info("   Durum: {}", statusText);
        log.info("   Karar Veren: {}", finalApproverName);
        log.info("   İzin Türü: {}", leaveRequest.getLeaveType().getName());
        log.info("   Link: {}/my-leaves?requestId={}", frontendUrl, leaveRequest.getId());
    }
}
