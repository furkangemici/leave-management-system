package com.cozumtr.leave_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Test ortamında CORS yapılandırması.
 * Test istekleri aynı origin'den geldiği için CORS kontrolüne gerek yoktur.
 * Bu yapılandırma tüm origin'lere izin verir ve testlerin sorunsuz çalışmasını sağlar.
 */
@Configuration
@Profile("test")
public class TestCorsConfig {

    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Test ortamında tüm origin'lere izin ver
        configuration.setAllowedOrigins(Arrays.asList("*"));
        
        // Tüm HTTP metodlarına izin ver
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Tüm header'lara izin ver
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Test ortamında credentials gerekmez
        configuration.setAllowCredentials(false);
        
        // Preflight cache
        configuration.setMaxAge(3600L);
        
        // CORS yapılandırmasını tüm endpoint'lere uygula
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

