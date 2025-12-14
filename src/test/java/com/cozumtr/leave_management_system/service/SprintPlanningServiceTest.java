package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Sprint;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.SprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SprintPlanningService Unit Tests")
class SprintPlanningServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private SprintPlanningService sprintPlanningService;

    private Department testDepartment;
    private Sprint latestSprint;

    @BeforeEach
    void setUp() {
        testDepartment = new Department();
        testDepartment.setId(1L);
        testDepartment.setName("IT Department");
        testDepartment.setIsActive(true);

        latestSprint = new Sprint();
        latestSprint.setId(1L);
        latestSprint.setName("Sprint 3 - IT Department - 2024");
        latestSprint.setStartDate(LocalDate.of(2024, 1, 1));
        latestSprint.setEndDate(LocalDate.of(2024, 1, 31));
        latestSprint.setDurationWeeks(4);
        latestSprint.setDepartment(testDepartment);
    }

    // ========== CREATE SPRINTS FOR DEPARTMENT TESTS ==========

    @Test
    @DisplayName("createSprintsForDepartment - Departman için sprint yoksa hiç sprint oluşturmamalı")
    void createSprintsForDepartment_NoExistingSprints_ShouldReturnZero() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(sprintRepository.findAllByDepartmentIdOrderByEndDateDesc(1L)).thenReturn(List.of());

        // Act
        int created = sprintPlanningService.createSprintsForDepartment(1L);

        // Assert
        assertEquals(0, created);
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    @DisplayName("createSprintsForDepartment - durationWeeks yoksa hiç sprint oluşturmamalı")
    void createSprintsForDepartment_NoDurationWeeks_ShouldReturnZero() {
        // Arrange
        latestSprint.setDurationWeeks(null);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(sprintRepository.findAllByDepartmentIdOrderByEndDateDesc(1L)).thenReturn(List.of(latestSprint));

        // Act
        int created = sprintPlanningService.createSprintsForDepartment(1L);

        // Assert
        assertEquals(0, created);
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    @DisplayName("createSprintsForDepartment - En son sprint'in adından numara çıkarıp yeni sprint'ler oluşturmalı")
    void createSprintsForDepartment_ShouldCreateSprintsWithCorrectNumbers() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(sprintRepository.findAllByDepartmentIdOrderByEndDateDesc(1L)).thenReturn(List.of(latestSprint));
        
        // Mock save işlemi
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> {
            Sprint sprint = invocation.getArgument(0);
            sprint.setId(100L);
            return sprint;
        });

        // Act
        int created = sprintPlanningService.createSprintsForDepartment(1L);

        // Assert
        assertTrue(created > 0);
        
        // Kaydedilen sprint'leri kontrol et
        ArgumentCaptor<Sprint> sprintCaptor = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository, atLeastOnce()).save(sprintCaptor.capture());
        
        List<Sprint> savedSprints = sprintCaptor.getAllValues();
        assertFalse(savedSprints.isEmpty());
        
        // İlk oluşturulan sprint'in adı "Sprint 4" olmalı (en son sprint 3'tü)
        Sprint firstCreated = savedSprints.get(0);
        assertTrue(firstCreated.getName().contains("Sprint 4"));
        assertEquals(4, firstCreated.getDurationWeeks()); // durationWeeks miras alınmalı
        assertEquals(LocalDate.of(2024, 2, 1), firstCreated.getStartDate()); // En son sprint'in bitişi + 1 gün
    }

    @Test
    @DisplayName("createSprintsForDepartment - Yeni sprint'ler durationWeeks değerini miras almalı")
    void createSprintsForDepartment_ShouldInheritDurationWeeks() {
        // Arrange
        latestSprint.setDurationWeeks(3); // 3 haftalık sprint
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(sprintRepository.findAllByDepartmentIdOrderByEndDateDesc(1L)).thenReturn(List.of(latestSprint));
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> {
            Sprint sprint = invocation.getArgument(0);
            sprint.setId(100L);
            return sprint;
        });

        // Act
        sprintPlanningService.createSprintsForDepartment(1L);

        // Assert
        ArgumentCaptor<Sprint> sprintCaptor = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository, atLeastOnce()).save(sprintCaptor.capture());
        
        Sprint savedSprint = sprintCaptor.getAllValues().get(0); // İlk oluşturulan sprint
        assertEquals(3, savedSprint.getDurationWeeks()); // 3 hafta miras alınmalı
    }

    @Test
    @DisplayName("createSprintsForDepartment - Tarih hesaplama doğru olmalı")
    void createSprintsForDepartment_ShouldCalculateDatesCorrectly() {
        // Arrange
        latestSprint.setEndDate(LocalDate.of(2024, 1, 31));
        latestSprint.setDurationWeeks(4);
        
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(sprintRepository.findAllByDepartmentIdOrderByEndDateDesc(1L)).thenReturn(List.of(latestSprint));
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> {
            Sprint sprint = invocation.getArgument(0);
            sprint.setId(100L);
            return sprint;
        });

        // Act
        sprintPlanningService.createSprintsForDepartment(1L);

        // Assert
        ArgumentCaptor<Sprint> sprintCaptor = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintRepository, atLeastOnce()).save(sprintCaptor.capture());
        
        // İlk oluşturulan sprint'i kontrol et (getValue() son olanı döndürür, getAllValues().get(0) ilk olanı)
        Sprint firstSavedSprint = sprintCaptor.getAllValues().get(0);
        // Yeni sprint'in başlangıcı = en son sprint'in bitişi + 1 gün
        assertEquals(LocalDate.of(2024, 2, 1), firstSavedSprint.getStartDate());
        // Yeni sprint'in bitişi = başlangıç + 4 hafta - 1 gün
        assertEquals(LocalDate.of(2024, 2, 28), firstSavedSprint.getEndDate());
    }

    @Test
    @DisplayName("createSprintsForDepartment - Departman aktif değilse sprint oluşturmamalı")
    void createSprintsForDepartment_InactiveDepartment_ShouldReturnZero() {
        // Arrange
        testDepartment.setIsActive(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));

        // Act
        int created = sprintPlanningService.createSprintsForDepartment(1L);

        // Assert
        assertEquals(0, created);
        verify(sprintRepository, never()).save(any(Sprint.class));
    }

    @Test
    @DisplayName("createSprintsForDepartment - Birden fazla sprint oluşturulduğunda numaralar sıralı olmalı")
    void createSprintsForDepartment_MultipleSprints_ShouldHaveSequentialNumbers() {
        // Arrange
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(testDepartment));
        when(sprintRepository.findAllByDepartmentIdOrderByEndDateDesc(1L)).thenReturn(List.of(latestSprint));
        
        List<Sprint> savedSprints = new ArrayList<>();
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(invocation -> {
            Sprint sprint = invocation.getArgument(0);
            sprint.setId((long) (savedSprints.size() + 100));
            savedSprints.add(sprint);
            return sprint;
        });

        // Act
        sprintPlanningService.createSprintsForDepartment(1L);

        // Assert
        assertTrue(savedSprints.size() > 1);
        
        // İlk sprint "Sprint 4" olmalı (en son sprint 3'tü)
        assertTrue(savedSprints.get(0).getName().contains("Sprint 4"));
        
        // İkinci sprint "Sprint 5" olmalı
        if (savedSprints.size() > 1) {
            assertTrue(savedSprints.get(1).getName().contains("Sprint 5"));
        }
    }

    // ========== CREATE UPCOMING SPRINTS TESTS ==========

    @Test
    @DisplayName("createUpcomingSprints - Tüm departmanlar için sprint oluşturmalı")
    void createUpcomingSprints_ShouldCreateForAllDepartments() {
        // Arrange
        Department dept1 = new Department();
        dept1.setId(1L);
        dept1.setIsActive(true);
        
        Department dept2 = new Department();
        dept2.setId(2L);
        dept2.setIsActive(true);

        when(departmentRepository.findAllDistinctDepartmentIds()).thenReturn(List.of(1L, 2L));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept1));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(dept2));
        when(sprintRepository.findAllByDepartmentIdOrderByEndDateDesc(anyLong())).thenReturn(List.of());
        
        // Act
        sprintPlanningService.createUpcomingSprints();

        // Assert
        // Her departman için en az bir kez kontrol edilmeli
        verify(departmentRepository, atLeastOnce()).findById(1L);
        verify(departmentRepository, atLeastOnce()).findById(2L);
    }
}


