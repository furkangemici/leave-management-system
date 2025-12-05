package com.cozumtr.leave_management_system.filter;

import com.cozumtr.leave_management_system.service.CustomUserDetailsService;
import com.cozumtr.leave_management_system.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Token doğrulama filtresi
 * Her HTTP isteğinde Authorization header'ından JWT token'ı alır ve doğrular
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        // Authorization header yoksa veya Bearer ile başlamıyorsa, filtreyi atla
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // "Bearer " kısmını çıkar, sadece token'ı al
            final String jwt = authHeader.substring(7);
            
            // Token'dan email'i çıkar
            final String userEmail = jwtService.extractEmail(jwt);
            
            // Email var ve SecurityContext'te henüz authentication yoksa
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Kullanıcıyı yükle
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                
                // Token'ı doğrula (email ve expiration kontrolü)
                if (jwtService.validateToken(jwt, userEmail)) {
                    // Authentication oluştur
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // SecurityContext'e set et
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("JWT token doğrulama hatası: {}", e.getMessage());
            // Hata durumunda filtreyi devam ettir (401 dönecek)
        }
        
        filterChain.doFilter(request, response);
    }
}

