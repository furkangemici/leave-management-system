package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Gerçek Gmail SMTP ile çalışan email servisi
 * app.email.enabled=true olduğunda aktif olur
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendActivationEmail(String email, String activationToken) {
        String activationLink = frontendUrl + "/activate-account?token=" + activationToken;
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
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
            log.info("✅ Aktivasyon email'i gönderildi: {}", email);
        } catch (Exception e) {
            log.error("❌ Email gönderme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Email gönderilemedi: " + e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
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
            log.info("✅ Şifre sıfırlama email'i gönderildi: {}", email);
        } catch (Exception e) {
            log.error("❌ Email gönderme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Email gönderilemedi: " + e.getMessage());
        }
    }

    @Override
    public void sendApprovalNotification(String approverEmail, LeaveRequest leaveRequest, String approverRole) {
        Employee employee = leaveRequest.getEmployee();
        String employeeName = employee.getFirstName() + " " + employee.getLastName();
        String leaveType = leaveRequest.getLeaveType().getName();
        String startDate = leaveRequest.getStartDateTime().format(DATE_FORMATTER);
        String endDate = leaveRequest.getEndDateTime().format(DATE_FORMATTER);
        String duration = leaveRequest.getDurationHours() + " saat";
        String reason = leaveRequest.getReason() != null && !leaveRequest.getReason().isEmpty() 
            ? leaveRequest.getReason() : "Belirtilmemiş";
        
        // Direkt talep detayına yönlendir
        String approvalLink = frontendUrl + "/manager/dashboard?requestId=" + leaveRequest.getId();
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(approverEmail);
            message.setSubject("🔔 Yeni İzin Talebi Onayınızı Bekliyor");
            message.setText(
                "Merhaba,\n\n" +
                "Onayınızı bekleyen yeni bir izin talebi var:\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📋 TALEP DETAYLARI\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "👤 Çalışan: " + employeeName + "\n" +
                "📝 İzin Türü: " + leaveType + "\n" +
                "📅 Başlangıç: " + startDate + "\n" +
                "📅 Bitiş: " + endDate + "\n" +
                "⏱️ Süre: " + duration + "\n" +
                "💬 Açıklama: " + reason + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Talebi değerlendirmek için:\n" +
                approvalLink + "\n\n" +
                "İyi çalışmalar!"
            );
            
            mailSender.send(message);
            log.info("✅ Onay bildirimi gönderildi: {} (Rol: {}, Talep: #{})", 
                    approverEmail, approverRole, leaveRequest.getId());
        } catch (Exception e) {
            log.error("❌ Onay bildirimi gönderilemedi: {}", e.getMessage(), e);
            // İzin süreci devam etsin, email hatası kritik değil
        }
    }

    @Override
    public void sendProgressNotification(LeaveRequest leaveRequest, String approverName, String nextApproverRole) {
        Employee employee = leaveRequest.getEmployee();
        String employeeEmail = employee.getEmail();
        String leaveType = leaveRequest.getLeaveType().getName();
        String startDate = leaveRequest.getStartDateTime().format(DATE_FORMATTER);
        String endDate = leaveRequest.getEndDateTime().format(DATE_FORMATTER);
        
        String nextApproverRoleDisplay = getRoleDisplayName(nextApproverRole);
        String myLeavesLink = frontendUrl + "/my-leaves?requestId=" + leaveRequest.getId();
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(employeeEmail);
            message.setSubject("📊 İzin Talebiniz İlerliyor");
            message.setText(
                "Merhaba " + employee.getFirstName() + ",\n\n" +
                "İzin talebiniz bir aşamayı daha geçti!\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📋 TALEP DURUMU\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📝 İzin Türü: " + leaveType + "\n" +
                "📅 Tarih: " + startDate + " - " + endDate + "\n\n" +
                "✅ " + approverName + " tarafından onaylandı\n" +
                "⏳ Şu anda " + nextApproverRoleDisplay + " onayı bekleniyor\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Talebin durumunu takip etmek için:\n" +
                myLeavesLink + "\n\n" +
                "İyi çalışmalar!"
            );
            
            mailSender.send(message);
            log.info("✅ İlerleme bildirimi gönderildi: {} (Talep: #{})", 
                    employeeEmail, leaveRequest.getId());
        } catch (Exception e) {
            log.error("❌ İlerleme bildirimi gönderilemedi: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendFinalDecisionNotification(LeaveRequest leaveRequest, boolean isApproved, String finalApproverName) {
        Employee employee = leaveRequest.getEmployee();
        String employeeEmail = employee.getEmail();
        String leaveType = leaveRequest.getLeaveType().getName();
        String startDate = leaveRequest.getStartDateTime().format(DATE_FORMATTER);
        String endDate = leaveRequest.getEndDateTime().format(DATE_FORMATTER);
        String duration = leaveRequest.getDurationHours() + " saat";
        
        String statusIcon = isApproved ? "✅" : "❌";
        String statusText = isApproved ? "ONAYLANDI" : "REDDEDİLDİ";
        String subject = isApproved ? "✅ İzin Talebiniz Onaylandı!" : "❌ İzin Talebiniz Reddedildi";
        String myLeavesLink = frontendUrl + "/my-leaves?requestId=" + leaveRequest.getId();
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(employeeEmail);
            message.setSubject(subject);
            message.setText(
                "Merhaba " + employee.getFirstName() + ",\n\n" +
                "İzin talebiniz hakkında nihai karar verildi.\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📋 TALEP SONUCU\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                statusIcon + " DURUM: " + statusText + "\n\n" +
                "📝 İzin Türü: " + leaveType + "\n" +
                "📅 Başlangıç: " + startDate + "\n" +
                "📅 Bitiş: " + endDate + "\n" +
                "⏱️ Süre: " + duration + "\n" +
                "👤 Karar Veren: " + finalApproverName + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                (isApproved ? 
                    "İzniniz onaylanmıştır. İyi tatiller dileriz! 🎉\n\n" :
                    "İzin talebiniz reddedilmiştir. Detaylar için yöneticinizle görüşebilirsiniz.\n\n") +
                "Detayları görüntülemek için:\n" +
                myLeavesLink + "\n\n" +
                "İyi çalışmalar!"
            );
            
            mailSender.send(message);
            log.info("✅ Nihai karar bildirimi gönderildi: {} (Talep: #{}, Durum: {})", 
                    employeeEmail, leaveRequest.getId(), statusText);
        } catch (Exception e) {
            log.error("❌ Nihai karar bildirimi gönderilemedi: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Rol kodunu kullanıcı dostu isme çevirir
     */
    private String getRoleDisplayName(String roleCode) {
        return switch (roleCode) {
            case "HR" -> "İnsan Kaynakları";
            case "MANAGER" -> "Yönetici";
            case "CEO" -> "Genel Müdür";
            default -> roleCode;
        };
    }
}
