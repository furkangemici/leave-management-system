package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Bir personelin tüm izinleri
    List<LeaveRequest> findByEmployeeId(Long employeeId);

    // Yönetici ekranı için: Sadece "ONAY BEKLEYENLER"i getir
    List<LeaveRequest> findByRequestStatus(RequestStatus status);

    // --- SPRINT ÇAKIŞMA SORUNUNU ÇÖZEN SORGUMUZ ---
    @Query("SELECT l FROM LeaveRequest l WHERE " +
            "l.employee.id = :personnelId " +
            "AND l.requestStatus = 'APPROVED' " +
            "AND l.startDateTime < :sprintEnd " +
            "AND l.endDateTime > :sprintStart")
    List<LeaveRequest> findLeavesInSprint(
            @Param("personnelId") Long personnelId,
            @Param("sprintStart") LocalDateTime sprintStart,
            @Param("sprintEnd") LocalDateTime sprintEnd
    );

    // YENİ İZİN İSTERKEN ÇAKIŞMA KONTROLÜ (Çok Kritik)
    // Eğer aynı tarihlerde, REDDEDİLMEMİŞ veya İPTAL EDİLMEMİŞ başka bir izni var mı?
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE " +
            "l.employee.id = :personnelId " +
            "AND l.requestStatus NOT IN ('REJECTED', 'CANCELLED') " +
            "AND l.startDateTime < :end " +
            "AND l.endDateTime > :start")
    boolean hasOverlappingLeave(@Param("personnelId") Long personnelId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);


    // Kişi ID'si sormadan, verilen tarih aralığına denk gelen TÜM onaylı izinleri getirir.
    @Query("SELECT l FROM LeaveRequest l WHERE " +
            "l.requestStatus = :status AND " +
            "l.startDateTime <= :sprintEnd AND " +
            "l.endDateTime >= :sprintStart")
    List<LeaveRequest> findOverlappingLeavesForReport(
            @Param("sprintStart") LocalDateTime sprintStart,
            @Param("sprintEnd") LocalDateTime sprintEnd,
            @Param("status") RequestStatus status
    );
}