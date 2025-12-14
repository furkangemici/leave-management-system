package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"employee", "roles"})
    Optional<User> findByEmployeeEmail(String email);

    /**
     * Aktivasyon token'ına göre kullanıcıyı bulur
     * Token geçerli olmalı (süresi dolmamış olmalı)
     */
    Optional<User> findByPasswordResetTokenAndPasswordResetExpiresAfter(String token, LocalDateTime dateTime);

    /**
     * Sadece aktif kullanıcıları getirir (sayfalama ile)
     */
    @EntityGraph(attributePaths = {"employee", "employee.department", "roles"})
    org.springframework.data.domain.Page<User> findAllByIsActive(Boolean isActive, org.springframework.data.domain.Pageable pageable);
}
