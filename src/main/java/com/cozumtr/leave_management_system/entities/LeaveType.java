package com.cozumtr.leave_management_system.entities;

import com.cozumtr.leave_management_system.enums.RequestUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class LeaveType extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "is_paid", nullable = false)
    private boolean isPaid = true;

    @Column(name = "deducts_from_annual", nullable = false)
    private boolean deductsFromAnnual = true;

    // --- ONAY AKIŞI (WORKFLOW) ---

    // SQL: NVARCHAR(MAX)
    // Amaç: Bu izin türü için kimlerin onayı lazım?
    // Veri Örneği: "ROLE_MANAGER,ROLE_HR" (Virgülle ayrılmış roller)
    // Java tarafında bunu split(",") ile bölüp sırayla onay isteyeceğiz.
    @Column(name = "workflow_definition", columnDefinition = "TEXT")
    private String workflowDefinition;

    // --- BİRİM ---

    // Bu izin türü Günlük mü yoksa Saatlik mi istenir?
    @Enumerated(EnumType.STRING)
    @Column(name = "request_unit", nullable = false)
    private RequestUnit requestUnit = RequestUnit.DAY;

}
