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
@DisplayName("DepartmentController Integration Tests - HR Role Required")
class DepartmentControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Department testDepartment;
    private Employee hrEmployee;
    private Employee managerEmployee;
    private User hrUser;
    private Role hrRole;
    private Role employeeRole;
    private String hrToken;
    private String employeeToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        roleRepository.deleteAll();

        // Create Roles (idempotent)
        employeeRole = getOrCreateRole("EMPLOYEE");
        hrRole = getOrCreateRole("HR");

        // Create Department
        testDepartment = new Department();
        testDepartment.setName("HR Department");
        testDepartment.setIsActive(true);
        testDepartment = departmentRepository.save(testDepartment);

        // Create Manager Employee
        managerEmployee = new Employee();
        managerEmployee.setFirstName("Manager");
        managerEmployee.setLastName("User");
        managerEmployee.setEmail("manager@example.com");
        managerEmployee.setJobTitle("Manager");
        managerEmployee.setBirthDate(LocalDate.of(1980, 1, 1));
        managerEmployee.setHireDate(LocalDate.of(2020, 1, 1));
        managerEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        managerEmployee.setDepartment(testDepartment);
        managerEmployee.setIsActive(true);
        managerEmployee = employeeRepository.save(managerEmployee);

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
        employee.setIsActive(true);
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

    private Role getOrCreateRole(String roleName) {
        return roleRepository.findByRoleName(roleName).orElseGet(() -> {
            Role r = new Role();
            r.setRoleName(roleName);
            r.setIsActive(true);
            return roleRepository.save(r);
        });
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("POST /api/metadata/departments - HR rolü ile başarılı oluşturma (manager olmadan)")
    void createDepartment_WithHrRole_WithoutManager_ShouldCreate() throws Exception {
        String requestBody = """
                {
                  "name": "IT Department"
                }
                """;

        mockMvc.perform(post("/api/metadata/departments")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("IT Department"))
                .andExpect(jsonPath("$.managerId").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/metadata/departments - HR rolü ile başarılı oluşturma (manager ile)")
    void createDepartment_WithHrRole_WithManager_ShouldCreate() throws Exception {
        String requestBody = String.format("""
                {
                  "name": "IT Department",
                  "managerId": %d
                }
                """, managerEmployee.getId());

        mockMvc.perform(post("/api/metadata/departments")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("IT Department"))
                .andExpect(jsonPath("$.managerId").value(managerEmployee.getId().intValue()))
                .andExpect(jsonPath("$.managerName").value("Manager User"));
    }

    @Test
    @DisplayName("POST /api/metadata/departments - HR rolü olmadan erişim engellenmeli")
    void createDepartment_WithoutHrRole_ShouldReturnForbidden() throws Exception {
        String requestBody = """
                {
                  "name": "IT Department"
                }
                """;

        mockMvc.perform(post("/api/metadata/departments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/metadata/departments - Duplicate name kontrolü")
    void createDepartment_DuplicateName_ShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "HR Department"
                }
                """;

        mockMvc.perform(post("/api/metadata/departments")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/metadata/departments - Manager bulunamadı")
    void createDepartment_ManagerNotFound_ShouldReturnBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "IT Department",
                  "managerId": 999
                }
                """;

        mockMvc.perform(post("/api/metadata/departments")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("GET /api/metadata/departments - HR rolü ile tüm departmanları getirir")
    void getAllDepartments_WithHrRole_ShouldReturnAll() throws Exception {
        Department dept2 = new Department();
        dept2.setName("IT Department");
        dept2.setIsActive(true);
        departmentRepository.save(dept2);

        mockMvc.perform(get("/api/metadata/departments")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItem("HR Department")))
                .andExpect(jsonPath("$[*].name", hasItem("IT Department")));
    }

    @Test
    @DisplayName("GET /api/metadata/departments/{id} - HR rolü ile ID'ye göre getirir")
    void getDepartmentById_WithHrRole_ShouldReturn() throws Exception {
        mockMvc.perform(get("/api/metadata/departments/" + testDepartment.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testDepartment.getId().intValue()))
                .andExpect(jsonPath("$.name").value("HR Department"));
    }

    @Test
    @DisplayName("GET /api/metadata/departments/{id} - Bulunamayan ID için 400 Bad Request")
    void getDepartmentById_NotFound_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/metadata/departments/999")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("PUT /api/metadata/departments/{id} - HR rolü ile başarılı güncelleme")
    void updateDepartment_WithHrRole_ShouldUpdate() throws Exception {
        String requestBody = """
                {
                  "name": "Updated Department"
                }
                """;

        mockMvc.perform(put("/api/metadata/departments/" + testDepartment.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Department"));
    }

    @Test
    @DisplayName("PUT /api/metadata/departments/{id} - Manager güncelleme")
    void updateDepartment_WithManagerUpdate_ShouldUpdate() throws Exception {
        // Yeni bir manager oluştur
        Employee newManager = new Employee();
        newManager.setFirstName("New");
        newManager.setLastName("Manager");
        newManager.setEmail("new.manager@example.com");
        newManager.setJobTitle("New Manager");
        newManager.setBirthDate(LocalDate.of(1985, 1, 1));
        newManager.setHireDate(LocalDate.of(2021, 1, 1));
        newManager.setDailyWorkHours(new BigDecimal("8.0"));
        newManager.setDepartment(testDepartment);
        newManager.setIsActive(true);
        newManager = employeeRepository.save(newManager);

        String requestBody = String.format("""
                {
                  "name": "Updated Department",
                  "managerId": %d
                }
                """, newManager.getId());

        mockMvc.perform(put("/api/metadata/departments/" + testDepartment.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Department"))
                .andExpect(jsonPath("$.managerId").value(newManager.getId().intValue()))
                .andExpect(jsonPath("$.managerName").value("New Manager"));
    }

    @Test
    @DisplayName("PUT /api/metadata/departments/{id} - Duplicate name kontrolü")
    void updateDepartment_DuplicateName_ShouldReturnBadRequest() throws Exception {
        Department dept2 = new Department();
        dept2.setName("IT Department");
        dept2.setIsActive(true);
        dept2 = departmentRepository.save(dept2);

        String requestBody = """
                {
                  "name": "IT Department"
                }
                """;

        mockMvc.perform(put("/api/metadata/departments/" + testDepartment.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("DELETE /api/metadata/departments/{id} - HR rolü ile başarılı silme (aktif çalışan yok)")
    void deleteDepartment_WithHrRole_NoActiveEmployees_ShouldDelete() throws Exception {
        Department deptToDelete = new Department();
        deptToDelete.setName("Department To Delete");
        deptToDelete.setIsActive(true);
        deptToDelete = departmentRepository.save(deptToDelete);

        mockMvc.perform(delete("/api/metadata/departments/" + deptToDelete.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNoContent());

        // Soft delete kontrolü
        Department deleted = departmentRepository.findById(deptToDelete.getId()).orElse(null);
        assertNotNull(deleted);
        assertFalse(deleted.getIsActive());
    }

    @Test
    @DisplayName("DELETE /api/metadata/departments/{id} - Aktif çalışanlar varsa silme engellenir")
    void deleteDepartment_WithActiveEmployees_ShouldReturnBadRequest() throws Exception {
        // testDepartment'a aktif çalışan zaten var (hrEmployee ve managerEmployee)

        mockMvc.perform(delete("/api/metadata/departments/" + testDepartment.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());

        // Departman hala aktif olmalı
        Department department = departmentRepository.findById(testDepartment.getId()).orElse(null);
        assertNotNull(department);
        assertTrue(department.getIsActive());
    }

    @Test
    @DisplayName("DELETE /api/metadata/departments/{id} - Bulunamayan ID için 400 Bad Request")
    void deleteDepartment_NotFound_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/metadata/departments/999")
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


