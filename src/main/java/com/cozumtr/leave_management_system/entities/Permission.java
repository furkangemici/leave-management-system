package com.cozumtr.leave_management_system.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Permission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "permission_name", nullable = false, unique = true)
    private String permissionName;

    // NOT: Buraya "List<Role> roles" eklememize GENELDE gerek yoktur.
    // Çünkü sorguları hep tersten yaparız: "Bu rolün yetkileri ne?" deriz.
    // "Bu yetki hangi rollerde var?" diye sormayız. O yüzden burayı sade bırakıyoruz.
}
