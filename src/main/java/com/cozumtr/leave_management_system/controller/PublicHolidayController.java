package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.PublicHolidayCreateRequest;
import com.cozumtr.leave_management_system.dto.request.PublicHolidayUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.PublicHolidayResponse;
import com.cozumtr.leave_management_system.service.PublicHolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Resmi tatil (PublicHoliday) yönetimi controller'ı.
 * Tüm CRUD işlemleri sadece HR rolüne açıktır.
 */
@RestController
@RequestMapping("/api/metadata/public-holidays")
@RequiredArgsConstructor
public class PublicHolidayController {

    private final PublicHolidayService publicHolidayService;

    /**
     * Yeni resmi tatil oluşturur.
     */
    @PreAuthorize("hasRole('HR')")
    @PostMapping
    public ResponseEntity<PublicHolidayResponse> createPublicHoliday(@Valid @RequestBody PublicHolidayCreateRequest request) {
        PublicHolidayResponse response = publicHolidayService.createPublicHoliday(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Tüm resmi tatilleri getirir.
     */
    @PreAuthorize("hasRole('HR')")
    @GetMapping
    public ResponseEntity<List<PublicHolidayResponse>> getAllPublicHolidays() {
        List<PublicHolidayResponse> publicHolidays = publicHolidayService.getAllPublicHolidays();
        return ResponseEntity.ok(publicHolidays);
    }

    /**
     * ID'ye göre resmi tatil getirir.
     */
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/{id}")
    public ResponseEntity<PublicHolidayResponse> getPublicHolidayById(@PathVariable Long id) {
        PublicHolidayResponse response = publicHolidayService.getPublicHolidayById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Resmi tatil günceller.
     */
    @PreAuthorize("hasRole('HR')")
    @PutMapping("/{id}")
    public ResponseEntity<PublicHolidayResponse> updatePublicHoliday(
            @PathVariable Long id,
            @Valid @RequestBody PublicHolidayUpdateRequest request) {
        PublicHolidayResponse response = publicHolidayService.updatePublicHoliday(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Resmi tatil siler.
     */
    @PreAuthorize("hasRole('HR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePublicHoliday(@PathVariable Long id) {
        publicHolidayService.deletePublicHoliday(id);
        return ResponseEntity.noContent().build();
    }
}

