package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.ForgotPasswordRequestDto;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.enums.NotificationChannel;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.RoleRepository;
import com.cozumtr.leave_management_system.repository.UserRepository;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.service.EmailService;
import com.cozumtr.leave_management_system.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Forgot Password Integration Tests")
@SuppressWarnings("removal")
class AuthControllerForgotPasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmsService smsService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        roleRepository.deleteAll();
        departmentRepository.deleteAll();

        Role employeeRole = new Role();
        employeeRole.setRoleName("EMPLOYEE");
        employeeRole.setIsActive(true);
        roleRepository.save(employeeRole);

        Department department = new Department();
        department.setName("IT");
        department.setIsActive(true);
        department = departmentRepository.save(department);

        Employee employee = new Employee();
        employee.setFirstName("Test");
        employee.setLastName("User");
        employee.setEmail("test@example.com");
        employee.setJobTitle("Engineer");
        employee.setBirthDate(LocalDate.of(1990, 1, 1));
        employee.setHireDate(LocalDate.of(2020, 1, 1));
        employee.setDailyWorkHours(new BigDecimal("8.0"));
        employee.setDepartment(department);
        employee.setIsActive(true);
        employee.setPhoneNumber("+905551112233");
        employee = employeeRepository.save(employee);

        user = new User();
        user.setEmployee(employee);
        user.setIsActive(true);
        user.setPasswordHash("encoded"); // not used here
        user.setRoles(new java.util.HashSet<>(Set.of(employeeRole))); // mutable set for JPA
        userRepository.save(user);
    }

    @Test
    @DisplayName("EMAIL kanalı seçildiğinde email gönderilir, SMS gönderilmez")
    void forgotPassword_EmailChannel_SendsEmailOnly() throws Exception {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
        request.setEmail("test@example.com");
        request.setChannel(NotificationChannel.EMAIL);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("şifre sıfırlama")));

        verify(emailService, times(1)).sendPasswordResetEmail(anyString(), anyString());
        verify(smsService, never()).sendSms(anyString(), anyString());

        User updated = userRepository.findByEmployeeEmail("test@example.com").orElseThrow();
        assertNotNull(updated.getPasswordResetToken());
        assertNotNull(updated.getPasswordResetExpires());
    }

    @Test
    @DisplayName("SMS kanalı seçildiğinde doğru telefonla SMS gönderilir, email gönderilmez")
    void forgotPassword_SmsChannel_SendsSmsWithMatchingPhone() throws Exception {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
        request.setEmail("test@example.com");
        request.setChannel(NotificationChannel.SMS);
        request.setPhoneNumber("+905551112233");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(smsService, times(1)).sendSms(anyString(), anyString());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());

        User updated = userRepository.findByEmployeeEmail("test@example.com").orElseThrow();
        assertNotNull(updated.getPasswordResetToken());
        assertNotNull(updated.getPasswordResetExpires());
    }

    @Test
    @DisplayName("SMS kanalı seçildiğinde telefon eşleşmezse 400 döner, token oluşturulmaz")
    void forgotPassword_SmsChannel_MismatchedPhone_ShouldFail() throws Exception {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
        request.setEmail("test@example.com");
        request.setChannel(NotificationChannel.SMS);
        request.setPhoneNumber("+900000000000");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(smsService, never()).sendSms(anyString(), anyString());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());

        User updated = userRepository.findByEmployeeEmail("test@example.com").orElseThrow();
        assertNull(updated.getPasswordResetToken());
        assertNull(updated.getPasswordResetExpires());
    }
}

