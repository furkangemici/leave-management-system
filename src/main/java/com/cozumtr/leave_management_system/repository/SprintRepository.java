package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    
    /**
     * Belirli bir departmana ait tüm sprint'leri getirir.
     */
    List<Sprint> findByDepartmentId(Long departmentId);
    
    /**
     * Belirli bir departmana ait en son bitiş tarihli sprint'i getirir.
     * Otomatik planlama için kullanılır.
     */
    @Query("SELECT s FROM Sprint s " +
           "WHERE s.department.id = :departmentId " +
           "ORDER BY s.endDate DESC")
    List<Sprint> findAllByDepartmentIdOrderByEndDateDesc(@Param("departmentId") Long departmentId);
}