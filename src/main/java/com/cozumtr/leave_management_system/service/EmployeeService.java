package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.UpdateProfileRequest;
import com.cozumtr.leave_management_system.dto.response.UserResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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

  
    public UserResponse getMyProfile() {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + loggedInEmail));

        return new UserResponse(employee);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        // 1. GÜVENLİK: Şu an sisteme giriş yapmış kişinin emailini alıyoruz
        // (Böylece kimse başkasının ID'sini URL'den gönderip profilini değiştiremez)
        // NOT: Eğer "Authentication is null" hatası alırsan, test için JWT token ile istek atman gerekir.
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Veritabanından bu emaile sahip çalışanı buluyoruz
        Employee employee = employeeRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + loggedInEmail));

        if (request.getPhoneNumber() != null) {
            employee.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAddress() != null) {
            employee.setAddress(request.getAddress());
        }

        Employee savedEmployee = employeeRepository.save(employee);

        return new UserResponse(savedEmployee);
    }
}