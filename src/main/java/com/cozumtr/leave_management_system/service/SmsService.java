package com.cozumtr.leave_management_system.service;

public interface SmsService {
    void sendSms(String phoneNumber, String message);
}

