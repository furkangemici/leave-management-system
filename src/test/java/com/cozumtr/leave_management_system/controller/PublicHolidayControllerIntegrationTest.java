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
@DisplayName("PublicHolidayController Integration Tests - HR Role Required")
class PublicHolidayControllerIntegrationTest {

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
    private PublicHolidayRepository publicHolidayRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Department testDepartment;
    private Employee hrEmployee;
    private User hrUser;
    private Role hrRole;
    private Role employeeRole;
    private String hrToken;
    private String employeeToken;
    private LocalDate futureDate;
    private LocalDate pastDate;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        publicHolidayRepository.deleteAll();
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        roleRepository.deleteAll();

        futureDate = LocalDate.now().plusDays(30);
        pastDate = LocalDate.now().minusDays(10);

        // Create Roles
        employeeRole = new Role();
        employeeRole.setRoleName("EMPLOYEE");
        employeeRole.setIsActive(true);
        employeeRole = roleRepository.save(employeeRole);

        hrRole = new Role();
        hrRole.setRoleName("HR");
        hrRole.setIsActive(true);
        hrRole = roleRepository.save(hrRole);

        // Create Department
        testDepartment = new Department();
        testDepartment.setName("HR Department");
        testDepartment.setIsActive(true);
        testDepartment = departmentRepository.save(testDepartment);

        // Create HR Employee
        hrEmployee = new Employee();
        hrEmployee.setFirstName("HR");
        hrEmployee.setLastName("User");
        hrEmployee.setEmail("hr.user@example.com");
        hrEmployee.setJobTitle("HR Manager");
        hrEmployee.setBirthDate(LocalDate.of(1980, 1, 1));
        hrEmployee.setHireDate(LocalDate.of(2020, 1, 1));
        hrEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        hrEmployee.setDepartment(testDepartment);
        hrEmployee = employeeRepository.save(hrEmployee);

        // Create HR User
        hrUser = new User();
        hrUser.setEmployee(hrEmployee);
        hrUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        hrUser.setIsActive(true);
        Set<Role> hrRoles = new HashSet<>();
        hrRoles.add(employeeRole);
        hrRoles.add(hrRole);
        hrUser.setRoles(hrRoles);
        hrUser = userRepository.save(hrUser);

        // Create Employee User (for unauthorized tests)
        Employee employee = new Employee();
        employee.setFirstName("Employee");
        employee.setLastName("User");
        employee.setEmail("employee@example.com");
        employee.setJobTitle("Developer");
        employee.setBirthDate(LocalDate.of(1990, 1, 1));
        employee.setHireDate(LocalDate.of(2021, 1, 1));
        employee.setDailyWorkHours(new BigDecimal("8.0"));
        employee.setDepartment(testDepartment);
        employee = employeeRepository.save(employee);

        User employeeUser = new User();
        employeeUser.setEmployee(employee);
        employeeUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        employeeUser.setIsActive(true);
        Set<Role> empRoles = new HashSet<>();
        empRoles.add(employeeRole);
        employeeUser.setRoles(empRoles);
        userRepository.save(employeeUser);

        // Get tokens
        hrToken = loginAndGetToken("hr.user@example.com", "Password123!");
        employeeToken = loginAndGetToken("employee@example.com", "Password123!");
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("POST /api/metadata/public-holidays - HR rolü ile başarılı oluşturma")
    void createPublicHoliday_WithHrRole_ShouldCreate() throws Exception {
        String requestBody = String.format("""
                {
                  "date": "%s",
                  "name": "Yeni Yıl",
                  "isHalfDay": false
                }
                """, futureDate);

        mockMvc.perform(post("/api/metadata/public-holidays")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.date").value(futureDate.toString()))
                .andExpect(jsonPath("$.name").value("Yeni Yıl"))
                .andExpect(jsonPath("$.isHalfDay").value(false));
    }

    @Test
    @DisplayName("POST /api/metadata/public-holidays - HR rolü olmadan erişim engellenmeli")
    void createPublicHoliday_WithoutHrRole_ShouldReturnForbidden() throws Exception {
        String requestBody = String.format("""
                {
                  "date": "%s",
                  "name": "Yeni Yıl",
                  "isHalfDay": false
                }
                """, futureDate);

        mockMvc.perform(post("/api/metadata/public-holidays")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/metadata/public-holidays - Geçmiş tarih kontrolü")
    void createPublicHoliday_PastDate_ShouldReturnBadRequest() throws Exception {
        String requestBody = String.format("""
                {
                  "date": "%s",
                  "name": "Geçmiş Tatil",
                  "isHalfDay": false
                }
                """, pastDate);

        mockMvc.perform(post("/api/metadata/public-holidays")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/metadata/public-holidays - Bugünün tarihi kabul edilmeli")
    void createPublicHoliday_TodayDate_ShouldCreate() throws Exception {
        LocalDate today = LocalDate.now();
        String requestBody = String.format("""
                {
                  "date": "%s",
                  "name": "Bugünün Tatili",
                  "isHalfDay": false
                }
                """, today);

        mockMvc.perform(post("/api/metadata/public-holidays")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value(today.toString()));
    }

    @Test
    @DisplayName("POST /api/metadata/public-holidays - Duplicate date kontrolü")
    void createPublicHoliday_DuplicateDate_ShouldReturnBadRequest() throws Exception {
        // Önce bir public holiday oluştur
        PublicHoliday existing = new PublicHoliday();
        existing.setStartDate(futureDate);
        existing.setEndDate(futureDate);
        existing.setYear(futureDate.getYear());
        existing.setName("Mevcut Tatil");
        existing.setIsHalfDay(false);
        existing.setIsActive(true);
        publicHolidayRepository.save(existing);

        String requestBody = String.format("""
                {
                  "date": "%s",
                  "name": "Yeni Tatil",
                  "isHalfDay": false
                }
                """, futureDate);

        mockMvc.perform(post("/api/metadata/public-holidays")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("GET /api/metadata/public-holidays - HR rolü ile tüm resmi tatilleri getirir")
    void getAllPublicHolidays_WithHrRole_ShouldReturnAll() throws Exception {
        PublicHoliday holiday1 = new PublicHoliday();
        holiday1.setStartDate(futureDate);
        holiday1.setEndDate(futureDate);
        holiday1.setYear(futureDate.getYear());
        holiday1.setName("Yeni Yıl");
        holiday1.setIsHalfDay(false);
        holiday1.setIsActive(true);
        publicHolidayRepository.save(holiday1);

        PublicHoliday holiday2 = new PublicHoliday();
        holiday2.setStartDate(futureDate.plusDays(10));
        holiday2.setEndDate(futureDate.plusDays(10));
        holiday2.setYear(futureDate.plusDays(10).getYear());
        holiday2.setName("Cumhuriyet Bayramı");
        holiday2.setIsHalfDay(false);
        holiday2.setIsActive(true);
        publicHolidayRepository.save(holiday2);

        mockMvc.perform(get("/api/metadata/public-holidays")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Yeni Yıl", "Cumhuriyet Bayramı")));
    }

    @Test
    @DisplayName("GET /api/metadata/public-holidays/{id} - HR rolü ile ID'ye göre getirir")
    void getPublicHolidayById_WithHrRole_ShouldReturn() throws Exception {
        PublicHoliday holiday = new PublicHoliday();
        holiday.setStartDate(futureDate);
        holiday.setEndDate(futureDate);
        holiday.setYear(futureDate.getYear());
        holiday.setName("Yeni Yıl");
        holiday.setIsHalfDay(false);
        holiday.setIsActive(true);
        holiday = publicHolidayRepository.save(holiday);

        mockMvc.perform(get("/api/metadata/public-holidays/" + holiday.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(holiday.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Yeni Yıl"))
                .andExpect(jsonPath("$.startDate").value(futureDate.toString()));
    }

    @Test
    @DisplayName("GET /api/metadata/public-holidays/{id} - Bulunamayan ID için 400 Bad Request")
    void getPublicHolidayById_NotFound_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/metadata/public-holidays/999")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("PUT /api/metadata/public-holidays/{id} - HR rolü ile başarılı güncelleme")
    void updatePublicHoliday_WithHrRole_ShouldUpdate() throws Exception {
        PublicHoliday holiday = new PublicHoliday();
        holiday.setStartDate(futureDate);
        holiday.setEndDate(futureDate);
        holiday.setYear(futureDate.getYear());
        holiday.setName("Yeni Yıl");
        holiday.setIsHalfDay(false);
        holiday.setIsActive(true);
        holiday = publicHolidayRepository.save(holiday);

        LocalDate newDate = futureDate.plusDays(5);
        String requestBody = String.format("""
                {
                  "date": "%s",
                  "name": "Güncellenmiş Tatil",
                  "isHalfDay": true
                }
                """, newDate);

        mockMvc.perform(put("/api/metadata/public-holidays/" + holiday.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Güncellenmiş Tatil"))
                .andExpect(jsonPath("$.startDate").value(newDate.toString()))
                .andExpect(jsonPath("$.isHalfDay").value(true));
    }

    @Test
    @DisplayName("PUT /api/metadata/public-holidays/{id} - Duplicate date kontrolü")
    void updatePublicHoliday_DuplicateDate_ShouldReturnBadRequest() throws Exception {
        PublicHoliday holiday1 = new PublicHoliday();
        holiday1.setStartDate(futureDate);
        holiday1.setEndDate(futureDate);
        holiday1.setYear(futureDate.getYear());
        holiday1.setName("Tatil 1");
        holiday1.setIsHalfDay(false);
        holiday1.setIsActive(true);
        holiday1 = publicHolidayRepository.save(holiday1);

        PublicHoliday holiday2 = new PublicHoliday();
        holiday2.setStartDate(futureDate.plusDays(10));
        holiday2.setEndDate(futureDate.plusDays(10));
        holiday2.setYear(futureDate.plusDays(10).getYear());
        holiday2.setName("Tatil 2");
        holiday2.setIsHalfDay(false);
        holiday2.setIsActive(true);
        holiday2 = publicHolidayRepository.save(holiday2);

        String requestBody = String.format("""
                {
                  "date": "%s",
                  "name": "Tatil 1",
                  "isHalfDay": false
                }
                """, holiday2.getStartDate());

        mockMvc.perform(put("/api/metadata/public-holidays/" + holiday1.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/metadata/public-holidays/{id} - Bulunamayan ID için 400 Bad Request")
    void updatePublicHoliday_NotFound_ShouldReturnBadRequest() throws Exception {
        String requestBody = String.format("""
                {
                  "date": "%s",
                  "name": "Güncellenmiş Tatil",
                  "isHalfDay": false
                }
                """, futureDate);

        mockMvc.perform(put("/api/metadata/public-holidays/999")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("DELETE /api/metadata/public-holidays/{id} - HR rolü ile başarılı silme")
    void deletePublicHoliday_WithHrRole_ShouldDelete() throws Exception {
        PublicHoliday holiday = new PublicHoliday();
        holiday.setStartDate(futureDate);
        holiday.setEndDate(futureDate);
        holiday.setYear(futureDate.getYear());
        holiday.setName("Yeni Yıl");
        holiday.setIsHalfDay(false);
        holiday.setIsActive(true);
        holiday = publicHolidayRepository.save(holiday);

        mockMvc.perform(delete("/api/metadata/public-holidays/" + holiday.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNoContent());

        // Soft delete kontrolü
        PublicHoliday deleted = publicHolidayRepository.findById(holiday.getId()).orElse(null);
        assertNotNull(deleted);
        assertFalse(deleted.getIsActive());
    }

    @Test
    @DisplayName("DELETE /api/metadata/public-holidays/{id} - Bulunamayan ID için 400 Bad Request")
    void deletePublicHoliday_NotFound_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/metadata/public-holidays/999")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());
    }

    // ========== HELPER METHODS ==========

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


