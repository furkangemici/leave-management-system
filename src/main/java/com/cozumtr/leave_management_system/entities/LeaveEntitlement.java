package com.cozumtr.leave_management_system.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_entitlements")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Check(constraints = "total_hours_entitled >= 0 AND hours_used >= 0")
public class LeaveEntitlement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "year", nullable = false) 
    private Integer year;

    @Column(name = "total_hours_entitled", nullable = false, precision = 10, scale = 2)
    @Min(value = 0, message = "Toplam hak edilen izin eksi olamaz")
    private BigDecimal totalHoursEntitled = BigDecimal.ZERO;

    @Column(name = "hours_used", nullable = false, precision = 10, scale = 2)
    @Min(value = 0, message = "Kullanılan izin miktarı eksi olamaz")
    private BigDecimal hoursUsed = BigDecimal.ZERO;

    // --- İLİŞKİLER ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @ToString.Exclude
    private Employee employee;

    // --- SANAL HESAPLAMA (Veritabanında Yok) ---
    // Kalan izni veritabanında tutmak yerine anlık hesaplıyoruz.
    // Böylece "Kullandı ama Kalan düşmedi" gibi senkronizasyon hataları asla olmaz.
    @Transient
    public BigDecimal getRemainingHours() {
        if (totalHoursEntitled == null || hoursUsed == null) {
            return BigDecimal.ZERO;
        }
        return totalHoursEntitled.subtract(hoursUsed);
    }

}
