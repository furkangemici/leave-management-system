package com.cozumtr.leave_management_system.entities;

import com.cozumtr.leave_management_system.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_approval_history")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class LeaveApprovalHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // --- İLİŞKİLER ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @ToString.Exclude
    private LeaveRequest leaveRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @ToString.Exclude
    private Employee approver;

    // --- İŞLEM DETAYLARI ---

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private RequestStatus action;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
}
