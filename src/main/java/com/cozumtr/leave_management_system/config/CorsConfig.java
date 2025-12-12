package com.cozumtr.leave_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@Profile("!test") // Test ortamında devre dışı (TestCorsConfig kullanılacak)
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // İzin verilen origin'ler (Frontend adresi)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        
        // İzin verilen HTTP metodları
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // İzin verilen header'lar
        // setAllowCredentials(true) ile birlikte wildcard (*) kullanılamaz, bu yüzden gerekli header'ları spesifik olarak belirtiyoruz
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",      // JWT token için gerekli
                "Content-Type",       // JSON/Form data/Multipart için
                "Accept",             // Response formatı için
                "X-Requested-With",   // AJAX istekleri için
                "Origin",             // CORS için
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        
        // Frontend'in okuyabileceği response header'ları
        // Özellikle file download için Content-Disposition gerekli
        configuration.setExposedHeaders(Arrays.asList(
                "Content-Disposition",  // File download için gerekli
                "Content-Type",         // Response content type
                "Content-Length"        // File size bilgisi
        ));
        
        // Kimlik bilgilerine (Cookie, Authorization Header vb.) izin ver
        configuration.setAllowCredentials(true);
        
        // Preflight request'lerin ne kadar süre cache'leneceği (saniye cinsinden)
        configuration.setMaxAge(3600L);
        
        // CORS yapılandırmasını tüm endpoint'lere uygula
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

