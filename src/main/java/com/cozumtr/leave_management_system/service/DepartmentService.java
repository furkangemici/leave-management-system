package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.DepartmentResponse;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public Department save(Department department) {
        return departmentRepository.save(department);
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    /**
     * Aktif tüm departmanları listeler.
     * Frontend'de dropdown, radio button, card vb. için kullanılır.
     * İK yeni eleman eklerken departman seçimi için kullanılır.
     */
    public List<DepartmentResponse> getAllActiveDepartments() {
        return departmentRepository.findAll().stream()
                .filter(Department::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .build();
    }
}
