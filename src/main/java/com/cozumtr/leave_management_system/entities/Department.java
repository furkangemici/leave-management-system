package com.cozumtr.leave_management_system.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    // --- İLİŞKİLER ---

    // YÖNETİCİ İLİŞKİSİ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_personnel_id")
    @ToString.Exclude
    private Employee manager;

    // 1 ÇALIŞAN LİSTESİ
    @OneToMany(mappedBy = "department")
    @ToString.Exclude
    private List<Employee> employees;

    // 2. SPRINT LİSTESİ
    @OneToMany(mappedBy = "department")
    @ToString.Exclude
    private List<Sprint> sprints;
}