package com.cozumtr.leave_management_system.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class User extends BaseEntity {
    @Id
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id; // Otomatik artan DEĞİL. Employee'den kopyalayacak.

    // --- PERSONEL BAĞLANTISI ---

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // "Benim ID'm, aşağıdaki employee nesnesinin ID'sidir" der.
    @JoinColumn(name = "id")
    @ToString.Exclude
    private Employee employee;

    // --- GÜVENLİK BİLGİLERİ ---

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expires")
    private LocalDateTime passwordResetExpires;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    // --- ROLLER (Çoka-Çok İlişki) ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();
}
