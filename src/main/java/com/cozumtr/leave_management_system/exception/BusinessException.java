package com.cozumtr.leave_management_system.exception;

/**
 * İş kuralı ihlalleri için özel exception sınıfı.
 * Bakiye yetersizliği, çakışma kontrolü gibi durumlarda kullanılır.
 */
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
