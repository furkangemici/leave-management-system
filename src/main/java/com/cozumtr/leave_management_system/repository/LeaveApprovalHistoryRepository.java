package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.LeaveApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveApprovalHistoryRepository extends JpaRepository<LeaveApprovalHistory, Long> {
    java.util.List<LeaveApprovalHistory> findByLeaveRequestIdOrderByCreatedAtAsc(Long leaveRequestId);
}
