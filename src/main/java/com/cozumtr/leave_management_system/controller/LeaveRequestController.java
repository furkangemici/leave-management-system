package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    // --- TASK 7: İZİN TALEBİ OLUŞTURMA ---
    // URL: POST http://localhost:8080/api/leaves
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(@Valid @RequestBody CreateLeaveRequest request) {

        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

        // 201 Created (Oluşturuldu) kodu ile dönüyoruz
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- TASK 9: İZİN İPTAL ETME ---
    // URL: PUT http://localhost:8080/api/leaves/{id}/cancel
    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelLeaveRequest(@PathVariable Long id) {

        leaveRequestService.cancelLeaveRequest(id);

        return ResponseEntity.ok("İzin talebi başarıyla iptal edildi.");
    }

    // (Opsiyonel Placeholder - Task 4 için yer tutucu)
    @GetMapping("/my-leaves")
    public String getMyLeaves() {
        return "Geçmiş izinlerim listesi (Henüz implemente edilmedi)...";
    }
}