package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);
    
    /**
     * Veritabanındaki tüm benzersiz departman ID'lerini getirir.
     * Otomatik sprint planlama için kullanılır.
     */
    @Query("SELECT DISTINCT d.id FROM Department d WHERE d.isActive = true")
    List<Long> findAllDistinctDepartmentIds();
}
