package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.DepartmentCreateRequest;
import com.cozumtr.leave_management_system.dto.request.DepartmentUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.DepartmentResponse;
import com.cozumtr.leave_management_system.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Departman yönetimi controller'ı.
 * Tüm CRUD işlemleri sadece HR rolüne açıktır.
 */
@RestController
@RequestMapping("/api/metadata/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Yeni departman oluşturur.
     */
    @PreAuthorize("hasRole('HR')")
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Tüm departmanları getirir.
     */
    @PreAuthorize("hasRole('HR')")
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        List<DepartmentResponse> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    /**
     * ID'ye göre departman getirir.
     */
    @PreAuthorize("hasRole('HR')")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Departman günceller.
     */
    @PreAuthorize("hasRole('HR')")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        DepartmentResponse response = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Departman siler.
     * Eğer departmana bağlı aktif çalışanlar varsa silme işlemi engellenir.
     */
    @PreAuthorize("hasRole('HR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}

