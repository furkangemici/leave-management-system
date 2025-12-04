package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.RoleRepository;
import com.cozumtr.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String HR_EMAIL = "test@sirket.com";
    private static final String EMPLOYEE_EMAIL = "employee@sirket.com";
    private static final String PASSWORD = "Password123!";

    private Long testDepartmentId;
    private Long employeeRoleId;

    @BeforeEach
    void setUp() {
        // Her testten önce sadece bu sınıfta kullandığımız tabloları temizleyelim
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        roleRepository.deleteAll();
        departmentRepository.deleteAll();

        // Basit bir departman ve HR rolü oluşturalım
        Department department = new Department();
        department.setName("Test Department");
        department.setIsActive(true);
        department = departmentRepository.save(department);
        this.testDepartmentId = department.getId();

        Role hrRole = new Role();
        hrRole.setRoleName("HR");
        hrRole.setIsActive(true);
        hrRole = roleRepository.save(hrRole);

        Role employeeRole = new Role();
        employeeRole.setRoleName("EMPLOYEE");
        employeeRole.setIsActive(true);
        employeeRole = roleRepository.save(employeeRole);
        this.employeeRoleId = employeeRole.getId();

        // Test kullanıcısı oluştur (Employee, User ile birlikte cascade ile kaydedilecek)
        Employee employee = new Employee();
        employee.setFirstName("Test");
        employee.setLastName("User");
        employee.setEmail(HR_EMAIL);
        employee.setJobTitle("Testçi");
        employee.setBirthDate(LocalDate.now().minusYears(25));
        employee.setHireDate(LocalDate.now());
        employee.setDailyWorkHours(BigDecimal.valueOf(8));
        employee.setIsActive(true);
        employee.setDepartment(department);

        User user = new User();
        user.setEmployee(employee);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        Set<Role> roles = new HashSet<>();
        roles.add(hrRole);
        user.setRoles(roles);
        userRepository.save(user);

        // Sıradan çalışan kullanıcısı (EMPLOYEE rolü ile)
        Employee normalEmployee = new Employee();
        normalEmployee.setFirstName("Employee");
        normalEmployee.setLastName("User");
        normalEmployee.setEmail(EMPLOYEE_EMAIL);
        normalEmployee.setJobTitle("Çalışan");
        normalEmployee.setBirthDate(LocalDate.now().minusYears(25));
        normalEmployee.setHireDate(LocalDate.now());
        normalEmployee.setDailyWorkHours(BigDecimal.valueOf(8));
        normalEmployee.setIsActive(true);
        normalEmployee.setDepartment(department);

        User normalUser = new User();
        normalUser.setEmployee(normalEmployee);
        normalUser.setPasswordHash(passwordEncoder.encode(PASSWORD));
        normalUser.setIsActive(true);
        normalUser.setFailedLoginAttempts(0);
        Set<Role> normalRoles = new HashSet<>();
        normalRoles.add(employeeRole);
        normalUser.setRoles(normalRoles);
        userRepository.save(normalUser);
    }

    @Test
    @DisplayName("POST/api/auth/login - Başarılı giriş senaryosu")
    void login_Success_ShouldReturnToken() throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(HR_EMAIL, PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userEmail").value(HR_EMAIL));
    }

    @Test
    @DisplayName("POST/api/auth/login - Yanlış şifre ile giriş başarısız olmalı")
    void login_WrongPassword_ShouldReturnUnauthorized() throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "WrongPassword!"
                }
                """.formatted(HR_EMAIL);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/invite - EMPLOYEE rolü ile 403 Forbidden dönmeli")
    void invite_WithEmployeeRole_ShouldReturnForbidden() throws Exception {
        String token = loginAndGetToken(EMPLOYEE_EMAIL, PASSWORD);

        String body = "{}";

        mockMvc.perform(post("/api/auth/invite")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/invite - HR rolü ile 201 Created dönmeli")
    void invite_WithHrRole_ShouldReturnCreated() throws Exception {
        String token = loginAndGetToken(HR_EMAIL, PASSWORD);

        String body = """
                {
                  "firstName": "Yeni",
                  "lastName": "Calisan",
                  "email": "yeni.calisan@sirket.com",
                  "jobTitle": "Yazilim Gelistirici",
                  "departmentId": %d,
                  "roleId": %d,
                  "dailyWorkHours": 8.0
                }
                """.formatted(testDepartmentId, employeeRoleId);

        mockMvc.perform(post("/api/auth/invite")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("yeni.calisan@sirket.com"));
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String responseJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> map = objectMapper.readValue(responseJson, Map.class);
        return (String) map.get("token");
    }
}


