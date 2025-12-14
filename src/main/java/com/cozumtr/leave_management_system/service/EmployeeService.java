
package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.UpdateProfileRequest;
import com.cozumtr.leave_management_system.dto.response.UserResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Giriş yapan kullanıcının Employee ID'sini döndürür.
     *
     * @return Çalışan ID'si
     * @throws EntityNotFoundException Eğer çalışan bulunamazsa
     */
    public Long getCurrentEmployeeId() {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + loggedInEmail));
        return employee.getId();
    }


    public UserResponse getMyProfile() {
        String loggedInEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Employee employee = employeeRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + loggedInEmail));

        return mapToResponse(employee);
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

        // 3. Güncelleme işlemleri (Sadece dolu gelen alanları)
        if (request.getPhoneNumber() != null) {
            employee.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAddress() != null) {
            employee.setAddress(request.getAddress());
        }

        Employee savedEmployee = employeeRepository.save(employee);

        return mapToResponse(savedEmployee);
    }

    /**
     * Employee Entity'yi UserResponse DTO'suna çevirir.
     *
     * @param employee Employee entity
     * @return UserResponse DTO
     */
    private UserResponse mapToResponse(Employee employee) {
        String departmentName = null;
        if (employee.getDepartment() != null) {
            departmentName = employee.getDepartment().getName();
        }

        String roleName = null;
        Set<String> roles = Collections.emptySet();

        if (employee.getUser() != null && employee.getUser().getRoles() != null && !employee.getUser().getRoles().isEmpty()) {
            // İlk rolü al (Geriye dönük uyumluluk için)
            roleName = employee.getUser().getRoles().iterator().next().getRoleName();

            // Tüm rolleri al
            roles = employee.getUser().getRoles().stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toSet());
        }

        return UserResponse.builder()
                .id(employee.getId())
                .email(employee.getEmail())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .phoneNumber(employee.getPhoneNumber())
                .address(employee.getAddress())
                .departmentName(departmentName)
                .roleName(roleName)
                .roles(roles)
                .build();
    }
}