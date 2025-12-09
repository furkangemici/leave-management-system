package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Sprint;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.SprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SprintPlanningService Integration Tests - H2 In-Memory Database ile")
class SprintPlanningServiceIntegrationTest {

    @Autowired
    private SprintPlanningService sprintPlanningService;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department testDepartment;

    @BeforeEach
    void setUp() {
        // Temizlik
        sprintRepository.deleteAll();
        departmentRepository.deleteAll();

        // Test departmanı oluştur
        testDepartment = new Department();
        testDepartment.setName("IT Department");
        testDepartment.setIsActive(true);
        testDepartment = departmentRepository.save(testDepartment);
    }

    @Test
    @DisplayName("createSprintsForDepartment - İlk sprint yoksa hiç sprint oluşturmamalı")
    void createSprintsForDepartment_NoExistingSprints_ShouldReturnZero() {
        // Act
        int created = sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        assertEquals(0, created);
        assertEquals(0, sprintRepository.findByDepartmentId(testDepartment.getId()).size());
    }

    @Test
    @DisplayName("createSprintsForDepartment - İlk sprint oluşturulduktan sonra otomatik sprint'ler oluşturulmalı")
    void createSprintsForDepartment_AfterFirstSprint_ShouldCreateAutomaticSprints() {
        // Arrange - İlk sprint'i manuel oluştur (MANAGER'ın yaptığı gibi)
        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 1 - IT Department - 2024");
        firstSprint.setStartDate(LocalDate.of(2024, 1, 1));
        firstSprint.setEndDate(LocalDate.of(2024, 1, 31));
        firstSprint.setDurationWeeks(4);
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act - Otomatik planlama çalıştır
        int created = sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        assertTrue(created > 0, "En az bir sprint oluşturulmalı");
        
        List<Sprint> allSprints = sprintRepository.findByDepartmentId(testDepartment.getId());
        assertTrue(allSprints.size() > 1, "İlk sprint + otomatik sprint'ler olmalı");
        
        // İkinci sprint'in adı "Sprint 2" olmalı
        Sprint secondSprint = allSprints.stream()
                .filter(s -> s.getName().contains("Sprint 2"))
                .findFirst()
                .orElse(null);
        assertNotNull(secondSprint, "İkinci sprint oluşturulmalı");
        assertEquals(4, secondSprint.getDurationWeeks(), "durationWeeks miras alınmalı");
    }

    @Test
    @DisplayName("createSprintsForDepartment - Tarih hesaplama doğru olmalı")
    void createSprintsForDepartment_ShouldCalculateDatesCorrectly() {
        // Arrange - İlk sprint: 1 Ocak - 31 Ocak
        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 1 - IT Department - 2024");
        firstSprint.setStartDate(LocalDate.of(2024, 1, 1));
        firstSprint.setEndDate(LocalDate.of(2024, 1, 31));
        firstSprint.setDurationWeeks(4);
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act
        sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        List<Sprint> allSprints = sprintRepository.findByDepartmentId(testDepartment.getId());
        assertTrue(allSprints.size() > 1);
        
        // İkinci sprint'in tarihleri kontrol et
        Sprint secondSprint = allSprints.stream()
                .filter(s -> s.getName().contains("Sprint 2"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(secondSprint);
        // İkinci sprint başlangıcı = ilk sprint bitişi + 1 gün
        assertEquals(LocalDate.of(2024, 2, 1), secondSprint.getStartDate());
        // İkinci sprint bitişi = başlangıç + 4 hafta - 1 gün = 28 Şubat
        assertEquals(LocalDate.of(2024, 2, 28), secondSprint.getEndDate());
    }

    @Test
    @DisplayName("createSprintsForDepartment - Sprint numaraları sıralı olmalı")
    void createSprintsForDepartment_ShouldHaveSequentialNumbers() {
        // Arrange - İlk sprint: Sprint 3 (örnek olarak)
        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 3 - IT Department - 2024");
        firstSprint.setStartDate(LocalDate.of(2024, 1, 1));
        firstSprint.setEndDate(LocalDate.of(2024, 1, 31));
        firstSprint.setDurationWeeks(4);
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act
        sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        List<Sprint> allSprints = sprintRepository.findByDepartmentId(testDepartment.getId());
        assertTrue(allSprints.size() > 1);
        
        // İlk otomatik sprint "Sprint 4" olmalı
        Sprint fourthSprint = allSprints.stream()
                .filter(s -> s.getName().contains("Sprint 4"))
                .findFirst()
                .orElse(null);
        assertNotNull(fourthSprint, "Sprint 4 oluşturulmalı");
        
        // İkinci otomatik sprint "Sprint 5" olmalı
        Sprint fifthSprint = allSprints.stream()
                .filter(s -> s.getName().contains("Sprint 5"))
                .findFirst()
                .orElse(null);
        assertNotNull(fifthSprint, "Sprint 5 oluşturulmalı");
    }

    @Test
    @DisplayName("createSprintsForDepartment - durationWeeks miras alınmalı")
    void createSprintsForDepartment_ShouldInheritDurationWeeks() {
        // Arrange - İlk sprint 3 haftalık
        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 1 - IT Department - 2024");
        firstSprint.setStartDate(LocalDate.of(2024, 1, 1));
        firstSprint.setEndDate(LocalDate.of(2024, 1, 21)); // 3 hafta
        firstSprint.setDurationWeeks(3);
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act
        sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        List<Sprint> allSprints = sprintRepository.findByDepartmentId(testDepartment.getId());
        Sprint secondSprint = allSprints.stream()
                .filter(s -> s.getName().contains("Sprint 2"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(secondSprint);
        assertEquals(3, secondSprint.getDurationWeeks(), "3 hafta miras alınmalı");
        
        // Tarih kontrolü: 22 Ocak + 3 hafta - 1 gün = 11 Şubat
        assertEquals(LocalDate.of(2024, 1, 22), secondSprint.getStartDate());
        assertEquals(LocalDate.of(2024, 2, 11), secondSprint.getEndDate());
    }

    @Test
    @DisplayName("createSprintsForDepartment - Birden fazla sprint oluşturulduğunda tarihler kesintisiz olmalı")
    void createSprintsForDepartment_MultipleSprints_ShouldHaveContinuousDates() {
        // Arrange
        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 1 - IT Department - 2024");
        firstSprint.setStartDate(LocalDate.of(2024, 1, 1));
        firstSprint.setEndDate(LocalDate.of(2024, 1, 31));
        firstSprint.setDurationWeeks(4);
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act
        sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        List<Sprint> allSprints = sprintRepository.findByDepartmentId(testDepartment.getId());
        assertTrue(allSprints.size() > 2);
        
        // Sprint'leri tarihe göre sırala
        allSprints.sort((s1, s2) -> s1.getStartDate().compareTo(s2.getStartDate()));
        
        // Her sprint'in bitişi + 1 gün = bir sonraki sprint'in başlangıcı olmalı
        for (int i = 0; i < allSprints.size() - 1; i++) {
            Sprint current = allSprints.get(i);
            Sprint next = allSprints.get(i + 1);
            
            LocalDate expectedNextStart = current.getEndDate().plusDays(1);
            assertEquals(expectedNextStart, next.getStartDate(), 
                    String.format("Sprint %s bitişi + 1 gün = Sprint %s başlangıcı olmalı", 
                            current.getName(), next.getName()));
        }
    }

    @Test
    @DisplayName("createUpcomingSprints - Tüm departmanlar için sprint oluşturmalı")
    void createUpcomingSprints_ShouldCreateForAllDepartments() {
        // Arrange - İki departman oluştur (setUp'daki departmanı kullan + yeni bir tane)
        Department dept1 = testDepartment; // setUp'daki departmanı kullan

        Department dept2 = new Department();
        dept2.setName("HR Department");
        dept2.setIsActive(true);
        dept2 = departmentRepository.save(dept2);

        // Her departman için ilk sprint oluştur
        Sprint sprint1 = new Sprint();
        sprint1.setName("Sprint 1 - IT Department - 2024");
        sprint1.setStartDate(LocalDate.of(2024, 1, 1));
        sprint1.setEndDate(LocalDate.of(2024, 1, 31));
        sprint1.setDurationWeeks(4);
        sprint1.setDepartment(dept1);
        sprintRepository.save(sprint1);

        Sprint sprint2 = new Sprint();
        sprint2.setName("Sprint 1 - HR Department - 2024");
        sprint2.setStartDate(LocalDate.of(2024, 1, 1));
        sprint2.setEndDate(LocalDate.of(2024, 1, 31));
        sprint2.setDurationWeeks(4);
        sprint2.setDepartment(dept2);
        sprintRepository.save(sprint2);

        // Act
        sprintPlanningService.createUpcomingSprints();

        // Assert
        List<Sprint> dept1Sprints = sprintRepository.findByDepartmentId(dept1.getId());
        List<Sprint> dept2Sprints = sprintRepository.findByDepartmentId(dept2.getId());
        
        assertTrue(dept1Sprints.size() > 1, "IT Department için otomatik sprint'ler oluşturulmalı");
        assertTrue(dept2Sprints.size() > 1, "HR Department için otomatik sprint'ler oluşturulmalı");
    }

    @Test
    @DisplayName("createSprintsForDepartment - durationWeeks null ise sprint oluşturmamalı")
    void createSprintsForDepartment_NullDurationWeeks_ShouldReturnZero() {
        // Arrange - durationWeeks olmayan sprint
        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 1 - IT Department - 2024");
        firstSprint.setStartDate(LocalDate.of(2024, 1, 1));
        firstSprint.setEndDate(LocalDate.of(2024, 1, 31));
        firstSprint.setDurationWeeks(null); // null
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act
        int created = sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        assertEquals(0, created, "durationWeeks null ise sprint oluşturulmamalı");
        List<Sprint> allSprints = sprintRepository.findByDepartmentId(testDepartment.getId());
        assertEquals(1, allSprints.size(), "Sadece ilk sprint olmalı");
    }

    @Test
    @DisplayName("createSprintsForDepartment - Aktif olmayan departman için sprint oluşturmamalı")
    void createSprintsForDepartment_InactiveDepartment_ShouldReturnZero() {
        // Arrange
        testDepartment.setIsActive(false);
        testDepartment = departmentRepository.save(testDepartment);

        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 1 - IT Department - 2024");
        firstSprint.setStartDate(LocalDate.of(2024, 1, 1));
        firstSprint.setEndDate(LocalDate.of(2024, 1, 31));
        firstSprint.setDurationWeeks(4);
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act
        int created = sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        assertEquals(0, created, "Aktif olmayan departman için sprint oluşturulmamalı");
    }

    @Test
    @DisplayName("createSprintsForDepartment - 6 ay ileriye kadar sprint oluşturmalı")
    void createSprintsForDepartment_ShouldCreateSprintsForSixMonths() {
        // Arrange - Bugünden 5 ay önce başlayan bir sprint
        LocalDate pastDate = LocalDate.now().minusMonths(5);
        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 1 - IT Department - 2024");
        firstSprint.setStartDate(pastDate);
        firstSprint.setEndDate(pastDate.plusWeeks(4).minusDays(1));
        firstSprint.setDurationWeeks(4);
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act
        int created = sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        assertTrue(created > 0, "En az bir sprint oluşturulmalı");
        
        List<Sprint> allSprints = sprintRepository.findByDepartmentId(testDepartment.getId());
        
        // En son sprint'in tarihi bugünden en az 6 ay sonrasına kadar olmalı
        Sprint lastSprint = allSprints.stream()
                .max((s1, s2) -> s1.getEndDate().compareTo(s2.getEndDate()))
                .orElse(null);
        
        assertNotNull(lastSprint);
        LocalDate sixMonthsLater = LocalDate.now().plusMonths(6);
        assertTrue(lastSprint.getEndDate().isAfter(sixMonthsLater.minusDays(30)) || 
                   lastSprint.getEndDate().isEqual(sixMonthsLater),
                "En son sprint bugünden en az 6 ay sonrasına kadar olmalı");
    }

    @Test
    @DisplayName("createSprintsForDepartment - Sprint adı formatı doğru olmalı")
    void createSprintsForDepartment_ShouldHaveCorrectNameFormat() {
        // Arrange
        Sprint firstSprint = new Sprint();
        firstSprint.setName("Sprint 5 - IT Department - 2024");
        firstSprint.setStartDate(LocalDate.of(2024, 1, 1));
        firstSprint.setEndDate(LocalDate.of(2024, 1, 31));
        firstSprint.setDurationWeeks(4);
        firstSprint.setDepartment(testDepartment);
        sprintRepository.save(firstSprint);

        // Act
        sprintPlanningService.createSprintsForDepartment(testDepartment.getId());

        // Assert
        List<Sprint> allSprints = sprintRepository.findByDepartmentId(testDepartment.getId());
        Sprint secondSprint = allSprints.stream()
                .filter(s -> s.getName().contains("Sprint 6"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(secondSprint);
        // Format: "Sprint {numara} - {departman} - {yıl}"
        assertTrue(secondSprint.getName().matches("Sprint \\d+ - IT Department - \\d{4}"),
                "Sprint adı formatı doğru olmalı: 'Sprint 6 - IT Department - 2024'");
    }
}

