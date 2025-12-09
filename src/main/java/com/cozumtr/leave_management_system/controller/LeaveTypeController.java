package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.LeaveTypeCreateRequest;
import com.cozumtr.leave_management_system.dto.request.LeaveTypeUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveTypeResponse;
import com.cozumtr.leave_management_system.service.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * İzin türü (LeaveType) yönetimi controller'ı.
 * Tüm CRUD işlemleri sadece HR rolüne açıktır.
 */
@RestController
@RequestMapping("/api/metadata/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    /**
     * Yeni izin türü oluşturur.
     */
    @PreAuthorize("hasRole('HR')")
    @PostMapping
    public ResponseEntity<LeaveTypeResponse> createLeaveType(@Valid @RequestBody LeaveTypeCreateRequest request) {
        LeaveTypeResponse response = leaveTypeService.createLeaveType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Tüm izin türlerini getirir.
     */
    @PreAuthorize("hasRole('HR')")
    @GetMapping
    public ResponseEntity<List<LeaveTypeResponse>> getAllLeaveTypes() {
        List<LeaveTypeResponse> leaveTypes = leaveTypeService.getAllLeaveTypes();
        return ResponseEntity.ok(leaveTypes);
    }

    /**
     * ID'ye göre izin türü getirir.
     */
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveTypeResponse> getLeaveTypeById(@PathVariable Long id) {
        LeaveTypeResponse response = leaveTypeService.getLeaveTypeById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * İzin türü günceller.
     */
    @PreAuthorize("hasRole('HR')")
    @PutMapping("/{id}")
    public ResponseEntity<LeaveTypeResponse> updateLeaveType(
            @PathVariable Long id,
            @Valid @RequestBody LeaveTypeUpdateRequest request) {
        LeaveTypeResponse response = leaveTypeService.updateLeaveType(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * İzin türü siler.
     */
    @PreAuthorize("hasRole('HR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaveType(@PathVariable Long id) {
        leaveTypeService.deleteLeaveType(id);
        return ResponseEntity.noContent().build();
    }
}

