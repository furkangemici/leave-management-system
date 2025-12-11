package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.dto.response.LeaveTypeResponse;
import com.cozumtr.leave_management_system.dto.response.LeaveApprovalHistoryResponse;
import com.cozumtr.leave_management_system.dto.response.MessageResponseDto;
import com.cozumtr.leave_management_system.dto.response.AttachmentResponse;
import com.cozumtr.leave_management_system.dto.response.TeamLeaveResponseDTO;
import com.cozumtr.leave_management_system.dto.response.ManagerLeaveResponseDTO;
import com.cozumtr.leave_management_system.service.EmployeeService;
import com.cozumtr.leave_management_system.service.LeaveRequestService;
import com.cozumtr.leave_management_system.service.LeaveAttachmentService;
import com.cozumtr.leave_management_system.service.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveAttachmentService leaveAttachmentService;
    private final LeaveTypeService leaveTypeService;
    private final EmployeeService employeeService;

    // --- KENDİ İZİN TALEPLERİMİ LİSTELEME ---
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/me")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests() {
        List<LeaveRequestResponse> leaveRequests = leaveRequestService.getMyLeaveRequests();
        return ResponseEntity.ok(leaveRequests);
    }

    // --- İZİN TALEBİ GEÇMİŞİ ---
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','CEO')")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<LeaveApprovalHistoryResponse>> getLeaveHistory(@PathVariable Long id) {
        List<LeaveApprovalHistoryResponse> history = leaveRequestService.getLeaveApprovalHistory(id);
        return ResponseEntity.ok(history);
    }

    // --- İZİN TALEBİ OLUŞTURMA ---
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LeaveRequestResponse> createLeaveRequestJson(
            @Valid @RequestBody CreateLeaveRequest request
    ) {
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LeaveRequestResponse> createLeaveRequestMultipart(
            @Valid @RequestPart("request") CreateLeaveRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request, file);
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

    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','CEO')")
    @GetMapping("/{leaveRequestId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable Long leaveRequestId) {
        List<AttachmentResponse> attachments = leaveAttachmentService.listAttachments(leaveRequestId);
        return ResponseEntity.ok(attachments);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','CEO')")
    @PostMapping("/{leaveRequestId}/attachments")
    public ResponseEntity<MessageResponseDto> uploadAttachment(
            @PathVariable Long leaveRequestId,
            @RequestPart("file") MultipartFile file
    ) {
        leaveAttachmentService.uploadAttachment(leaveRequestId, file);
        MessageResponseDto response = MessageResponseDto.builder()
                .message("Dosya başarıyla yüklendi.")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','MANAGER','CEO')")
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        return leaveAttachmentService.downloadAttachment(attachmentId);
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

    @PreAuthorize("hasAnyRole('MANAGER','HR','CEO')")
    @GetMapping("/manager/dashboard")
    public ResponseEntity<List<ManagerLeaveResponseDTO>> getManagerDashboard() {
        List<ManagerLeaveResponseDTO> responses = leaveRequestService.getManagerDashboardRequests();
        return ResponseEntity.ok(responses);
    }

    // --- EKİP İZİN TAKİBİ (TEAM VISIBILITY) ---
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/team-calendar")
    public ResponseEntity<List<TeamLeaveResponseDTO>> getTeamCalendar() {
        Long currentEmployeeId = employeeService.getCurrentEmployeeId();
        List<TeamLeaveResponseDTO> teamLeaves = leaveRequestService.getTeamApprovedLeaves(currentEmployeeId);
        return ResponseEntity.ok(teamLeaves);
    }

    @PreAuthorize("hasAnyRole('HR','CEO')")
    @GetMapping("/company-current")
    public ResponseEntity<List<TeamLeaveResponseDTO>> getCompanyCurrentLeaves() {
        List<TeamLeaveResponseDTO> leaves = leaveRequestService.getCompanyCurrentApprovedLeaves();
        return ResponseEntity.ok(leaves);
    }
}