package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.response.HolidayTemplateResponse;
import com.cozumtr.leave_management_system.service.HolidayTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holiday-templates")
@RequiredArgsConstructor
public class HolidayTemplateController {
    
    private final HolidayTemplateService templateService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('HR')")
    public ResponseEntity<List<HolidayTemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }
}
