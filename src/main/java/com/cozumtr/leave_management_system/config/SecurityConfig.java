package com.cozumtr.leave_management_system.config;

import com.cozumtr.leave_management_system.filter.JwtAuthenticationFilter;
import com.cozumtr.leave_management_system.security.OAuth2SuccessHandler;
import com.cozumtr.leave_management_system.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // JWT kullandığımız için CSRF'e gerek yok
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // CORS yapılandırmasını etkinleştir
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless - Session kullanma
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Public endpoint'ler (herkes erişebilir)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/api-docs/**",
                                "/api-docs",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/auth/login",              // Giriş herkese açık
                                "/api/auth/activate",           // Aktivasyon herkese açık
                                "/api/auth/refresh",            // Token yenileme herkese açık
                                "/api/auth/forgot-password",    // Şifremi unuttum - talep etme
                                "/api/auth/validate-reset-token", // Token doğrulama
                                "/api/auth/reset-password",     // Şifre sıfırlama
                                "/api/seed/**",
                                "/login/oauth2/**",             // OAuth2 login endpoints
                                "/oauth2/**"                     // OAuth2 callback endpoints
                        ).permitAll()

                        // 2. İK/Admin endpoint'leri (sadece yetkili kullanıcılar)
                        .requestMatchers("/api/auth/invite", "/api/auth/roles", "/api/auth/departments")
                                .hasRole("HR")

                        // 3. Tüm diğer API endpoint'leri için JWT token gerekli
                        // Rol kontrolleri @PreAuthorize ile metot seviyesinde yapılıyor
                        .anyRequest().authenticated()
                )
                // OAuth2 Login Configuration
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                )
                // JWT Filter'ı ekle (UsernamePasswordAuthenticationFilter'dan önce)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Form login ve Basic Auth'u kaldır (JWT kullanıyoruz)
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // Şifreleme makinesi (Veritabanına personel kaydederken lazım olacak)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationProvider - Spring Security'nin kullanıcı doğrulaması için
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // AuthenticationManager - Login işlemi için gerekli
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}