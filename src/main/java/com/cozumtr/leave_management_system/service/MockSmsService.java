package com.cozumtr.leave_management_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev", "default", "test"})
@Slf4j
public class MockSmsService implements SmsService {

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("[MOCK SMS] Numara: {}, Mesaj: {}", phoneNumber, message);
    }
}

