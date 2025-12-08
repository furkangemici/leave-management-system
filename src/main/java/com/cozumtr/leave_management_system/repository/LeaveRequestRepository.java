package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // 1. Bir personelin geçmiş tüm izinleri
    List<LeaveRequest> findByEmployeeId(Long employeeId);

    // 2. Yönetici ekranı için: Duruma göre filtreleme (Örn: Sadece Bekleyenler)
    List<LeaveRequest> findByRequestStatus(RequestStatus status);

    // 3. SPRINT ÇAKIŞMA RAPORU
    // Belirli bir tarih aralığına (Sprint) denk gelen ONAYLI izinleri bulur.
    @Query("SELECT l FROM LeaveRequest l WHERE " +
            "l.employee.id = :employeeId " +
            "AND l.requestStatus = 'APPROVED' " +
            "AND (l.startDateTime < :sprintEnd AND l.endDateTime > :sprintStart)")
    List<LeaveRequest> findLeavesInSprint(
            @Param("employeeId") Long employeeId,
            @Param("sprintStart") LocalDateTime sprintStart,
            @Param("sprintEnd") LocalDateTime sprintEnd
    );

    // 4. YENİ İZİN İSTERKEN ÇAKIŞMA KONTROLÜ (KRİTİK) [cite: 512, 545]
    // Mantık: (YeniBaslangic < EskiBitis) VE (YeniBitis > EskiBaslangic) ise çakışma vardır.
    // İyileştirme: Durumları elle yazmak yerine parametre olarak alıyoruz (:excludedStatuses).
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
           "FROM LeaveRequest l " +
           "WHERE l.employee.id = :employeeId " +
           "AND l.requestStatus NOT IN (:excludedStatuses) " +
           "AND (:startDate < l.endDateTime AND :endDate > l.startDateTime)")
    boolean existsByEmployeeAndDateRangeOverlap(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("excludedStatuses") List<RequestStatus> excludedStatuses
    );

    // 5. İZİN TÜRÜNE GÖRE AYLIK KULLANIM HESAPLAMA
    // Belirli bir ay için onaylı izinlerin toplam süresini hesaplar (saat cinsinden)
    @Query("SELECT COALESCE(SUM(l.durationHours), 0) " +
           "FROM LeaveRequest l " +
           "WHERE l.employee.id = :employeeId " +
           "AND l.leaveType.id = :leaveTypeId " +
           "AND l.requestStatus = 'APPROVED' " +
           "AND YEAR(l.startDateTime) = :year " +
           "AND MONTH(l.startDateTime) = :month")
    BigDecimal calculateMonthlyUsageByLeaveType(
            @Param("employeeId") Long employeeId,
            @Param("leaveTypeId") Long leaveTypeId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 6. İZİN TÜRÜNE GÖRE YILLIK KULLANIM HESAPLAMA
    // Belirli bir yıl için onaylı izinlerin toplam süresini hesaplar (saat cinsinden)
    @Query("SELECT COALESCE(SUM(l.durationHours), 0) " +
           "FROM LeaveRequest l " +
           "WHERE l.employee.id = :employeeId " +
           "AND l.leaveType.id = :leaveTypeId " +
           "AND l.requestStatus = 'APPROVED' " +
           "AND YEAR(l.startDateTime) = :year")
    BigDecimal calculateYearlyUsageByLeaveType(
            @Param("employeeId") Long employeeId,
            @Param("leaveTypeId") Long leaveTypeId,
            @Param("year") int year
    );

    // 7. İZİN TÜRÜNE GÖRE AYLIK KULLANIM SAYISI
    // Belirli bir ay için onaylı mazeret izinlerinin sayısını hesaplar (ayda kaç kere alındı)
    @Query("SELECT COUNT(l) " +
           "FROM LeaveRequest l " +
           "WHERE l.employee.id = :employeeId " +
           "AND l.leaveType.id = :leaveTypeId " +
           "AND l.requestStatus = 'APPROVED' " +
           "AND YEAR(l.startDateTime) = :year " +
           "AND MONTH(l.startDateTime) = :month")
    Long countMonthlyUsageByLeaveType(
            @Param("employeeId") Long employeeId,
            @Param("leaveTypeId") Long leaveTypeId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 8. EKİP İZİN TAKİBİ (TEAM VISIBILITY)
    // Belirli bir departmandaki onaylanmış izinleri getirir (güncel ve gelecekteki izinler için)
    @Query("SELECT l FROM LeaveRequest l " +
           "WHERE l.employee.department.id = :departmentId " +
           "AND l.requestStatus = 'APPROVED' " +
           "AND l.endDateTime >= :startDate")
    List<LeaveRequest> findApprovedLeavesByDepartment(
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDateTime startDate
    );
}