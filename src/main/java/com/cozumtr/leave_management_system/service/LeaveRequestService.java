package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;

    /**
     * Feature 12: Yöneticinin Ekranı Mantığı
     * Giren kullanıcı İK ise TÜM bekleyenleri görür.
     * Giren kullanıcı Yönetici ise SADECE KENDİ DEPARTMANINDAKİ bekleyenleri görür.
     */
    public List<LeaveRequest> getRequestsForApproval(User currentUser){
        // 1. Kullanıcının rollerini kontrol et
        boolean isHR = currentUser.getRoles().stream()
                .anyMatch(role-> role.getRoleName().equals("ROLE_HR"));
        if(isHR){
            // Sen İK'sın, tüm şirketin bekleyen taleplerini gör.
            return leaveRequestRepository.findByRequestStatus(RequestStatus.PENDING_APPROVAL);
        }else {
            // Sen Yöneticisin, sadece kendi departmanını gör.
            Long myDepartmentId = currentUser.getEmployee().getDepartment().getId();
            return leaveRequestRepository.findByEmployeeDepartmentIdAndRequestStatus(
                    myDepartmentId, RequestStatus.PENDING_APPROVAL
            );
        }
    }

}
