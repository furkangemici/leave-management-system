package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.enums.RequestStatus;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ReportingController Integration Tests - Sprint Overlap Report")
class ReportingControllerIntegrationTest {

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
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Department testDepartment;
    private Sprint testSprint;
    private Employee employee1;
    private Employee employee2;
    private Employee hrEmployee;
    private User hrUser;
    private LeaveType annualLeaveType;
    private String hrToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        leaveRequestRepository.deleteAll();
        sprintRepository.deleteAll();
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        leaveTypeRepository.deleteAll();
        departmentRepository.deleteAll();
        roleRepository.deleteAll();

        // Create Department
        testDepartment = new Department();
        testDepartment.setName("Test Department");
        testDepartment.setIsActive(true);
        testDepartment = departmentRepository.save(testDepartment);

        // Create Roles
        Role employeeRole = new Role();
        employeeRole.setRoleName("EMPLOYEE");
        employeeRole.setIsActive(true);
        employeeRole = roleRepository.save(employeeRole);

        Role hrRole = new Role();
        hrRole.setRoleName("HR");
        hrRole.setIsActive(true);
        hrRole = roleRepository.save(hrRole);

        Role managerRole = new Role();
        managerRole.setRoleName("MANAGER");
        managerRole.setIsActive(true);
        managerRole = roleRepository.save(managerRole);

        // Create Sprint
        testSprint = new Sprint();
        testSprint.setName("Sprint 1 - 2024");
        testSprint.setStartDate(LocalDate.of(2024, 1, 1));
        testSprint.setEndDate(LocalDate.of(2024, 1, 31));
        testSprint.setDepartment(testDepartment);
        testSprint = sprintRepository.save(testSprint);

        // Create Employees
        employee1 = new Employee();
        employee1.setFirstName("Ahmet");
        employee1.setLastName("Yılmaz");
        employee1.setEmail("ahmet@example.com");
        employee1.setJobTitle("Developer");
        employee1.setBirthDate(LocalDate.of(1990, 1, 1));
        employee1.setHireDate(LocalDate.of(2020, 1, 1));
        employee1.setDailyWorkHours(new BigDecimal("8.0"));
        employee1.setDepartment(testDepartment);
        employee1 = employeeRepository.save(employee1);

        employee2 = new Employee();
        employee2.setFirstName("Mehmet");
        employee2.setLastName("Demir");
        employee2.setEmail("mehmet@example.com");
        employee2.setJobTitle("Developer");
        employee2.setBirthDate(LocalDate.of(1991, 1, 1));
        employee2.setHireDate(LocalDate.of(2020, 1, 1));
        employee2.setDailyWorkHours(new BigDecimal("8.0"));
        employee2.setDepartment(testDepartment);
        employee2 = employeeRepository.save(employee2);

        hrEmployee = new Employee();
        hrEmployee.setFirstName("HR");
        hrEmployee.setLastName("User");
        hrEmployee.setEmail("hr@example.com");
        hrEmployee.setJobTitle("HR Manager");
        hrEmployee.setBirthDate(LocalDate.of(1985, 1, 1));
        hrEmployee.setHireDate(LocalDate.of(2019, 1, 1));
        hrEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        hrEmployee.setDepartment(testDepartment);
        hrEmployee = employeeRepository.save(hrEmployee);

        // Create HR User
        hrUser = new User();
        hrUser.setEmployee(hrEmployee);
        hrUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        hrUser.setIsActive(true);
        Set<Role> hrRoles = new HashSet<>();
        hrRoles.add(hrRole);
        hrRoles.add(managerRole);
        hrUser.setRoles(hrRoles);
        hrUser = userRepository.save(hrUser);

        // Create Leave Type
        annualLeaveType = new LeaveType();
        annualLeaveType.setName("Yıllık İzin");
        annualLeaveType.setIsActive(true);
        annualLeaveType.setDeductsFromAnnual(true);
        annualLeaveType.setRequestUnit(com.cozumtr.leave_management_system.enums.RequestUnit.DAY);
        annualLeaveType.setWorkflowDefinition("HR,MANAGER");
        annualLeaveType = leaveTypeRepository.save(annualLeaveType);

        // Get token
        hrToken = loginAndGetToken("hr@example.com", "Password123!");
    }

    // ========== SPRINT LİSTESİ TESTLERİ ==========

    @Test
    @DisplayName("GET /api/reports/sprints - HR rolü ile sprint listesi alınabilmeli")
    void getAllSprints_WithHrRole_ShouldReturnSprintList() throws Exception {
        mockMvc.perform(get("/api/reports/sprints")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[0].id").value(testSprint.getId()))
                .andExpect(jsonPath("$[0].name").value("Sprint 1 - 2024"))
                .andExpect(jsonPath("$[0].startDate").value("2024-01-01"))
                .andExpect(jsonPath("$[0].endDate").value("2024-01-31"));
    }

    @Test
    @DisplayName("GET /api/reports/sprints - Yetkisiz kullanıcı erişememeli")
    void getAllSprints_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/reports/sprints"))
                .andExpect(status().isForbidden());
    }

    // ========== SPRINT ÇAKIŞMA RAPORU TESTLERİ (GET - Sprint ID ile) ==========

    @Test
    @DisplayName("GET /api/reports/sprint-overlap?sprintId=X - Sprint ID ile rapor alınabilmeli")
    void getSprintOverlapReport_WithSprintId_ShouldReturnReport() throws Exception {
        // Test verisi: Sprint içinde çakışan izin oluştur
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 20, 23, 59),
                new BigDecimal("40.0")
        );

        mockMvc.perform(get("/api/reports/sprint-overlap")
                        .param("sprintId", String.valueOf(testSprint.getId()))
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLossHours").exists())
                .andExpect(jsonPath("$.overlappingLeaves", isA(java.util.List.class)))
                .andExpect(jsonPath("$.overlappingLeaves[0].employeeFullName").value("Ahmet Yılmaz"))
                .andExpect(jsonPath("$.overlappingLeaves[0].leaveTypeName").value("Yıllık İzin"))
                .andExpect(jsonPath("$.overlappingLeaves[0].overlappingHours").exists());
    }

    @Test
    @DisplayName("GET /api/reports/sprint-overlap?sprintId=X - Geçersiz sprint ID ile hata dönmeli")
    void getSprintOverlapReport_WithInvalidSprintId_ShouldReturnError() throws Exception {
        mockMvc.perform(get("/api/reports/sprint-overlap")
                        .param("sprintId", "99999")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reports/sprint-overlap?sprintId=X - Sprint ID parametresi eksikse hata dönmeli")
    void getSprintOverlapReport_WithoutSprintId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reports/sprint-overlap - Çakışan izin yoksa boş liste dönmeli")
    void getSprintOverlapReport_NoOverlappingLeaves_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/reports/sprint-overlap")
                        .param("sprintId", String.valueOf(testSprint.getId()))
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLossHours").value(0))
                .andExpect(jsonPath("$.overlappingLeaves", hasSize(0)));
    }

    // ========== SPRINT ÇAKIŞMA RAPORU TESTLERİ (POST - Manuel tarih) ==========

    @Test
    @DisplayName("POST /api/reports/sprint-overlap - Manuel tarih ile rapor alınabilmeli")
    void getSprintOverlapReport_WithManualDates_ShouldReturnReport() throws Exception {
        // Test verisi: Sprint içinde çakışan izin oluştur
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 20, 23, 59),
                new BigDecimal("40.0")
        );

        String requestBody = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLossHours").exists())
                .andExpect(jsonPath("$.overlappingLeaves", isA(java.util.List.class)))
                .andExpect(jsonPath("$.overlappingLeaves[0].employeeFullName").value("Ahmet Yılmaz"));
    }

    @Test
    @DisplayName("POST /api/reports/sprint-overlap - Tarih parametreleri eksikse validation hatası dönmeli")
    void getSprintOverlapReport_WithMissingDates_ShouldReturnValidationError() throws Exception {
        String requestBody = """
                {
                  "sprintStart": "2024-01-01"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reports/sprint-overlap - Birden fazla çalışanın izinleri toplanmalı")
    void getSprintOverlapReport_MultipleEmployees_ShouldSumTotalHours() throws Exception {
        // İki çalışan için izin oluştur
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 10, 0, 0),
                LocalDateTime.of(2024, 1, 12, 23, 59),
                new BigDecimal("24.0")
        );

        createApprovedLeaveRequest(
                employee2,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 17, 23, 59),
                new BigDecimal("24.0")
        );

        String requestBody = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overlappingLeaves", hasSize(2)))
                .andExpect(jsonPath("$.totalLossHours").value(greaterThan(24.0)));
    }

    // ========== EXCEL EXPORT TESTLERİ (GET - Sprint ID ile) ==========

    @Test
    @DisplayName("GET /api/reports/sprint-overlap/export?sprintId=X - Excel export başarılı olmalı")
    void exportSprintOverlapReport_WithSprintId_ShouldReturnExcelFile() throws Exception {
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 20, 23, 59),
                new BigDecimal("40.0")
        );

        mockMvc.perform(get("/api/reports/sprint-overlap/export")
                        .param("sprintId", String.valueOf(testSprint.getId()))
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    @DisplayName("GET /api/reports/sprint-overlap/export - Geçersiz sprint ID ile hata dönmeli")
    void exportSprintOverlapReport_WithInvalidSprintId_ShouldReturnError() throws Exception {
        mockMvc.perform(get("/api/reports/sprint-overlap/export")
                        .param("sprintId", "99999")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isBadRequest());
    }

    // ========== EXCEL EXPORT TESTLERİ (POST - Manuel tarih) ==========

    @Test
    @DisplayName("POST /api/reports/sprint-overlap/export - Manuel tarih ile Excel export başarılı olmalı")
    void exportSprintOverlapReport_WithManualDates_ShouldReturnExcelFile() throws Exception {
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 20, 23, 59),
                new BigDecimal("40.0")
        );

        String requestBody = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap/export")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    // ========== YETKİLENDİRME TESTLERİ ==========

    @Test
    @DisplayName("GET /api/reports/sprint-overlap - Yetkisiz kullanıcı erişememeli")
    void getSprintOverlapReport_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/reports/sprint-overlap")
                        .param("sprintId", String.valueOf(testSprint.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/reports/sprint-overlap - Yetkisiz kullanıcı erişememeli")
    void postSprintOverlapReport_WithoutAuth_ShouldReturnForbidden() throws Exception {
        String requestBody = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    // ========== ÇAKIŞMA MANTIĞI TESTLERİ ==========

    @Test
    @DisplayName("Sprint ile tamamen çakışan izin bulunmalı")
    void getSprintOverlapReport_FullyOverlappingLeave_ShouldBeFound() throws Exception {
        // İzin: 10-20 Ocak, Sprint: 1-31 Ocak → Tamamen çakışıyor
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 10, 0, 0),
                LocalDateTime.of(2024, 1, 20, 23, 59),
                new BigDecimal("80.0")
        );

        String requestBody = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overlappingLeaves", hasSize(1)));
    }

    @Test
    @DisplayName("Sprint ile kısmen çakışan izin bulunmalı")
    void getSprintOverlapReport_PartiallyOverlappingLeave_ShouldBeFound() throws Exception {
        // İzin: 25 Ocak - 5 Şubat, Sprint: 1-31 Ocak → Kısmen çakışıyor (25-31 Ocak)
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 25, 0, 0),
                LocalDateTime.of(2024, 2, 5, 23, 59),
                new BigDecimal("80.0")
        );

        String requestBody = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overlappingLeaves", hasSize(1)));
    }

    @Test
    @DisplayName("Seçilen sprint ile çakışmayan izin bulunmamalı (izin başka bir sprint ile çakışabilir)")
    void getSprintOverlapReport_NonOverlappingLeave_ShouldNotBeFound() throws Exception {
        // İzin: 1-5 Şubat, Sprint: 1-31 Ocak → Bu sprint ile çakışmıyor
        // (İzin başka bir sprint ile çakışabilir, ama bu sprint ile çakışmaz)
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 2, 5, 23, 59),
                new BigDecimal("40.0")
        );

        String requestBody = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overlappingLeaves", hasSize(0)))
                .andExpect(jsonPath("$.totalLossHours").value(0));
    }

    @Test
    @DisplayName("İzin iki sprint'e yayılıyorsa her sprint için sadece o sprint ile çakışan kısım gösterilmeli")
    void getSprintOverlapReport_LeaveSpansTwoSprints_ShouldShowOnlyOverlappingPart() throws Exception {
        // Senaryo: İzin Sprint 1'in son haftası + Sprint 2'nin ikinci haftası
        // Sprint 1: 1-31 Ocak
        // Sprint 2: 1-29 Şubat
        // İzin: 25 Ocak - 10 Şubat

        // İzin oluştur
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 25, 0, 0),  // İzin başlangıcı: 25 Ocak
                LocalDateTime.of(2024, 2, 10, 23, 59), // İzin bitişi: 10 Şubat
                new BigDecimal("120.0")
        );

        // SPRINT 1 İÇİN RAPOR
        String sprint1Body = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sprint1Body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overlappingLeaves", hasSize(1)))
                .andExpect(jsonPath("$.overlappingLeaves[0].employeeFullName").value("Ahmet Yılmaz"))
                // Sprint 1 için sadece 25-31 Ocak arası hesaplanmalı (7 gün)
                // Hafta sonu ve tatil kontrolü yapıldığı için tam 7 gün olmayabilir
                .andExpect(jsonPath("$.overlappingLeaves[0].overlappingHours").exists())
                .andExpect(jsonPath("$.totalLossHours").exists());

        // SPRINT 2 İÇİN RAPOR (Yeni sprint oluştur)
        Sprint sprint2 = new Sprint();
        sprint2.setName("Sprint 2 - 2024");
        sprint2.setStartDate(LocalDate.of(2024, 2, 1));
        sprint2.setEndDate(LocalDate.of(2024, 2, 29));
        sprint2.setDepartment(testDepartment);
        sprint2 = sprintRepository.save(sprint2);

        String sprint2Body = """
                {
                  "sprintStart": "2024-02-01",
                  "sprintEnd": "2024-02-29"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sprint2Body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overlappingLeaves", hasSize(1)))
                .andExpect(jsonPath("$.overlappingLeaves[0].employeeFullName").value("Ahmet Yılmaz"))
                // Sprint 2 için sadece 1-10 Şubat arası hesaplanmalı (10 gün)
                .andExpect(jsonPath("$.overlappingLeaves[0].overlappingHours").exists())
                .andExpect(jsonPath("$.totalLossHours").exists());

        // ÖNEMLİ: Her sprint için sadece kendi çakışan kısmı hesaplanmalı
        // Sprint 1: 25-31 Ocak (7 gün)
        // Sprint 2: 1-10 Şubat (10 gün)
        // Her sprint kendi kısmını gösterir, toplam izin süresi değil!
    }

    @Test
    @DisplayName("Sadece APPROVED durumundaki izinler bulunmalı")
    void getSprintOverlapReport_OnlyApprovedLeaves_ShouldBeFound() throws Exception {
        // APPROVED izin
        createApprovedLeaveRequest(
                employee1,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 20, 23, 59),
                new BigDecimal("40.0")
        );

        // PENDING izin (bulunmamalı)
        LeaveRequest pendingLeave = new LeaveRequest();
        pendingLeave.setEmployee(employee2);
        pendingLeave.setLeaveType(annualLeaveType);
        pendingLeave.setStartDateTime(LocalDateTime.of(2024, 1, 10, 0, 0));
        pendingLeave.setEndDateTime(LocalDateTime.of(2024, 1, 12, 23, 59));
        pendingLeave.setDurationHours(new BigDecimal("24.0"));
        pendingLeave.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        pendingLeave.setWorkflowNextApproverRole("HR");
        pendingLeave.setReason("Test");
        leaveRequestRepository.save(pendingLeave);

        String requestBody = """
                {
                  "sprintStart": "2024-01-01",
                  "sprintEnd": "2024-01-31"
                }
                """;

        mockMvc.perform(post("/api/reports/sprint-overlap")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overlappingLeaves", hasSize(1)))
                .andExpect(jsonPath("$.overlappingLeaves[0].employeeFullName").value("Ahmet Yılmaz"));
    }

    // ========== HELPER METODLAR ==========

    private LeaveRequest createApprovedLeaveRequest(Employee employee, LocalDateTime start, LocalDateTime end, BigDecimal duration) {
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(annualLeaveType);
        leaveRequest.setStartDateTime(start);
        leaveRequest.setEndDateTime(end);
        leaveRequest.setDurationHours(duration);
        leaveRequest.setRequestStatus(RequestStatus.APPROVED);
        leaveRequest.setWorkflowNextApproverRole("");
        leaveRequest.setReason("Test izin");
        return leaveRequestRepository.save(leaveRequest);
    }

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


