package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("EmployeeController - Leave Balance Integration Tests")
class EmployeeControllerLeaveBalanceIntegrationTest {

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
    private LeaveEntitlementRepository leaveEntitlementRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private Employee testEmployee;
    private User testUser;
    private LeaveType annualLeaveType;
    private LeaveType excuseLeaveType;
    private LeaveEntitlement testEntitlement;

    @BeforeEach
    void setUp() {
        // Clean up
        leaveEntitlementRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        leaveTypeRepository.deleteAll();
        departmentRepository.deleteAll();
        roleRepository.deleteAll();

        // Create Department
        Department department = new Department();
        department.setName("Test Department");
        department.setIsActive(true);
        department = departmentRepository.save(department);

        // Create Role
        Role employeeRole = new Role();
        employeeRole.setRoleName("EMPLOYEE");
        employeeRole.setIsActive(true);
        employeeRole = roleRepository.save(employeeRole);

        // Create Employee - 3 yıl kıdem (14 gün izin hakkı)
        testEmployee = new Employee();
        testEmployee.setFirstName("Test");
        testEmployee.setLastName("Employee");
        testEmployee.setEmail("test.employee@example.com");
        testEmployee.setJobTitle("Software Developer");
        testEmployee.setBirthDate(LocalDate.of(1990, 1, 1));
        testEmployee.setHireDate(LocalDate.now().minusYears(3)); // 3 yıl kıdem
        testEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        testEmployee.setDepartment(department);
        testEmployee.setIsActive(true);
        testEmployee = employeeRepository.save(testEmployee);

        // Create User
        testUser = new User();
        testUser.setEmployee(testEmployee);
        testUser.setPasswordHash("encodedPassword");
        testUser.setIsActive(true);
        testUser.setFailedLoginAttempts(0);
        Set<Role> roles = new HashSet<>();
        roles.add(employeeRole);
        testUser.setRoles(roles);
        testUser = userRepository.save(testUser);

        // Create Leave Types
        annualLeaveType = new LeaveType();
        annualLeaveType.setName("Yıllık İzin");
        annualLeaveType.setIsActive(true);
        annualLeaveType.setDeductsFromAnnual(true);
        annualLeaveType.setRequestUnit(com.cozumtr.leave_management_system.enums.RequestUnit.DAY);
        annualLeaveType.setWorkflowDefinition("HR,MANAGER,CEO");
        annualLeaveType = leaveTypeRepository.save(annualLeaveType);

        excuseLeaveType = new LeaveType();
        excuseLeaveType.setName("Mazeret İzni (Saatlik)");
        excuseLeaveType.setIsActive(true);
        excuseLeaveType.setDeductsFromAnnual(false);
        excuseLeaveType.setRequestUnit(com.cozumtr.leave_management_system.enums.RequestUnit.HOUR);
        excuseLeaveType.setWorkflowDefinition("MANAGER");
        excuseLeaveType = leaveTypeRepository.save(excuseLeaveType);

        // Create Leave Entitlement for current year
        int currentYear = LocalDate.now().getYear();
        testEntitlement = new LeaveEntitlement();
        testEntitlement.setEmployee(testEmployee);
        testEntitlement.setYear(currentYear);
        testEntitlement.setTotalHoursEntitled(new BigDecimal("112.0")); // 14 gün × 8 saat
        testEntitlement.setHoursUsed(new BigDecimal("40.0")); // 5 gün kullanılmış
        testEntitlement.setCarriedForwardHours(BigDecimal.ZERO);
        testEntitlement = leaveEntitlementRepository.save(testEntitlement);
    }

    @Test
    @WithMockUser(username = "test.employee@example.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/employees/me/balance - Çalışan kendi bakiyelerini görebilmeli")
    void getMyLeaveBalances_AsEmployee_ShouldReturnAllLeaveBalances() throws Exception {
        mockMvc.perform(get("/api/employees/me/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2))) // En az 2 izin türü (Yıllık + Mazeret)
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')]").exists())
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')].totalHours", hasItem(112.0)))
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')].hoursUsed", hasItem(40.0)))
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')].remainingHours", hasItem(72.0)))
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Mazeret İzni (Saatlik)')]").exists())
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Mazeret İzni (Saatlik)')].totalHours", hasItem(8))); 
    }

    @Test
    @WithMockUser(username = "test.employee@example.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/employees/me/balance - Entitlement yoksa otomatik oluşturulmalı")
    void getMyLeaveBalances_NoEntitlement_ShouldCreateAutomatically() throws Exception {
        // Given - Mevcut entitlement'ı sil
        leaveEntitlementRepository.deleteAll();

        // When & Then
        mockMvc.perform(get("/api/employees/me/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')]").exists());

        // Entitlement otomatik oluşturulmuş olmalı
        org.junit.jupiter.api.Assertions.assertTrue(leaveEntitlementRepository.findByEmployeeIdAndYear(
                testEmployee.getId(), LocalDate.now().getYear()).isPresent());
    }

    @Test
    @WithMockUser(username = "test.employee@example.com", roles = {"MANAGER"}) // EMPLOYEE rolü yok
    @DisplayName("GET /api/employees/me/balance - Sadece EMPLOYEE rolü erişebilmeli")
    void getMyLeaveBalances_WithoutEmployeeRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/employees/me/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/employees/me/balance - Unauthenticated kullanıcı erişememeli")
    void getMyLeaveBalances_Unauthenticated_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/employees/me/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test.employee@example.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/employees/me/balance - Gün hesaplaması doğru olmalı (totalDays, daysUsed, remainingDays)")
    void getMyLeaveBalances_ShouldCalculateDaysCorrectly() throws Exception {
        mockMvc.perform(get("/api/employees/me/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')].totalDays", hasItem(14)))
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')].daysUsed", hasItem(5)))
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')].remainingDays", hasItem(9)));
    }

    @Test
    @WithMockUser(username = "test.employee@example.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/employees/me/balance - Aktarılan izin ile bakiye hesaplama")
    void getMyLeaveBalances_WithCarriedForwardLeave_ShouldIncludeInTotal() throws Exception {
        // Given - Önceki yıldan 8 saat aktarılmış izin
        testEntitlement.setCarriedForwardHours(new BigDecimal("8.0")); // 8 saat aktarılan
        testEntitlement.setTotalHoursEntitled(new BigDecimal("120.0")); // 112 (14 gün) + 8 (aktarılan)
        leaveEntitlementRepository.save(testEntitlement);

        // When & Then
        mockMvc.perform(get("/api/employees/me/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')].totalHours", hasItem(120.0)))
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')].remainingHours", hasItem(80.0))); // 120 - 40
    }

    @Test
    @WithMockUser(username = "test.employee@example.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/employees/me/balance - Mazeret izni kullanıldığında bakiyenin güncellenmesi")
    void getMyLeaveBalances_AfterExcuseLeaveUsed_ShouldUpdateBalance() throws Exception {
        // Given - Bu ay için 2 saat mazeret izni kullanılmış (onaylanmış)
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        
        LeaveRequest excuseRequest = new LeaveRequest();
        excuseRequest.setEmployee(testEmployee);
        excuseRequest.setLeaveType(excuseLeaveType);
        excuseRequest.setStartDateTime(LocalDateTime.of(currentYear, currentMonth, 15, 9, 0));
        excuseRequest.setEndDateTime(LocalDateTime.of(currentYear, currentMonth, 15, 11, 0)); // 2 saat
        excuseRequest.setDurationHours(new BigDecimal("2.0"));
        excuseRequest.setRequestStatus(com.cozumtr.leave_management_system.enums.RequestStatus.APPROVED);
        excuseRequest.setReason("Doktor randevusu");
        excuseRequest.setWorkflowNextApproverRole("");
        leaveRequestRepository.save(excuseRequest);

        // When & Then
        mockMvc.perform(get("/api/employees/me/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Mazeret İzni (Saatlik)')].hoursUsed", hasItem(2.0)))
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Mazeret İzni (Saatlik)')].remainingHours", hasItem(6.0))); // 8 - 2
    }

    @Test
    @WithMockUser(username = "test.employee@example.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/employees/me/balance - Tüm izin türleri için year alanı dolu olmalı")
    void getMyLeaveBalances_ShouldIncludeYearForAllTypes() throws Exception {
        int currentYear = LocalDate.now().getYear();
        
        mockMvc.perform(get("/api/employees/me/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].year", everyItem(is(currentYear))));
    }
}

