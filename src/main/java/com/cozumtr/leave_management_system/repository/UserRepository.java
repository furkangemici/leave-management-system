package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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
    
    /**
     * Belirli bir role sahip aktif kullanıcıları bulur (tüm departmanlar)
     * HR ve CEO gibi roller için kullanılır
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.employee e " +
           "JOIN FETCH e.department " +
           "JOIN u.roles r " +
           "WHERE r.roleName = :roleName " +
           "AND u.isActive = true")
    List<User> findActiveUsersByRole(@Param("roleName") String roleName);
    
    /**
     * Belirli bir role ve departmana sahip aktif kullanıcıları bulur
     * MANAGER gibi departman bazlı roller için kullanılır
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.employee e " +
           "JOIN FETCH e.department d " +
           "JOIN u.roles r " +
           "WHERE r.roleName = :roleName " +
           "AND d.id = :departmentId " +
           "AND u.isActive = true")
    List<User> findActiveUsersByRoleAndDepartment(
        @Param("roleName") String roleName, 
        @Param("departmentId") Long departmentId
    );
}

