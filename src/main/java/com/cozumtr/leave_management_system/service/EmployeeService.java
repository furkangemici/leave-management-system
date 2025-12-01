package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.UpdateProfileRequest;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;


    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public List<Employee> getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    // --- YENİ EKLENEN METOT (TASK 6 İÇİN) ---

    @Transactional // Veritabanı işlemini güvenli yapar
    public void updateProfile(Long employeeId, UpdateProfileRequest request) {
        // 1. Çalışanı bul
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Çalışan bulunamadı ID: " + employeeId));

        // 2. Sadece telefon ve adresi güncelle (Diğerlerine dokunma)
        if (request.getPhoneNumber() != null) {
            employee.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAddress() != null) {
            employee.setAddress(request.getAddress());
        }

        // 3. Kaydet
        employeeRepository.save(employee);
    }
}