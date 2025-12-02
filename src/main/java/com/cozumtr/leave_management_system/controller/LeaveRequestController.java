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

    // Task 7: İzin Talebi Oluşturma Ucu (Gerçek Logic)
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(@Valid @RequestBody CreateLeaveRequest request) {
        
        // Servise gönderip işlemi yaptırıyoruz
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);
        
        // 201 Created (Oluşturuldu) statüsü ile cevabı dönüyoruz
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

     //Task 4'te doldurulur
    @GetMapping("/my-leaves")
    public String getMyLeaves() {
        return "Geçmiş izinlerim listesi (Henüz implemente edilmedi)...";
    }
}