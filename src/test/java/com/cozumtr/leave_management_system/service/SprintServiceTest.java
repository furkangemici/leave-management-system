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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SprintService Unit Tests")
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SprintService sprintService;

    private Employee managerEmployee;
    private Department managerDepartment;
    private Department otherDepartment;
    private Sprint existingSprint;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        // Manager'ın departmanı
        managerDepartment = new Department();
        managerDepartment.setId(1L);
        managerDepartment.setName("IT Department");
        managerDepartment.setIsActive(true);

        // Başka bir departman
        otherDepartment = new Department();
        otherDepartment.setId(2L);
        otherDepartment.setName("HR Department");
        otherDepartment.setIsActive(true);

        // Manager employee
        managerEmployee = new Employee();
        managerEmployee.setId(1L);
        managerEmployee.setEmail("manager@example.com");
        managerEmployee.setDepartment(managerDepartment);

        // Mevcut sprint (manager'ın departmanına ait)
        existingSprint = new Sprint();
        existingSprint.setId(1L);
        existingSprint.setName("Sprint 1 - IT Department - 2024");
        existingSprint.setStartDate(LocalDate.of(2024, 1, 1));
        existingSprint.setEndDate(LocalDate.of(2024, 1, 31));
        existingSprint.setDurationWeeks(4);
        existingSprint.setDepartment(managerDepartment);
    }

    // ========== CREATE SPRINT TESTS ==========

    @Test
    @DisplayName("createSprint - MANAGER kendi departmanı için sprint oluşturabilmeli")
    void createSprint_ManagerOwnDepartment_ShouldCreateSprint() {
        // Arrange
        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(managerDepartment));
        when(sprintRepository.findByDepartmentId(1L)).thenReturn(List.of()); // Henüz sprint yok
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> {
            Sprint sprint = invocation.getArgument(0);
            sprint.setId(100L);
            return sprint;
        });

        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1 - IT Department - 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .durationWeeks(4)
                .build();

        // Act
        SprintResponse response = sprintService.createSprint(request);

        // Assert
        assertNotNull(response);
        assertEquals("Sprint 1 - IT Department - 2024", response.getName());
        assertEquals(4, response.getDurationWeeks());
        assertEquals(1L, response.getDepartmentId());
        verify(sprintRepository).save(any(Sprint.class));
    }

    @Test
    @DisplayName("createSprint - Bitiş tarihi başlangıç tarihinden önce olamaz")
    void createSprint_EndDateBeforeStartDate_ShouldThrowException() {
        // Arrange
        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(managerDepartment));

        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .startDate(LocalDate.of(2024, 1, 31))
                .endDate(LocalDate.of(2024, 1, 1)) // Bitiş başlangıçtan önce
                .durationWeeks(4)
                .build();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sprintService.createSprint(request);
        });

        assertEquals("Bitiş tarihi başlangıç tarihinden önce olamaz!", exception.getMessage());
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    @DisplayName("createSprint - Aynı departman içinde aynı isimde sprint olamaz")
    void createSprint_DuplicateNameInDepartment_ShouldThrowException() {
        // Arrange
        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(managerDepartment));
        when(sprintRepository.findByDepartmentId(1L)).thenReturn(List.of(existingSprint)); // Mevcut sprint var

        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1 - IT Department - 2024") // Aynı isim
                .startDate(LocalDate.of(2024, 2, 1))
                .endDate(LocalDate.of(2024, 2, 28))
                .durationWeeks(4)
                .build();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sprintService.createSprint(request);
        });

        assertTrue(exception.getMessage().contains("aynı isimde bir sprint zaten mevcut"));
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    @DisplayName("createSprint - Kullanıcı bulunamazsa hata dönmeli")
    void createSprint_UserNotFound_ShouldThrowException() {
        // Arrange
        String email = "unknown@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());

        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .durationWeeks(4)
                .build();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sprintService.createSprint(request);
        });

        assertTrue(exception.getMessage().contains("Kullanıcı bulunamadı"));
    }

    @Test
    @DisplayName("createSprint - Kullanıcının departmanı yoksa hata dönmeli")
    void createSprint_UserHasNoDepartment_ShouldThrowException() {
        // Arrange
        String email = "manager@example.com";
        Employee employeeWithoutDepartment = new Employee();
        employeeWithoutDepartment.setEmail(email);
        employeeWithoutDepartment.setDepartment(null); // Departman yok

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employeeWithoutDepartment));

        CreateSprintRequest request = CreateSprintRequest.builder()
                .name("Sprint 1")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .durationWeeks(4)
                .build();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sprintService.createSprint(request);
        });

        assertTrue(exception.getMessage().contains("departman bilgisi bulunamadı"));
    }

    // ========== UPDATE SPRINT TESTS ==========

    @Test
    @DisplayName("updateSprint - MANAGER kendi departmanına ait sprint'i güncelleyebilmeli")
    void updateSprint_ManagerOwnDepartment_ShouldUpdateSprint() {
        // Arrange
        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(existingSprint));
        when(sprintRepository.findByDepartmentId(1L)).thenReturn(List.of(existingSprint));
        when(sprintRepository.save(any(Sprint.class))).thenReturn(existingSprint);

        UpdateSprintRequest request = UpdateSprintRequest.builder()
                .name("Sprint 1 - Updated")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .durationWeeks(3) // Güncellenmiş süre
                .build();

        // Act
        SprintResponse response = sprintService.updateSprint(1L, request);

        // Assert
        assertNotNull(response);
        verify(sprintRepository).save(any(Sprint.class));
    }

    @Test
    @DisplayName("updateSprint - MANAGER başka departmana ait sprint'i güncelleyememeli")
    void updateSprint_OtherDepartment_ShouldThrowException() {
        // Arrange
        Sprint otherDepartmentSprint = new Sprint();
        otherDepartmentSprint.setId(2L);
        otherDepartmentSprint.setDepartment(otherDepartment); // Başka departman

        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(otherDepartmentSprint));

        UpdateSprintRequest request = UpdateSprintRequest.builder()
                .name("Sprint 1")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .durationWeeks(4)
                .build();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sprintService.updateSprint(2L, request);
        });

        assertTrue(exception.getMessage().contains("güncelleme yetkiniz yok"));
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    @DisplayName("updateSprint - Sprint bulunamazsa hata dönmeli")
    void updateSprint_SprintNotFound_ShouldThrowException() {
        // Arrange
        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateSprintRequest request = UpdateSprintRequest.builder()
                .name("Sprint 1")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .durationWeeks(4)
                .build();

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            sprintService.updateSprint(999L, request);
        });

        assertTrue(exception.getMessage().contains("Sprint bulunamadı"));
    }

    // ========== GET SPRINT TESTS ==========

    @Test
    @DisplayName("getSprintById - MANAGER kendi departmanına ait sprint'i görebilmeli")
    void getSprintById_ManagerOwnDepartment_ShouldReturnSprint() {
        // Arrange
        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(existingSprint));

        // Act
        SprintResponse response = sprintService.getSprintById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Sprint 1 - IT Department - 2024", response.getName());
    }

    @Test
    @DisplayName("getSprintById - MANAGER başka departmana ait sprint'i görememeli")
    void getSprintById_OtherDepartment_ShouldThrowException() {
        // Arrange
        Sprint otherDepartmentSprint = new Sprint();
        otherDepartmentSprint.setId(2L);
        otherDepartmentSprint.setDepartment(otherDepartment);

        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(otherDepartmentSprint));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sprintService.getSprintById(2L);
        });

        assertTrue(exception.getMessage().contains("görüntüleme yetkiniz yok"));
    }

    // ========== GET ALL SPRINTS TESTS ==========

    @Test
    @DisplayName("getAllSprints - MANAGER sadece kendi departmanına ait sprint'leri görmeli")
    void getAllSprints_ShouldReturnOnlyManagerDepartmentSprints() {
        // Arrange
        Sprint sprint2 = new Sprint();
        sprint2.setId(2L);
        sprint2.setName("Sprint 2 - IT Department - 2024");
        sprint2.setDepartment(managerDepartment);

        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(sprintRepository.findByDepartmentId(1L)).thenReturn(List.of(existingSprint, sprint2));

        // Act
        List<SprintResponse> responses = sprintService.getAllSprints();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertTrue(responses.stream().allMatch(r -> r.getDepartmentId().equals(1L)));
    }

    // ========== DELETE SPRINT TESTS ==========

    @Test
    @DisplayName("deleteSprint - MANAGER kendi departmanına ait sprint'i silebilmeli")
    void deleteSprint_ManagerOwnDepartment_ShouldDeleteSprint() {
        // Arrange
        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(existingSprint));

        // Act
        assertDoesNotThrow(() -> {
            sprintService.deleteSprint(1L);
        });

        // Assert
        verify(sprintRepository).delete(existingSprint);
    }

    @Test
    @DisplayName("deleteSprint - MANAGER başka departmana ait sprint'i silememeli")
    void deleteSprint_OtherDepartment_ShouldThrowException() {
        // Arrange
        Sprint otherDepartmentSprint = new Sprint();
        otherDepartmentSprint.setId(2L);
        otherDepartmentSprint.setDepartment(otherDepartment);

        String email = "manager@example.com";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(managerEmployee));
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(otherDepartmentSprint));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sprintService.deleteSprint(2L);
        });

        assertTrue(exception.getMessage().contains("silme yetkiniz yok"));
        verify(sprintRepository, never()).delete(any(Sprint.class));
    }
}

