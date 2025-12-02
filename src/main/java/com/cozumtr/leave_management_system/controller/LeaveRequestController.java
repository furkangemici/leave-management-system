package com.cozumtr.leave_management_system.controller;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    public String createLeave() {
        return "İzin talep etme ucu...";
    }

    @GetMapping("/my-leaves")
    public String getMyLeaves() {
        return "Geçmiş izinlerim listesi...";
    }

    // Bu endpoint'i yönetici veya İK çağıracak.
    @GetMapping("/approvals")
    public ResponseEntity<List<LeaveRequest>> getRequestsForApproval(){
        // --- GEÇİCİ TEST KODU (Integration öncesi) ---
        // Şu an Spring Security entegre edilmediği için, servisi test edebilmek adına
        // burada "sanki İK giriş yapmış gibi" boş bir kullanıcı oluşturup gönderiyoruz.
        // Güvenlik katmanı eklenince burası değişecek.
        User dummyUser = new User();

        List<LeaveRequest> requests = leaveRequestService.getRequestsForApproval(dummyUser);
        return ResponseEntity.ok(requests);
    }
}
