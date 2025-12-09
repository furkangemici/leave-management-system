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

    @Query("""
            SELECT DISTINCT lr FROM LeaveRequest lr
            LEFT JOIN FETCH lr.leaveType lt
            LEFT JOIN FETCH lr.employee e
            LEFT JOIN FETCH e.department d
            LEFT JOIN FETCH lr.approvalHistories ah
            LEFT JOIN FETCH ah.approver app
            LEFT JOIN FETCH app.department appDept
            WHERE lr.workflowNextApproverRole IN :roles
              AND lr.requestStatus NOT IN ('REJECTED', 'CANCELLED', 'APPROVED')
            """)
    List<LeaveRequest> findByWorkflowNextApproverRoleIn(@Param("roles") List<String> roles);

    @Query("""
            SELECT DISTINCT lr FROM LeaveRequest lr
            LEFT JOIN FETCH lr.leaveType lt
            LEFT JOIN FETCH lr.employee e
            LEFT JOIN FETCH e.department d
            LEFT JOIN FETCH lr.approvalHistories ah
            LEFT JOIN FETCH ah.approver app
            LEFT JOIN FETCH app.department appDept
            WHERE lr.workflowNextApproverRole IN :roles
              AND lr.requestStatus NOT IN ('REJECTED', 'CANCELLED', 'APPROVED')
              AND e.department.id = :departmentId
            """)
    List<LeaveRequest> findByWorkflowNextApproverRoleInAndDepartmentId(
            @Param("roles") List<String> roles,
            @Param("departmentId") Long departmentId
    );

    @Query("""
            SELECT lr FROM LeaveRequest lr
            JOIN FETCH lr.employee e
            JOIN FETCH e.department d
            JOIN FETCH lr.leaveType lt
            WHERE lr.requestStatus = 'APPROVED'
              AND lr.startDateTime <= :now
              AND lr.endDateTime >= :now
            """)
    List<LeaveRequest> findCurrentlyOnLeave(@Param("now") LocalDateTime now);

    // 3. YENİ İZİN İSTERKEN ÇAKIŞMA KONTROLÜ (KRİTİK) 
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

    // 4. İZİN TÜRÜNE GÖRE AYLIK KULLANIM HESAPLAMA
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

    // 5. İZİN TÜRÜNE GÖRE YILLIK KULLANIM HESAPLAMA
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

    // 6. İZİN TÜRÜNE GÖRE AYLIK KULLANIM SAYISI
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

    // 7. EKİP İZİN TAKİBİ (TEAM VISIBILITY)
    // Belirli bir departmandaki onaylanmış izinleri getirir (güncel ve gelecekteki izinler için)
    @Query("SELECT l FROM LeaveRequest l " +
           "WHERE l.employee.department.id = :departmentId " +
           "AND l.requestStatus = 'APPROVED' " +
           "AND l.endDateTime >= :startDate")
    List<LeaveRequest> findApprovedLeavesByDepartment(
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDateTime startDate
    );

    // 8. SPRINT ÇAKIŞMA RAPORU - Çakışan Onaylı İzinleri Bulma
    // Çakışma koşulu: LeaveRequest.endDateTime >= sprintStart AND LeaveRequest.startDateTime <= sprintEnd
    @Query("""
            SELECT l FROM LeaveRequest l
            JOIN FETCH l.employee e
            JOIN FETCH l.leaveType lt
            WHERE l.requestStatus = 'APPROVED'
              AND l.endDateTime >= :sprintStart
              AND l.startDateTime <= :sprintEnd
            """)
    List<LeaveRequest> findOverlappingApprovedLeaves(
            @Param("sprintStart") LocalDateTime sprintStart,
            @Param("sprintEnd") LocalDateTime sprintEnd
    );
}