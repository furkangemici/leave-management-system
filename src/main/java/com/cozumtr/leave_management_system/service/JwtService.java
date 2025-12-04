package com.cozumtr.leave_management_system.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24 saat (milisaniye cinsinden)
    private Long expiration;

    /**
     * Secret key'i oluşturur
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWT Token üretir
     * @param email Kullanıcı email'i
     * @param userId Kullanıcı ID'si
     * @param roles Kullanıcı rolleri
     * @return JWT Token string'i
     */
    public String generateToken(String email, Long userId, java.util.Set<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("roles", roles);
        
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Token'dan email'i çıkarır
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Token'dan userId'yi çıkarır
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Token'dan rolleri çıkarır
     */
    @SuppressWarnings("unchecked")
    public java.util.Set<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof java.util.List) {
            return new java.util.HashSet<>((java.util.List<String>) rolesObj);
        }
        return new java.util.HashSet<>();
    }

    /**
     * Token'ın geçerliliğini kontrol eder
     */
    public Boolean validateToken(String token, String email) {
        final String tokenEmail = extractEmail(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }

    /**
     * Token'ın süresi dolmuş mu kontrol eder
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Token'dan expiration date'i çıkarır
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Token'dan belirli bir claim'i çıkarır
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Token'dan tüm claim'leri çıkarır
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

