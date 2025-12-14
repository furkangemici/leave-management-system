package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("SprintController Integration Tests - Department-Based Sprint Management")
class SprintControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Department managerDepartment;
    private Department otherDepartment;
    private Employee managerEmployee;
    private User managerUser;
    private Employee otherManagerEmployee;
    private User otherManagerUser;
    private Sprint existingSprint;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        sprintRepository.deleteAll();
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        roleRepository.deleteAll();

        // Create Roles
        Role employeeRole = new Role();
        employeeRole.setRoleName("EMPLOYEE");
        employeeRole.setIsActive(true);
        employeeRole = roleRepository.save(employeeRole);

        Role managerRole = new Role();
        managerRole.setRoleName("MANAGER");
        managerRole.setIsActive(true);
        managerRole = roleRepository.save(managerRole);

        // Create Departments
        managerDepartment = new Department();
        managerDepartment.setName("IT Department");
        managerDepartment.setIsActive(true);
        managerDepartment = departmentRepository.save(managerDepartment);

        otherDepartment = new Department();
        otherDepartment.setName("HR Department");
        otherDepartment.setIsActive(true);
        otherDepartment = departmentRepository.save(otherDepartment);

        // Create Manager Employee (IT Department)
        managerEmployee = new Employee();
        managerEmployee.setFirstName("IT");
        managerEmployee.setLastName("Manager");
        managerEmployee.setEmail("it.manager@example.com");
        managerEmployee.setJobTitle("IT Manager");
        managerEmployee.setBirthDate(LocalDate.of(1980, 1, 1));
        managerEmployee.setHireDate(LocalDate.of(2020, 1, 1));
        managerEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        managerEmployee.setDepartment(managerDepartment);
        managerEmployee = employeeRepository.save(managerEmployee);

        // Create Manager User (IT Department)
        managerUser = new User();
        managerUser.setEmployee(managerEmployee);
        managerUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        managerUser.setIsActive(true);
        Set<Role> managerRoles = new HashSet<>();
        managerRoles.add(employeeRole);
        managerRoles.add(managerRole);
        managerUser.setRoles(managerRoles);
        managerUser = userRepository.save(managerUser);

        // Create Other Manager Employee (HR Department)
        otherManagerEmployee = new Employee();
        otherManagerEmployee.setFirstName("HR");
        otherManagerEmployee.setLastName("Manager");
        otherManagerEmployee.setEmail("hr.manager@example.com");
        otherManagerEmployee.setJobTitle("HR Manager");
        otherManagerEmployee.setBirthDate(LocalDate.of(1981, 1, 1));
        otherManagerEmployee.setHireDate(LocalDate.of(2020, 1, 1));
        otherManagerEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        otherManagerEmployee.setDepartment(otherDepartment);
        otherManagerEmployee = employeeRepository.save(otherManagerEmployee);

        // Create Other Manager User (HR Department)
        otherManagerUser = new User();
        otherManagerUser.setEmployee(otherManagerEmployee);
        otherManagerUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        otherManagerUser.setIsActive(true);
        Set<Role> otherManagerRoles = new HashSet<>();
        otherManagerRoles.add(employeeRole);
        otherManagerRoles.add(managerRole);
        otherManagerUser.setRoles(otherManagerRoles);
        otherManagerUser = userRepository.save(otherManagerUser);

        // Create existing sprint for IT Department
        existingSprint = new Sprint();
        existingSprint.setName("Sprint 1 - IT Department - 2024");
        existingSprint.setStartDate(LocalDate.of(2024, 1, 1));
        existingSprint.setEndDate(LocalDate.of(2024, 1, 31));
        existingSprint.setDurationWeeks(4);
        existingSprint.setDepartment(managerDepartment);
        existingSprint = sprintRepository.save(existingSprint);

        // Get token
        managerToken = loginAndGetToken("it.manager@example.com", "Password123!");
    }

    // ========== CREATE SPRINT TESTS ==========

    @Test
    @DisplayName("POST /api/sprints - MANAGER kendi departmanı için sprint oluşturabilmeli")
    void createSprint_ManagerOwnDepartment_ShouldCreateSprint() throws Exception {
        String requestBody = """
                {
                  "name": "Sprint 2 - IT Department - 2024",
                  "startDate": "2024-02-01",
                  "endDate": "2024-02-28",
                  "durationWeeks": 4
                }
                """;

        mockMvc.perform(post("/api/sprints")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sprint 2 - IT Department - 2024"))
                .andExpect(jsonPath("$.durationWeeks").value(4))
                .andExpect(jsonPath("$.departmentId").value(managerDepartment.getId().intValue()))
                .andExpect(jsonPath("$.startDate").value("2024-02-01"))
                .andExpect(jsonPath("$.endDate").value("2024-02-28"));
    }

    @Test
    @DisplayName("POST /api/sprints - Bitiş tarihi başlangıçtan önce olamaz")
    void createSprint_EndDateBeforeStartDate_ShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "Sprint 2",
                  "startDate": "2024-02-01",
                  "endDate": "2024-01-01",
                  "durationWeeks": 4
                }
                """;

        mockMvc.perform(post("/api/sprints")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/sprints - Aynı departman içinde aynı isimde sprint olamaz")
    void createSprint_DuplicateName_ShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "Sprint 1 - IT Department - 2024",
                  "startDate": "2024-02-01",
                  "endDate": "2024-02-28",
                  "durationWeeks": 4
                }
                """;

        mockMvc.perform(post("/api/sprints")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/sprints - Sadece MANAGER rolü erişebilmeli")
    void createSprint_WithoutManagerRole_ShouldReturnForbidden() throws Exception {
        String requestBody = """
                {
                  "name": "Sprint 2",
                  "startDate": "2024-02-01",
                  "endDate": "2024-02-28",
                  "durationWeeks": 4
                }
                """;

        mockMvc.perform(post("/api/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    // ========== GET ALL SPRINTS TESTS ==========

    @Test
    @DisplayName("GET /api/sprints - MANAGER sadece kendi departmanına ait sprint'leri görmeli")
    void getAllSprints_ShouldReturnOnlyManagerDepartmentSprints() throws Exception {
        // HR Department için sprint oluştur
        Sprint hrSprint = new Sprint();
        hrSprint.setName("Sprint 1 - HR Department - 2024");
        hrSprint.setStartDate(LocalDate.of(2024, 1, 1));
        hrSprint.setEndDate(LocalDate.of(2024, 1, 31));
        hrSprint.setDurationWeeks(4);
        hrSprint.setDepartment(otherDepartment);
        sprintRepository.save(hrSprint);

        mockMvc.perform(get("/api/sprints")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[*].departmentId", everyItem(is(managerDepartment.getId().intValue()))))
                .andExpect(jsonPath("$[*].departmentId", not(hasItem(otherDepartment.getId().intValue()))));
    }

    // ========== GET SPRINT BY ID TESTS ==========

    @Test
    @DisplayName("GET /api/sprints/{id} - MANAGER kendi departmanına ait sprint'i görebilmeli")
    void getSprintById_ManagerOwnDepartment_ShouldReturnSprint() throws Exception {
        mockMvc.perform(get("/api/sprints/" + existingSprint.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingSprint.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Sprint 1 - IT Department - 2024"))
                .andExpect(jsonPath("$.departmentId").value(managerDepartment.getId().intValue()));
    }

    @Test
    @DisplayName("GET /api/sprints/{id} - MANAGER başka departmana ait sprint'i görememeli")
    void getSprintById_OtherDepartment_ShouldReturnForbidden() throws Exception {
        // HR Department için sprint oluştur
        Sprint hrSprint = new Sprint();
        hrSprint.setName("Sprint 1 - HR Department - 2024");
        hrSprint.setStartDate(LocalDate.of(2024, 1, 1));
        hrSprint.setEndDate(LocalDate.of(2024, 1, 31));
        hrSprint.setDurationWeeks(4);
        hrSprint.setDepartment(otherDepartment);
        hrSprint = sprintRepository.save(hrSprint);

        mockMvc.perform(get("/api/sprints/" + hrSprint.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/sprints/{id} - Sprint bulunamazsa hata dönmeli")
    void getSprintById_SprintNotFound_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/sprints/99999")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());
    }

    // ========== UPDATE SPRINT TESTS ==========

    @Test
    @DisplayName("PUT /api/sprints/{id} - MANAGER kendi departmanına ait sprint'i güncelleyebilmeli")
    void updateSprint_ManagerOwnDepartment_ShouldUpdateSprint() throws Exception {
        String requestBody = """
                {
                  "name": "Sprint 1 - Updated",
                  "startDate": "2024-01-01",
                  "endDate": "2024-01-31",
                  "durationWeeks": 3
                }
                """;

        mockMvc.perform(put("/api/sprints/" + existingSprint.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sprint 1 - Updated"))
                .andExpect(jsonPath("$.durationWeeks").value(3));
    }

    @Test
    @DisplayName("PUT /api/sprints/{id} - MANAGER başka departmana ait sprint'i güncelleyememeli")
    void updateSprint_OtherDepartment_ShouldReturnForbidden() throws Exception {
        // HR Department için sprint oluştur
        Sprint hrSprint = new Sprint();
        hrSprint.setName("Sprint 1 - HR Department - 2024");
        hrSprint.setStartDate(LocalDate.of(2024, 1, 1));
        hrSprint.setEndDate(LocalDate.of(2024, 1, 31));
        hrSprint.setDurationWeeks(4);
        hrSprint.setDepartment(otherDepartment);
        hrSprint = sprintRepository.save(hrSprint);

        String requestBody = """
                {
                  "name": "Sprint 1 - Updated",
                  "startDate": "2024-01-01",
                  "endDate": "2024-01-31",
                  "durationWeeks": 3
                }
                """;

        mockMvc.perform(put("/api/sprints/" + hrSprint.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE SPRINT TESTS ==========

    @Test
    @DisplayName("DELETE /api/sprints/{id} - MANAGER kendi departmanına ait sprint'i silebilmeli")
    void deleteSprint_ManagerOwnDepartment_ShouldDeleteSprint() throws Exception {
        mockMvc.perform(delete("/api/sprints/" + existingSprint.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        // Sprint'in silindiğini doğrula
        assertFalse(sprintRepository.findById(existingSprint.getId()).isPresent());
    }

    @Test
    @DisplayName("DELETE /api/sprints/{id} - MANAGER başka departmana ait sprint'i silememeli")
    void deleteSprint_OtherDepartment_ShouldReturnForbidden() throws Exception {
        // HR Department için sprint oluştur
        Sprint hrSprint = new Sprint();
        hrSprint.setName("Sprint 1 - HR Department - 2024");
        hrSprint.setStartDate(LocalDate.of(2024, 1, 1));
        hrSprint.setEndDate(LocalDate.of(2024, 1, 31));
        hrSprint.setDurationWeeks(4);
        hrSprint.setDepartment(otherDepartment);
        hrSprint = sprintRepository.save(hrSprint);

        mockMvc.perform(delete("/api/sprints/" + hrSprint.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest());

        // Sprint'in silinmediğini doğrula
        assertTrue(sprintRepository.findById(hrSprint.getId()).isPresent());
    }

    // ========== AUTHORIZATION TESTS ==========

    @Test
    @DisplayName("POST /api/sprints - Yetkisiz kullanıcı erişememeli")
    void createSprint_WithoutAuth_ShouldReturnForbidden() throws Exception {
        String requestBody = """
                {
                  "name": "Sprint 2",
                  "startDate": "2024-02-01",
                  "endDate": "2024-02-28",
                  "durationWeeks": 4
                }
                """;

        mockMvc.perform(post("/api/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/sprints - Yetkisiz kullanıcı erişememeli")
    void getAllSprints_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/sprints"))
                .andExpect(status().isForbidden());
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @DisplayName("POST /api/sprints - Zorunlu alanlar eksikse validation hatası dönmeli")
    void createSprint_MissingRequiredFields_ShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "Sprint 2"
                }
                """;

        mockMvc.perform(post("/api/sprints")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/sprints - durationWeeks pozitif olmalı")
    void createSprint_InvalidDurationWeeks_ShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "Sprint 2",
                  "startDate": "2024-02-01",
                  "endDate": "2024-02-28",
                  "durationWeeks": -1
                }
                """;

        mockMvc.perform(post("/api/sprints")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== HELPER METODLAR ==========

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = String.format("""
                {
                  "email": "%s",
                  "password": "%s"
                }
                """, email, password);

        String responseJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(responseJson, Map.class);
        return (String) map.get("token");
    }
}


