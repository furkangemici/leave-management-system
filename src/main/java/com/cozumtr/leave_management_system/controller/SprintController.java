package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.CreateSprintRequest;
import com.cozumtr.leave_management_system.dto.request.UpdateSprintRequest;
import com.cozumtr.leave_management_system.dto.response.SprintResponse;
import com.cozumtr.leave_management_system.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Sprint yönetimi controller'ı.
 * Tüm CRUD işlemleri sadece MANAGER rolüne açıktır.
 * MANAGER sadece kendi departmanı için sprint oluşturabilir, güncelleyebilir ve silebilir.
 */
@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    /**
     * Yeni sprint oluşturur.
     * MANAGER ilk sprinti girerken başlangıç tarihi, bitiş tarihi ve durationWeeks belirleyebilir.
     * 
     * @param request CreateSprintRequest
     * @return SprintResponse
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<SprintResponse> createSprint(@Valid @RequestBody CreateSprintRequest request) {
        SprintResponse response = sprintService.createSprint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Tüm sprint'leri getirir.
     * MANAGER sadece kendi departmanına ait sprint'leri görür.
     * 
     * @return Sprint listesi
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public ResponseEntity<List<SprintResponse>> getAllSprints() {
        List<SprintResponse> sprints = sprintService.getAllSprints();
        return ResponseEntity.ok(sprints);
    }

    /**
     * ID'ye göre sprint getirir.
     * MANAGER sadece kendi departmanına ait sprint'leri görebilir.
     * 
     * @param sprintId Sprint ID
     * @return SprintResponse
     */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/{sprintId}")
    public ResponseEntity<SprintResponse> getSprintById(@PathVariable Long sprintId) {
        SprintResponse response = sprintService.getSprintById(sprintId);
        return ResponseEntity.ok(response);
    }

    /**
     * Sprint günceller.
     * MANAGER sadece kendi departmanına ait sprint'leri güncelleyebilir.
     * 
     * @param sprintId Sprint ID
     * @param request UpdateSprintRequest
     * @return SprintResponse
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{sprintId}")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable Long sprintId,
            @Valid @RequestBody UpdateSprintRequest request) {
        SprintResponse response = sprintService.updateSprint(sprintId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Sprint siler.
     * MANAGER sadece kendi departmanına ait sprint'leri silebilir.
     * 
     * @param sprintId Sprint ID
     * @return 204 No Content
     */
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/{sprintId}")
    public ResponseEntity<Void> deleteSprint(@PathVariable Long sprintId) {
        sprintService.deleteSprint(sprintId);
        return ResponseEntity.noContent().build();
    }
}

