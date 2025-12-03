package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.LeaveApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveApprovalHistoryRepository extends JpaRepository<LeaveApprovalHistory, Long> {
    // Belirli bir izin talebine ait tarihçeyi, oluşma sırasına göre getir.
    List<LeaveApprovalHistory> findByLeaveRequestIdOrderByCreatedAtAsc(Long requestId);

}
