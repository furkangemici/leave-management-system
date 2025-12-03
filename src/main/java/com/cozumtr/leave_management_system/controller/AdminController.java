package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.LeaveTypeRequestDto;
import com.cozumtr.leave_management_system.dto.request.PublicHolidayRequestDto;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.entities.PublicHoliday;
import com.cozumtr.leave_management_system.service.MetadataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MetadataService metadataService;

    // --- İZİN TÜRLERİ (LEAVE TYPES) ---

    @GetMapping("/leave-types")
    public ResponseEntity<List<LeaveType>> getAllLeaveTypes() {
        return ResponseEntity.ok(metadataService.getAllLeaveTypes());
    }

    @PostMapping("/leave-types")
    public ResponseEntity<LeaveType> createLeaveType(@Valid @RequestBody LeaveTypeRequestDto dto) {
        return ResponseEntity.ok(metadataService.createLeaveType(dto));
    }

    @DeleteMapping("/leave-types/{id}")
    public ResponseEntity<Void> deleteLeaveType(@PathVariable Long id) {
        metadataService.deleteLeaveType(id);
        return ResponseEntity.ok().build();
    }

    // --- RESMİ TATİLLER (PUBLIC HOLIDAYS) ---

    @GetMapping("/holidays")
    public ResponseEntity<List<PublicHoliday>> getAllHolidays() {
        return ResponseEntity.ok(metadataService.getAllHolidays());
    }

    @PostMapping("/holidays")
    public ResponseEntity<PublicHoliday> createHoliday(@Valid @RequestBody PublicHolidayRequestDto dto) {
        return ResponseEntity.ok(metadataService.createHoliday(dto));
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        metadataService.deleteHoliday(id);
        return ResponseEntity.ok().build();
    }
}