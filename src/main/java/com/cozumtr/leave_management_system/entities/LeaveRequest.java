package com.cozumtr.leave_management_system.entities;

import com.cozumtr.leave_management_system.enums.RequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Check(constraints = "end_date_time > start_date_time")
public class LeaveRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // --- İLİŞKİLER ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false) // Standart isim: employee_id
    @ToString.Exclude
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    @ToString.Exclude
    private LeaveType leaveType;

    // --- TALEP DETAYLARI ---

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    private RequestStatus requestStatus = RequestStatus.PENDING_APPROVAL;

    // --- WORKFLOW MANTIĞI ---

    @Column(name = "workflow_next_approver_role", nullable = false)
    private String workflowNextApproverRole;

    // --- TARİH VE SÜRE ---

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    // Süre: 1.5 Gün veya 3.5 Saat.
    // Eksi değer girilmesini Java tarafında da engelliyoruz.
    @Column(name = "duration_hours", nullable = false, precision = 10, scale = 2)
    @Min(value = 0, message = "Süre 0'dan küçük olamaz")
    private BigDecimal durationHours;

    // Açıklama
    // NOT: Eğer SQL Server kullanıyorsan "TEXT" yerine "NVARCHAR(MAX)" daha doğru olabilir.
    // Ama "TEXT" de çoğu veritabanında çalışır.
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
}
