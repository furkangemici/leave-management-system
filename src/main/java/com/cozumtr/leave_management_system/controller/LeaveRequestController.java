package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.dto.response.LeaveTypeResponse;
import com.cozumtr.leave_management_system.dto.response.MessageResponseDto;
import com.cozumtr.leave_management_system.dto.response.TeamLeaveResponseDTO;
import com.cozumtr.leave_management_system.service.EmployeeService;
import com.cozumtr.leave_management_system.service.LeaveRequestService;
import com.cozumtr.leave_management_system.service.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveTypeService leaveTypeService;
    private final EmployeeService employeeService;

    // --- KENDİ İZİN TALEPLERİMİ LİSTELEME ---
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/me")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
        List<LeaveRequestResponse> leaveRequests = leaveRequestService.getMyLeaveRequests();
        return ResponseEntity.ok(leaveRequests);
    }

    // --- İZİN TALEBİ OLUŞTURMA ---
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(@Valid @RequestBody CreateLeaveRequest request) {
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);
        // 201 Created (Oluşturuldu) kodu ile dönüyoruz
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ---  İZİN İPTAL ETME (Soft Delete) ---
    // Ownership kontrolü service katmanında yapılıyor, burada sadece rol kontrolü
    @PreAuthorize("hasRole('EMPLOYEE')")
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

    // --- İZİN TALEBİNİ ONAYLA ---
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'CEO')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String comments) {
        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(id, comments);
        return ResponseEntity.ok(response);
    }

    // --- İZİN TALEBİNİ REDDET ---
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'CEO')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String comments) {
        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(id, comments);
        return ResponseEntity.ok(response);
    }

    // --- EKİP İZİN TAKİBİ (TEAM VISIBILITY) ---
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/team-calendar")
    public ResponseEntity<List<TeamLeaveResponseDTO>> getTeamCalendar() {
        Long currentEmployeeId = employeeService.getCurrentEmployeeId();
        List<TeamLeaveResponseDTO> teamLeaves = leaveRequestService.getTeamApprovedLeaves(currentEmployeeId);
        return ResponseEntity.ok(teamLeaves);
    }
}