package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.dto.response.LeaveTypeResponse;
import com.cozumtr.leave_management_system.dto.response.MessageResponseDto;
import com.cozumtr.leave_management_system.service.LeaveRequestService;
import com.cozumtr.leave_management_system.service.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveTypeService leaveTypeService;

    // --- KENDİ İZİN TALEPLERİMİ LİSTELEME ---
    @GetMapping("/me")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
        List<LeaveRequestResponse> leaveRequests = leaveRequestService.getMyLeaveRequests();
        return ResponseEntity.ok(leaveRequests);
    }

    // --- İZİN TALEBİ OLUŞTURMA ---
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(@Valid @RequestBody CreateLeaveRequest request) {
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);
        // 201 Created (Oluşturuldu) kodu ile dönüyoruz
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ---  İZİN İPTAL ETME (Soft Delete) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponseDto> cancelLeaveRequest(@PathVariable Long id) {
        leaveRequestService.cancelLeaveRequest(id);
        
        MessageResponseDto response = MessageResponseDto.builder()
                .message("İzin talebi başarıyla iptal edildi.")
                .build();
        return ResponseEntity.ok(response);
    }

    // --- İZİN TÜRLERİNİ LİSTELEME (Frontend'de kullanıcıya gösterilmek için) ---
    @GetMapping("/types")
    public ResponseEntity<List<LeaveTypeResponse>> getAllLeaveTypes() {
        List<LeaveTypeResponse> leaveTypes = leaveTypeService.getAllActiveLeaveTypes();
        return ResponseEntity.ok(leaveTypes);
    }
}