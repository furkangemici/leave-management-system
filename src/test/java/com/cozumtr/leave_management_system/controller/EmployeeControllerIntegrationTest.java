package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.RoleRepository;
import com.cozumtr.leave_management_system.repository.UserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerIntegrationTest {

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

    private static final String EMPLOYEE_EMAIL = "profile.employee@sirket.com";
    private static final String OTHER_EMPLOYEE_EMAIL = "other.employee@sirket.com";
    private static final String PASSWORD = "Password123!";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        roleRepository.deleteAll();
        departmentRepository.deleteAll();

        Department department = new Department();
        department.setName("Profil Departmanı");
        department.setIsActive(true);
        department = departmentRepository.save(department);

        Role employeeRole = new Role();
        employeeRole.setRoleName("EMPLOYEE");
        employeeRole.setIsActive(true);
        employeeRole = roleRepository.save(employeeRole);

        // 1. Kullanıcı (EMPLOYEE_EMAIL)
        Employee employee = new Employee();
        employee.setFirstName("Profil");
        employee.setLastName("Calisan");
        employee.setEmail(EMPLOYEE_EMAIL);
        employee.setJobTitle("Yazilimci");
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
        roles.add(employeeRole);
        user.setRoles(roles);
        userRepository.save(user);

        // 2. Kullanıcı (OTHER_EMPLOYEE_EMAIL)
        Employee otherEmployee = new Employee();
        otherEmployee.setFirstName("Diger");
        otherEmployee.setLastName("Calisan");
        otherEmployee.setEmail(OTHER_EMPLOYEE_EMAIL);
        otherEmployee.setJobTitle("Tester");
        otherEmployee.setBirthDate(LocalDate.now().minusYears(28));
        otherEmployee.setHireDate(LocalDate.now());
        otherEmployee.setDailyWorkHours(BigDecimal.valueOf(8));
        otherEmployee.setIsActive(true);
        otherEmployee.setDepartment(department);

        User otherUser = new User();
        otherUser.setEmployee(otherEmployee);
        otherUser.setPasswordHash(passwordEncoder.encode(PASSWORD));
        otherUser.setIsActive(true);
        otherUser.setFailedLoginAttempts(0);
        Set<Role> otherRoles = new HashSet<>();
        otherRoles.add(employeeRole);
        otherUser.setRoles(otherRoles);
        userRepository.save(otherUser);
    }

    @Test
    @DisplayName("GET /api/employees/profile - Giriş yapan kullanıcının profilini döndürmeli")
    void getMyProfile_ShouldReturnCurrentUser() throws Exception {
        String token = loginAndGetToken(EMPLOYEE_EMAIL, PASSWORD);

        mockMvc.perform(get("/api/employees/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMPLOYEE_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Profil"))
                .andExpect(jsonPath("$.lastName").value("Calisan"));
    }

    @Test
    @DisplayName("PUT /api/employees/profile - Telefon ve adres güncellenmeli")
    void updateMyProfile_ShouldUpdateFields() throws Exception {
        String token = loginAndGetToken(EMPLOYEE_EMAIL, PASSWORD);

        String body = """
                {
                  "phoneNumber": "+90 555 000 11 22",
                  "address": "İstanbul, Türkiye"
                }
                """;

        mockMvc.perform(put("/api/employees/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+90 555 000 11 22"))
                .andExpect(jsonPath("$.address").value("İstanbul, Türkiye"));
    }

    @Test
    @DisplayName("PUT /api/employees/profile - Geçersiz telefon formatında 400 ve doğrulama hatası dönmeli")
    void updateMyProfile_InvalidPhone_ShouldReturnBadRequest() throws Exception {
        String token = loginAndGetToken(EMPLOYEE_EMAIL, PASSWORD);

        String body = """
                {
                  "phoneNumber": "INVALID_PHONE_#",
                  "address": "Adres"
                }
                """;

        mockMvc.perform(put("/api/employees/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Doğrulama hatası"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phoneNumber"));
    }

    @Test
    @DisplayName("GET /api/employees/profile - Her kullanıcı sadece kendi profilini görebilmeli")
    void getMyProfile_TwoDifferentUsers_ShouldSeeOwnProfile() throws Exception {
        String token1 = loginAndGetToken(EMPLOYEE_EMAIL, PASSWORD);
        String token2 = loginAndGetToken(OTHER_EMPLOYEE_EMAIL, PASSWORD);

        // 1. Kullanıcı kendi profilini görür
        mockMvc.perform(get("/api/employees/profile")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMPLOYEE_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Profil"))
                .andExpect(jsonPath("$.lastName").value("Calisan"));

        // 2. Kullanıcı kendi profilini görür (diğerinin verisini değil)
        mockMvc.perform(get("/api/employees/profile")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(OTHER_EMPLOYEE_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Diger"))
                .andExpect(jsonPath("$.lastName").value("Calisan"));
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


