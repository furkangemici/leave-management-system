package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.DepartmentCreateRequest;
import com.cozumtr.leave_management_system.dto.request.DepartmentUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.DepartmentResponse;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService Unit Tests")
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department testDepartment;
    private Employee testManager;
    private DepartmentCreateRequest createRequest;
    private DepartmentUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testDepartment = new Department();
        testDepartment.setId(1L);
        testDepartment.setName("IT Department");
        testDepartment.setIsActive(true);

        testManager = new Employee();
        testManager.setId(10L);
        testManager.setFirstName("John");
        testManager.setLastName("Doe");
        testManager.setEmail("john.doe@example.com");
        testManager.setIsActive(true);
        testManager.setDailyWorkHours(new BigDecimal("8.0"));
        testManager.setHireDate(LocalDate.of(2020, 1, 1));

        testDepartment.setManager(testManager);

        createRequest = DepartmentCreateRequest.builder()
                .name("HR Department")
                .managerId(null) // Manager opsiyonel
                .build();

        updateRequest = DepartmentUpdateRequest.builder()
                .name("Updated Department")
                .managerId(null) // Manager opsiyonel
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("createDepartment - Başarılı oluşturma (manager olmadan)")
    void createDepartment_Success_WithoutManager() {
        // Arrange
        when(departmentRepository.findByName(createRequest.getName())).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // Act
        DepartmentResponse response = departmentService.createDepartment(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(createRequest.getName(), response.getName());
        assertNull(response.getManagerId());
        verify(departmentRepository).findByName(createRequest.getName());
        verify(departmentRepository).save(any(Department.class));
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createDepartment - Başarılı oluşturma (manager ile)")
    void createDepartment_Success_WithManager() {
        // Arrange
        createRequest.setManagerId(10L);
        when(departmentRepository.findByName(createRequest.getName())).thenReturn(Optional.empty());
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(testManager));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setManager(testManager);
            return saved;
        });

        // Act
        DepartmentResponse response = departmentService.createDepartment(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(createRequest.getName(), response.getName());
        verify(departmentRepository).findByName(createRequest.getName());
        verify(employeeRepository).findById(10L);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("createDepartment - Name unique kontrolü başarısız")
    void createDepartment_DuplicateName_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findByName(createRequest.getName())).thenReturn(Optional.of(testDepartment));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            departmentService.createDepartment(createRequest);
        });

        assertEquals("Bu isimde bir departman zaten mevcut: " + createRequest.getName(), exception.getMessage());
        verify(departmentRepository).findByName(createRequest.getName());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("createDepartment - Manager bulunamadı")
    void createDepartment_ManagerNotFound_ShouldThrowException() {
        // Arrange
        createRequest.setManagerId(999L);
        when(departmentRepository.findByName(createRequest.getName())).thenReturn(Optional.empty());
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            departmentService.createDepartment(createRequest);
        });

        assertEquals("Yönetici bulunamadı: 999", exception.getMessage());
        verify(employeeRepository).findById(999L);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("createDepartment - Pasif manager atanamaz")
    void createDepartment_InactiveManager_ShouldThrowException() {
        // Arrange
        testManager.setIsActive(false);
        createRequest.setManagerId(10L);
        when(departmentRepository.findByName(createRequest.getName())).thenReturn(Optional.empty());
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(testManager));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            departmentService.createDepartment(createRequest);
        });

        assertEquals("Pasif bir çalışan yönetici olarak atanamaz: 10", exception.getMessage());
        verify(employeeRepository).findById(10L);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("getAllDepartments - Tüm departmanları getirir")
    void getAllDepartments_Success() {
        // Arrange
        Department dept2 = new Department();
        dept2.setId(2L);
        dept2.setName("HR Department");
        dept2.setIsActive(true);

        when(departmentRepository.findAll()).thenReturn(Arrays.asList(testDepartment, dept2));

        // Act
        List<DepartmentResponse> responses = departmentService.getAllDepartments();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(departmentRepository).findAll();
    }

    @Test
    @DisplayName("getDepartmentById - Başarılı getirme")
    void getDepartmentById_Success() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));

        // Act
        DepartmentResponse response = departmentService.getDepartmentById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testDepartment.getId(), response.getId());
        assertEquals(testDepartment.getName(), response.getName());
        assertEquals(testManager.getId(), response.getManagerId());
        assertEquals("John Doe", response.getManagerName());
        verify(departmentRepository).findById(1L);
    }

    @Test
    @DisplayName("getDepartmentById - Bulunamayan ID için exception")
    void getDepartmentById_NotFound_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            departmentService.getDepartmentById(999L);
        });

        assertEquals("Departman bulunamadı: 999", exception.getMessage());
        verify(departmentRepository).findById(999L);
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("updateDepartment - Başarılı güncelleme (manager değiştirmeden)")
    void updateDepartment_Success_WithoutManagerChange() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.findByName(updateRequest.getName())).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);

        // Act
        DepartmentResponse response = departmentService.updateDepartment(1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(departmentRepository).findById(1L);
        verify(departmentRepository).findByName(updateRequest.getName());
        verify(departmentRepository).save(any(Department.class));
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("updateDepartment - Manager güncelleme")
    void updateDepartment_Success_WithManagerUpdate() {
        // Arrange
        Employee newManager = new Employee();
        newManager.setId(20L);
        newManager.setFirstName("Jane");
        newManager.setLastName("Smith");
        newManager.setIsActive(true);

        updateRequest.setManagerId(20L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.findByName(updateRequest.getName())).thenReturn(Optional.empty());
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(newManager));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department saved = invocation.getArgument(0);
            saved.setManager(newManager);
            return saved;
        });

        // Act
        DepartmentResponse response = departmentService.updateDepartment(1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(departmentRepository).findById(1L);
        verify(employeeRepository).findById(20L);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("updateDepartment - Name unique kontrolü başarısız")
    void updateDepartment_DuplicateName_ShouldThrowException() {
        // Arrange
        Department otherDept = new Department();
        otherDept.setId(2L);
        otherDept.setName("Other Department");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.findByName(updateRequest.getName())).thenReturn(Optional.of(otherDept));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            departmentService.updateDepartment(1L, updateRequest);
        });

        assertEquals("Bu isimde bir departman zaten mevcut: " + updateRequest.getName(), exception.getMessage());
        verify(departmentRepository).findById(1L);
        verify(departmentRepository).findByName(updateRequest.getName());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("updateDepartment - Manager bulunamadı")
    void updateDepartment_ManagerNotFound_ShouldThrowException() {
        // Arrange
        updateRequest.setManagerId(999L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.findByName(updateRequest.getName())).thenReturn(Optional.empty());
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            departmentService.updateDepartment(1L, updateRequest);
        });

        assertEquals("Yönetici bulunamadı: 999", exception.getMessage());
        verify(employeeRepository).findById(999L);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteDepartment - Başarılı silme (aktif çalışan yok)")
    void deleteDepartment_Success_NoActiveEmployees() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(employeeRepository.findByDepartmentId(1L)).thenReturn(Collections.emptyList());
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);

        // Act
        departmentService.deleteDepartment(1L);

        // Assert
        verify(departmentRepository).findById(1L);
        verify(employeeRepository).findByDepartmentId(1L);
        verify(departmentRepository).save(argThat(dept -> !dept.getIsActive()));
    }

    @Test
    @DisplayName("deleteDepartment - Aktif çalışanlar varsa silme engellenir")
    void deleteDepartment_WithActiveEmployees_ShouldThrowException() {
        // Arrange
        Employee activeEmployee = new Employee();
        activeEmployee.setId(30L);
        activeEmployee.setIsActive(true);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(employeeRepository.findByDepartmentId(1L)).thenReturn(Arrays.asList(activeEmployee));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            departmentService.deleteDepartment(1L);
        });

        assertTrue(exception.getMessage().contains("aktif çalışan bulunmaktadır"));
        verify(departmentRepository).findById(1L);
        verify(employeeRepository).findByDepartmentId(1L);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("deleteDepartment - Pasif çalışanlar varsa silme başarılı")
    void deleteDepartment_WithInactiveEmployees_Success() {
        // Arrange
        Employee inactiveEmployee = new Employee();
        inactiveEmployee.setId(30L);
        inactiveEmployee.setIsActive(false);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(employeeRepository.findByDepartmentId(1L)).thenReturn(Arrays.asList(inactiveEmployee));
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);

        // Act
        departmentService.deleteDepartment(1L);

        // Assert
        verify(departmentRepository).findById(1L);
        verify(employeeRepository).findByDepartmentId(1L);
        verify(departmentRepository).save(argThat(dept -> !dept.getIsActive()));
    }

    @Test
    @DisplayName("deleteDepartment - Bulunamayan ID için exception")
    void deleteDepartment_NotFound_ShouldThrowException() {
        // Arrange
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            departmentService.deleteDepartment(999L);
        });

        assertEquals("Departman bulunamadı: 999", exception.getMessage());
        verify(departmentRepository).findById(999L);
        verify(departmentRepository, never()).save(any(Department.class));
    }
}

