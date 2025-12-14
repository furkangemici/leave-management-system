package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.enums.RequestUnit;
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
@DisplayName("LeaveTypeController Integration Tests - HR Role Required")
class LeaveTypeControllerIntegrationTest {

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
    private LeaveTypeRepository leaveTypeRepository;

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

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        leaveTypeRepository.deleteAll();
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        roleRepository.deleteAll();

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
    @DisplayName("POST /api/metadata/leave-types - HR rolü ile başarılı oluşturma")
    void createLeaveType_WithHrRole_ShouldCreate() throws Exception {
        String requestBody = """
                {
                  "name": "Yıllık İzin",
                  "isPaid": true,
                  "deductsFromAnnual": true,
                  "workflowDefinition": "ROLE_MANAGER,ROLE_HR",
                  "requestUnit": "DAY"
                }
                """;

        mockMvc.perform(post("/api/metadata/leave-types")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Yıllık İzin"))
                .andExpect(jsonPath("$.isPaid").value(true))
                .andExpect(jsonPath("$.deductsFromAnnual").value(true))
                .andExpect(jsonPath("$.requestUnit").value("DAY"));
    }

    @Test
    @DisplayName("POST /api/metadata/leave-types - HR rolü olmadan erişim engellenmeli")
    void createLeaveType_WithoutHrRole_ShouldReturnForbidden() throws Exception {
        String requestBody = """
                {
                  "name": "Yıllık İzin",
                  "isPaid": true,
                  "deductsFromAnnual": true,
                  "requestUnit": "DAY"
                }
                """;

        mockMvc.perform(post("/api/metadata/leave-types")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/metadata/leave-types - Token olmadan erişim engellenmeli")
    void createLeaveType_WithoutToken_ShouldReturnForbidden() throws Exception {
        String requestBody = """
                {
                  "name": "Yıllık İzin",
                  "isPaid": true,
                  "deductsFromAnnual": true,
                  "requestUnit": "DAY"
                }
                """;

        mockMvc.perform(post("/api/metadata/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/metadata/leave-types - Duplicate name kontrolü")
    void createLeaveType_DuplicateName_ShouldReturnBadRequest() throws Exception {
        // Önce bir leave type oluştur
        LeaveType existing = new LeaveType();
        existing.setName("Yıllık İzin");
        existing.setPaid(true);
        existing.setDeductsFromAnnual(true);
        existing.setRequestUnit(RequestUnit.DAY);
        existing.setIsActive(true);
        leaveTypeRepository.save(existing);

        String requestBody = """
                {
                  "name": "Yıllık İzin",
                  "isPaid": true,
                  "deductsFromAnnual": true,
                  "requestUnit": "DAY"
                }
                """;

        mockMvc.perform(post("/api/metadata/leave-types")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("GET /api/metadata/leave-types - HR rolü ile tüm izin türlerini getirir")
    void getAllLeaveTypes_WithHrRole_ShouldReturnAll() throws Exception {
        // Test verileri oluştur
        LeaveType leaveType1 = new LeaveType();
        leaveType1.setName("Yıllık İzin");
        leaveType1.setPaid(true);
        leaveType1.setDeductsFromAnnual(true);
        leaveType1.setRequestUnit(RequestUnit.DAY);
        leaveType1.setIsActive(true);
        leaveTypeRepository.save(leaveType1);

        LeaveType leaveType2 = new LeaveType();
        leaveType2.setName("Mazeret İzni");
        leaveType2.setPaid(false);
        leaveType2.setDeductsFromAnnual(false);
        leaveType2.setRequestUnit(RequestUnit.HOUR);
        leaveType2.setIsActive(true);
        leaveTypeRepository.save(leaveType2);

        mockMvc.perform(get("/api/metadata/leave-types")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Yıllık İzin", "Mazeret İzni")));
    }

    @Test
    @DisplayName("GET /api/metadata/leave-types/{id} - HR rolü ile ID'ye göre getirir")
    void getLeaveTypeById_WithHrRole_ShouldReturn() throws Exception {
        LeaveType leaveType = new LeaveType();
        leaveType.setName("Yıllık İzin");
        leaveType.setPaid(true);
        leaveType.setDeductsFromAnnual(true);
        leaveType.setRequestUnit(RequestUnit.DAY);
        leaveType.setIsActive(true);
        leaveType = leaveTypeRepository.save(leaveType);

        mockMvc.perform(get("/api/metadata/leave-types/" + leaveType.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leaveType.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Yıllık İzin"));
    }

    @Test
    @DisplayName("GET /api/metadata/leave-types/{id} - Bulunamayan ID için 400 Bad Request")
    void getLeaveTypeById_NotFound_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/metadata/leave-types/999")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("PUT /api/metadata/leave-types/{id} - HR rolü ile başarılı güncelleme")
    void updateLeaveType_WithHrRole_ShouldUpdate() throws Exception {
        LeaveType leaveType = new LeaveType();
        leaveType.setName("Yıllık İzin");
        leaveType.setPaid(true);
        leaveType.setDeductsFromAnnual(true);
        leaveType.setRequestUnit(RequestUnit.DAY);
        leaveType.setIsActive(true);
        leaveType = leaveTypeRepository.save(leaveType);

        String requestBody = """
                {
                  "name": "Güncellenmiş İzin",
                  "isPaid": false,
                  "deductsFromAnnual": false,
                  "workflowDefinition": "ROLE_MANAGER",
                  "requestUnit": "HOUR"
                }
                """;

        mockMvc.perform(put("/api/metadata/leave-types/" + leaveType.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Güncellenmiş İzin"))
                .andExpect(jsonPath("$.isPaid").value(false))
                .andExpect(jsonPath("$.requestUnit").value("HOUR"));
    }

    @Test
    @DisplayName("PUT /api/metadata/leave-types/{id} - Duplicate name kontrolü")
    void updateLeaveType_DuplicateName_ShouldReturnBadRequest() throws Exception {
        LeaveType leaveType1 = new LeaveType();
        leaveType1.setName("İzin 1");
        leaveType1.setPaid(true);
        leaveType1.setDeductsFromAnnual(true);
        leaveType1.setRequestUnit(RequestUnit.DAY);
        leaveType1.setIsActive(true);
        leaveType1 = leaveTypeRepository.save(leaveType1);

        LeaveType leaveType2 = new LeaveType();
        leaveType2.setName("İzin 2");
        leaveType2.setPaid(true);
        leaveType2.setDeductsFromAnnual(true);
        leaveType2.setRequestUnit(RequestUnit.DAY);
        leaveType2.setIsActive(true);
        leaveType2 = leaveTypeRepository.save(leaveType2);

        String requestBody = """
                {
                  "name": "İzin 2",
                  "isPaid": true,
                  "deductsFromAnnual": true,
                  "requestUnit": "DAY"
                }
                """;

        mockMvc.perform(put("/api/metadata/leave-types/" + leaveType1.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("DELETE /api/metadata/leave-types/{id} - HR rolü ile başarılı silme")
    void deleteLeaveType_WithHrRole_ShouldDelete() throws Exception {
        LeaveType leaveType = new LeaveType();
        leaveType.setName("Yıllık İzin");
        leaveType.setPaid(true);
        leaveType.setDeductsFromAnnual(true);
        leaveType.setRequestUnit(RequestUnit.DAY);
        leaveType.setIsActive(true);
        leaveType = leaveTypeRepository.save(leaveType);

        mockMvc.perform(delete("/api/metadata/leave-types/" + leaveType.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isNoContent());

        // Soft delete kontrolü
        LeaveType deleted = leaveTypeRepository.findById(leaveType.getId()).orElse(null);
        assertNotNull(deleted);
        assertFalse(deleted.getIsActive());
    }

    @Test
    @DisplayName("DELETE /api/metadata/leave-types/{id} - Bulunamayan ID için 400 Bad Request")
    void deleteLeaveType_NotFound_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/metadata/leave-types/999")
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


