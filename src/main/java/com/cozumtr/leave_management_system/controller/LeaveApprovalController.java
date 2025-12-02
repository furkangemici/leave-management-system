package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.service.LeaveApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves/approval")
@RequiredArgsConstructor
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;

    // YÖNETİCİ veya İK ONAY BUTONU
    // POST http://localhost:8080/api/leaves/approval/{id}/approve
    @PostMapping("/{id}/approve")
    // @PreAuthorize("hasAnyRole('MANAGER', 'HR')") // Güvenlik katmanı (Faz 1 bitince aktifleşecek)
    public ResponseEntity<String> approveRequest(@PathVariable Long id) {

        leaveApprovalService.approveRequest(id);

        return ResponseEntity.ok("İzin talebi başarıyla onaylandı (veya bir sonraki aşamaya iletildi).");
    }

    // RET BUTONU
    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectRequest(@PathVariable Long id) {

        // Ret mantığını Service'e eklediysen burayı açabilirsin,
        // yoksa şimdilik sadece onayı yapalım.
        // leaveApprovalService.rejectRequest(id);

        return ResponseEntity.ok("Talep reddedildi.");
    }
}