package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.LeaveRequest;

/**
 * Email gönderme servisi interface'i
 */
public interface EmailService {
    
    /**
     * Hesap aktivasyon maili gönderir
     */
    void sendActivationEmail(String email, String activationToken);
    
    /**
     * Şifre sıfırlama maili gönderir
     */
    void sendPasswordResetEmail(String email, String resetToken);
    
    /**
     * Onaycıya sıra geldiğinde bildirim maili gönderir
     * @param approverEmail Onaycının email adresi
     * @param leaveRequest İzin talebi
     * @param approverRole Onaycının rolü
     */
    void sendApprovalNotification(String approverEmail, LeaveRequest leaveRequest, String approverRole);
    
    /**
     * Talep sahibine aşamalı ilerleme bildirimi gönderir
     * @param leaveRequest İzin talebi
     * @param approverName Onaylayan kişinin adı
     * @param nextApproverRole Sıradaki onaycının rolü
     */
    void sendProgressNotification(LeaveRequest leaveRequest, String approverName, String nextApproverRole);
    
    /**
     * Talep sahibine nihai karar bildirimi gönderir
     * @param leaveRequest İzin talebi
     * @param isApproved Onaylandı mı reddedildi mi
     * @param finalApproverName Son onaylayan/reddeden kişinin adı
     */
    void sendFinalDecisionNotification(LeaveRequest leaveRequest, boolean isApproved, String finalApproverName);
}
