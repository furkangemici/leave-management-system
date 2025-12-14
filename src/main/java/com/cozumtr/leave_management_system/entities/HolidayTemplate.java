package com.cozumtr.leave_management_system.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "holiday_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class HolidayTemplate extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private Integer durationDays;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean isHalfDayBefore = false;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean isMovable = false;
    
    @Column(length = 5)
    private String fixedDate;
}
