package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    Optional<LeaveType> findByName(String name);
}
