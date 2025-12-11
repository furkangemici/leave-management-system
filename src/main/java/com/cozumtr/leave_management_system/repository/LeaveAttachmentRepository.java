package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.LeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, Long> {
    List<LeaveAttachment> findByLeaveRequestId(Long leaveRequestId);
}
