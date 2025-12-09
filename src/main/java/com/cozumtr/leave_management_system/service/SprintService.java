package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.CreateSprintRequest;
import com.cozumtr.leave_management_system.dto.request.UpdateSprintRequest;
import com.cozumtr.leave_management_system.dto.response.SprintResponse;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.Sprint;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.SprintRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Giriş yapan MANAGER'ın departman ID'sini döndürür.
     * 
     * @return Departman ID'si
     * @throws BusinessException Eğer kullanıcı bulunamazsa veya departmanı yoksa
     */
    private Long getCurrentManagerDepartmentId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı: " + email));
        
        if (employee.getDepartment() == null) {
            throw new BusinessException("Kullanıcının departman bilgisi bulunamadı.");
        }
        
        return employee.getDepartment().getId();
    }

    /**
     * Sprint oluşturur.
     * MANAGER sadece kendi departmanı için sprint oluşturabilir.
     * 
     * @param request CreateSprintRequest
     * @return SprintResponse
     */
    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request) {
        // 1. Güvenlik: Giriş yapan MANAGER'ın departman ID'sini al
        Long managerDepartmentId = getCurrentManagerDepartmentId();
        
        // 2. Departmanı bul
        Department department = departmentRepository.findById(managerDepartmentId)
                .orElseThrow(() -> new BusinessException("Departman bulunamadı: " + managerDepartmentId));
        
        // 3. Tarih validasyonu
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("Bitiş tarihi başlangıç tarihinden önce olamaz!");
        }
        
        // 4. Sprint adı unique kontrolü (aynı departman içinde)
        boolean nameExists = sprintRepository.findByDepartmentId(managerDepartmentId).stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(request.getName()));
        if (nameExists) {
            throw new BusinessException("Bu departman için aynı isimde bir sprint zaten mevcut: " + request.getName());
        }
        
        // 5. Sprint entity oluştur
        Sprint sprint = new Sprint();
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setDurationWeeks(request.getDurationWeeks());
        sprint.setDepartment(department);
        
        // 6. Kaydet
        Sprint savedSprint = sprintRepository.save(sprint);
        
        // 7. Response DTO oluştur
        return mapToResponse(savedSprint);
    }

    /**
     * Sprint günceller.
     * MANAGER sadece kendi departmanına ait sprint'leri güncelleyebilir.
     * 
     * @param sprintId Sprint ID
     * @param request UpdateSprintRequest
     * @return SprintResponse
     */
    @Transactional
    public SprintResponse updateSprint(Long sprintId, UpdateSprintRequest request) {
        // 1. Güvenlik: Giriş yapan MANAGER'ın departman ID'sini al
        Long managerDepartmentId = getCurrentManagerDepartmentId();
        
        // 2. Sprint'i bul
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint bulunamadı: " + sprintId));
        
        // 3. Departman kontrolü: Sprint'in departmanı MANAGER'ın departmanı ile eşleşmeli
        if (sprint.getDepartment() == null || !sprint.getDepartment().getId().equals(managerDepartmentId)) {
            throw new BusinessException("Bu sprint'i güncelleme yetkiniz yok. Sadece kendi departmanınıza ait sprint'leri güncelleyebilirsiniz.");
        }
        
        // 4. Tarih validasyonu
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("Bitiş tarihi başlangıç tarihinden önce olamaz!");
        }
        
        // 5. Sprint adı unique kontrolü (aynı departman içinde, mevcut sprint hariç)
        boolean nameExists = sprintRepository.findByDepartmentId(managerDepartmentId).stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(request.getName()) && !s.getId().equals(sprintId));
        if (nameExists) {
            throw new BusinessException("Bu departman için aynı isimde bir sprint zaten mevcut: " + request.getName());
        }
        
        // 6. Güncelle
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setDurationWeeks(request.getDurationWeeks());
        
        Sprint updatedSprint = sprintRepository.save(sprint);
        
        // 7. Response DTO oluştur
        return mapToResponse(updatedSprint);
    }

    /**
     * Sprint'i getirir.
     * MANAGER sadece kendi departmanına ait sprint'leri görebilir.
     * 
     * @param sprintId Sprint ID
     * @return SprintResponse
     */
    @Transactional(readOnly = true)
    public SprintResponse getSprintById(Long sprintId) {
        // 1. Güvenlik: Giriş yapan MANAGER'ın departman ID'sini al
        Long managerDepartmentId = getCurrentManagerDepartmentId();
        
        // 2. Sprint'i bul
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint bulunamadı: " + sprintId));
        
        // 3. Departman kontrolü
        if (sprint.getDepartment() == null || !sprint.getDepartment().getId().equals(managerDepartmentId)) {
            throw new BusinessException("Bu sprint'i görüntüleme yetkiniz yok. Sadece kendi departmanınıza ait sprint'leri görebilirsiniz.");
        }
        
        return mapToResponse(sprint);
    }

    /**
     * MANAGER'ın departmanına ait tüm sprint'leri getirir.
     * 
     * @return Sprint listesi
     */
    @Transactional(readOnly = true)
    public List<SprintResponse> getAllSprints() {
        // 1. Güvenlik: Giriş yapan MANAGER'ın departman ID'sini al
        Long managerDepartmentId = getCurrentManagerDepartmentId();
        
        // 2. Departmana ait tüm sprint'leri getir
        List<Sprint> sprints = sprintRepository.findByDepartmentId(managerDepartmentId);
        
        // 3. Response DTO'ya map et
        return sprints.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Sprint'i siler.
     * MANAGER sadece kendi departmanına ait sprint'leri silebilir.
     * 
     * @param sprintId Sprint ID
     */
    @Transactional
    public void deleteSprint(Long sprintId) {
        // 1. Güvenlik: Giriş yapan MANAGER'ın departman ID'sini al
        Long managerDepartmentId = getCurrentManagerDepartmentId();
        
        // 2. Sprint'i bul
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new EntityNotFoundException("Sprint bulunamadı: " + sprintId));
        
        // 3. Departman kontrolü
        if (sprint.getDepartment() == null || !sprint.getDepartment().getId().equals(managerDepartmentId)) {
            throw new BusinessException("Bu sprint'i silme yetkiniz yok. Sadece kendi departmanınıza ait sprint'leri silebilirsiniz.");
        }
        
        // 4. Sil
        sprintRepository.delete(sprint);
    }

    /**
     * Sprint entity'sini SprintResponse DTO'suna map eder.
     */
    private SprintResponse mapToResponse(Sprint sprint) {
        return SprintResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .durationWeeks(sprint.getDurationWeeks())
                .departmentName(sprint.getDepartment() != null ? sprint.getDepartment().getName() : null)
                .departmentId(sprint.getDepartment() != null ? sprint.getDepartment().getId() : null)
                .build();
    }
}

