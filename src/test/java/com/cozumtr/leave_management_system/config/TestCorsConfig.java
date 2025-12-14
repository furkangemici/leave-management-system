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
 * Test ortamÄ±nda CORS yapÄ±landÄ±rmasÄ±.
 * Test istekleri aynÄ± origin'den geldiÄŸi iÃ§in CORS kontrolÃ¼ne gerek yoktur.
 * Bu yapÄ±landÄ±rma tÃ¼m origin'lere izin verir ve testlerin sorunsuz Ã§alÄ±ÅŸmasÄ±nÄ± saÄŸlar.
 */
@Configuration
@Profile("test")
public class TestCorsConfig {

    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Test ortamÄ±nda tÃ¼m origin'lere izin ver
        configuration.setAllowedOrigins(Arrays.asList("*"));
        
        // TÃ¼m HTTP metodlarÄ±na izin ver
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // TÃ¼m header'lara izin ver
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Test ortamÄ±nda credentials gerekmez
        configuration.setAllowCredentials(false);
        
        // Preflight cache
        configuration.setMaxAge(3600L);
        
        // CORS yapÄ±landÄ±rmasÄ±nÄ± tÃ¼m endpoint'lere uygula
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}


