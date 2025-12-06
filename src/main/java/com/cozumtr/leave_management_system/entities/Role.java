package com.cozumtr.leave_management_system.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Role extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    // --- 1. USER İLİŞKİSİ (PASİF TARAF) ---
    // User sınıfındaki "roles" alanı burayı yönetiyor.
    // Burası sadece "Bu role sahip kullanıcıları göster" demek için.
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<User> users;

    // --- 2. PERMISSION İLİŞKİSİ (AKTİF/SAHİP TARAF) ---
    // Rolleri oluştururken içine yetkileri biz ekleriz.
    // Bu yüzden @JoinTable BURADA olacak.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @ToString.Exclude
    private Set<Permission> permissions = new HashSet<>();
}
