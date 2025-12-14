package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.DepartmentCreateRequest;
import com.cozumtr.leave_management_system.dto.request.DepartmentUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.DepartmentResponse;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

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

    /**
     * Tüm aktif departmanları listeler.
     * HR departman yönetimi sayfası için kullanılır.
     */
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .filter(Department::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * ID'ye göre departman getirir.
     */
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Departman bulunamadı: " + id));
        return mapToResponse(department);
    }

    /**
     * Yeni departman oluşturur.
     * Manager opsiyoneldir, atanmayabilir.
     */
    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        // Name unique kontrolü
        if (departmentRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("Bu isimde bir departman zaten mevcut: " + request.getName());
        }

        Department department = new Department();
        department.setName(request.getName());
        department.setIsActive(true);

        // Manager ataması (opsiyonel)
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new BusinessException("Yönetici bulunamadı: " + request.getManagerId()));
            
            if (!manager.getIsActive()) {
                throw new BusinessException("Pasif bir çalışan yönetici olarak atanamaz: " + request.getManagerId());
            }
            
            department.setManager(manager);
        }

        Department saved = departmentRepository.save(department);
        return mapToResponse(saved);
    }

    /**
     * Departman günceller.
     * Manager güncellemesi yapılabilir veya mevcut manager korunabilir.
     */
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Departman bulunamadı: " + id));

        // Name unique kontrolü (kendi ID'si hariç)
        departmentRepository.findByName(request.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException("Bu isimde bir departman zaten mevcut: " + request.getName());
                    }
                });

        department.setName(request.getName());

        // Manager güncellemesi (opsiyonel - null gönderilirse değiştirilmez)
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new BusinessException("Yönetici bulunamadı: " + request.getManagerId()));
            
            if (!manager.getIsActive()) {
                throw new BusinessException("Pasif bir çalışan yönetici olarak atanamaz: " + request.getManagerId());
            }
            
            department.setManager(manager);
        }
        // Eğer managerId null ise, mevcut manager korunur (değişiklik yapılmaz)

        Department updated = departmentRepository.save(department);
        return mapToResponse(updated);
    }

    /**
     * Departman siler (soft delete - isActive = false).
     * Eğer departmana bağlı aktif çalışanlar varsa silme işlemini engeller.
     */
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Departman bulunamadı: " + id));

        // Aktif çalışan kontrolü
        List<Employee> activeEmployees = employeeRepository.findByDepartmentId(id).stream()
                .filter(Employee::getIsActive)
                .collect(Collectors.toList());

        if (!activeEmployees.isEmpty()) {
            throw new BusinessException(
                    "Bu departmana bağlı " + activeEmployees.size() + " aktif çalışan bulunmaktadır. " +
                    "Departman silinemez. Önce çalışanların departmanını değiştirin veya çalışanları pasif hale getirin."
            );
        }

        department.setIsActive(false);
        departmentRepository.save(department);
    }

    private DepartmentResponse mapToResponse(Department department) {
        DepartmentResponse.DepartmentResponseBuilder builder = DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName());

        // Manager bilgilerini ekle (varsa)
        if (department.getManager() != null) {
            builder.managerId(department.getManager().getId());
            builder.managerName(department.getManager().getFirstName() + " " + department.getManager().getLastName());
        }

        return builder.build();
    }
}
