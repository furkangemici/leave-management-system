package com.cozumtr.leave_management_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
@Slf4j
public class LiveSmsService implements SmsService {

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.warn("Live SMS entegrasyonu henüz uygulanmadı. Gönderilmek istenen mesaj: {} -> {}", phoneNumber, message);
        // Burada gerçek SMS sağlayıcısına entegrasyon yapılacak.
    }
}

