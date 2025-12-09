package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("LeaveRequestController Integration Tests")
class LeaveRequestControllerIntegrationTest {

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
    private LeaveEntitlementRepository leaveEntitlementRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveApprovalHistoryRepository leaveApprovalHistoryRepository;

    @Autowired
    private PublicHolidayRepository publicHolidayRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee testEmployee;
    private User testUser;
    private Employee hrUser;
    private User hrUserEntity;
    private Employee managerUser;
    private User managerUserEntity;
    private Employee ceoUser;
    private User ceoUserEntity;
    private LeaveType annualLeaveType;
    private LeaveType excuseLeaveType;
    private LeaveEntitlement testEntitlement;
    private String employeeToken;
    private String hrToken;
    private String managerToken;
    private String ceoToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        leaveRequestRepository.deleteAll();
        leaveEntitlementRepository.deleteAll();
        publicHolidayRepository.deleteAll();
        leaveApprovalHistoryRepository.deleteAll();
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

        Role ceoRole = new Role();
        ceoRole.setRoleName("CEO");
        ceoRole.setIsActive(true);
        ceoRole = roleRepository.save(ceoRole);

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

        // Create Employee User
        testUser = new User();
        testUser.setEmployee(testEmployee);
        testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser.setIsActive(true);
        testUser.setFailedLoginAttempts(0);
        Set<Role> employeeRoles = new HashSet<>();
        employeeRoles.add(employeeRole);
        testUser.setRoles(employeeRoles);
        testUser = userRepository.save(testUser);

        // Create HR Employee
        hrUser = new Employee();
        hrUser.setFirstName("HR");
        hrUser.setLastName("Manager");
        hrUser.setEmail("hr@example.com");
        hrUser.setJobTitle("HR Manager");
        hrUser.setBirthDate(LocalDate.of(1985, 1, 1));
        hrUser.setHireDate(LocalDate.now().minusYears(5));
        hrUser.setDailyWorkHours(new BigDecimal("8.0"));
        hrUser.setDepartment(department);
        hrUser.setIsActive(true);
        hrUser = employeeRepository.save(hrUser);

        // Create HR User
        hrUserEntity = new User();
        hrUserEntity.setEmployee(hrUser);
        hrUserEntity.setPasswordHash(passwordEncoder.encode("Password123!"));
        hrUserEntity.setIsActive(true);
        hrUserEntity.setFailedLoginAttempts(0);
        Set<Role> hrRoles = new HashSet<>();
        hrRoles.add(employeeRole);
        hrRoles.add(hrRole);
        hrUserEntity.setRoles(hrRoles);
        hrUserEntity = userRepository.save(hrUserEntity);

        // Create Manager Employee
        managerUser = new Employee();
        managerUser.setFirstName("Manager");
        managerUser.setLastName("User");
        managerUser.setEmail("manager@example.com");
        managerUser.setJobTitle("Team Manager");
        managerUser.setBirthDate(LocalDate.of(1987, 2, 1));
        managerUser.setHireDate(LocalDate.now().minusYears(4));
        managerUser.setDailyWorkHours(new BigDecimal("8.0"));
        managerUser.setDepartment(department);
        managerUser.setIsActive(true);
        managerUser = employeeRepository.save(managerUser);

        // Create Manager User
        managerUserEntity = new User();
        managerUserEntity.setEmployee(managerUser);
        managerUserEntity.setPasswordHash(passwordEncoder.encode("Password123!"));
        managerUserEntity.setIsActive(true);
        managerUserEntity.setFailedLoginAttempts(0);
        Set<Role> managerRoles = new HashSet<>();
        managerRoles.add(employeeRole);
        managerRoles.add(managerRole);
        managerUserEntity.setRoles(managerRoles);
        managerUserEntity = userRepository.save(managerUserEntity);

        // Create CEO Employee
        ceoUser = new Employee();
        ceoUser.setFirstName("Ceo");
        ceoUser.setLastName("User");
        ceoUser.setEmail("ceo@example.com");
        ceoUser.setJobTitle("CEO");
        ceoUser.setBirthDate(LocalDate.of(1980, 3, 1));
        ceoUser.setHireDate(LocalDate.now().minusYears(8));
        ceoUser.setDailyWorkHours(new BigDecimal("8.0"));
        ceoUser.setDepartment(department);
        ceoUser.setIsActive(true);
        ceoUser = employeeRepository.save(ceoUser);

        // Create CEO User
        ceoUserEntity = new User();
        ceoUserEntity.setEmployee(ceoUser);
        ceoUserEntity.setPasswordHash(passwordEncoder.encode("Password123!"));
        ceoUserEntity.setIsActive(true);
        ceoUserEntity.setFailedLoginAttempts(0);
        Set<Role> ceoRoles = new HashSet<>();
        ceoRoles.add(employeeRole);
        ceoRoles.add(ceoRole);
        ceoUserEntity.setRoles(ceoRoles);
        ceoUserEntity = userRepository.save(ceoUserEntity);

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
        testEntitlement.setHoursUsed(BigDecimal.ZERO);
        testEntitlement.setCarriedForwardHours(BigDecimal.ZERO);
        testEntitlement = leaveEntitlementRepository.save(testEntitlement);

        // Get tokens
        employeeToken = loginAndGetToken("test.employee@example.com", "Password123!");
        hrToken = loginAndGetToken("hr@example.com", "Password123!");
        managerToken = loginAndGetToken("manager@example.com", "Password123!");
        ceoToken = loginAndGetToken("ceo@example.com", "Password123!");
    }

    // ========== İZİN TALEBİ OLUŞTURMA TESTLERİ ==========

    @Test
    @DisplayName("POST /api/leaves - Yıllık izin talebi oluşturulmalı")
    void createLeaveRequest_AnnualLeave_ShouldCreateSuccessfully() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime endDate = LocalDateTime.now().plusDays(3).withHour(17).withMinute(0);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Yıllık izin talebi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaveTypeName").value("Yıllık İzin"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("POST /api/leaves - Mazeret izni 2 saat başarılı")
    void createLeaveRequest_ExcuseLeave_2Hours_ShouldCreateSuccessfully() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0); // 2 saat

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Doktor randevusu"
                }
                """, excuseLeaveType.getId(), startDate, endDate);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaveTypeName").value("Mazeret İzni (Saatlik)"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("POST /api/leaves - Mazeret izni 2 saatten farklı olursa hata")
    void createLeaveRequest_ExcuseLeave_Not2Hours_ShouldReturnBadRequest() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0); // 3 saat

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Doktor randevusu"
                }
                """, excuseLeaveType.getId(), startDate, endDate);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("sadece 2 saat")));
    }

    @Test
    @DisplayName("POST /api/leaves - Ayda 5. mazeret izni talebi limiti aşmalı")
    void createLeaveRequest_ExcuseLeave_5thRequest_ShouldReturnBadRequest() throws Exception {
        // Önce 4 mazeret izni oluştur
        LocalDate nextMonthWeekday = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfNextMonth())
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDateTime baseDate = nextMonthWeekday.atTime(9, 0);
        for (int i = 1; i <= 4; i++) {
            LocalDateTime startDate = baseDate.plusDays(i);
            LocalDateTime endDate = baseDate.plusDays(i).withHour(11); // 2 saat

            String requestBody = String.format("""
                    {
                      "leaveTypeId": %d,
                      "startDate": "%s",
                      "endDate": "%s",
                      "reason": "Mazeret izni %d"
                    }
                    """, excuseLeaveType.getId(), startDate, endDate, i);

            mockMvc.perform(post("/api/leaves")
                            .header("Authorization", "Bearer " + employeeToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }

        // Onaylama (APPROVED olmalı ki sayıma dahil olsun)
        var requests = leaveRequestRepository.findByEmployeeId(testEmployee.getId());
        for (var request : requests) {
            if (request.getLeaveType().getId().equals(excuseLeaveType.getId())) {
                request.setRequestStatus(com.cozumtr.leave_management_system.enums.RequestStatus.APPROVED);
                leaveRequestRepository.save(request);
            }
        }

        // 5. mazeret izni talebi - Limit aşılmış olmalı
        LocalDateTime startDate = baseDate.plusDays(10);
        LocalDateTime endDate = baseDate.plusDays(10).withHour(11);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "5. mazeret izni"
                }
                """, excuseLeaveType.getId(), startDate, endDate);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("sayısı limiti")));
    }

    @Test
    @DisplayName("POST /api/leaves - Yetersiz yıllık izin bakiyesi ile hata")
    void createLeaveRequest_InsufficientAnnualLeaveBalance_ShouldReturnBadRequest() throws Exception {
        // Bakiye sıfırla
        testEntitlement.setHoursUsed(new BigDecimal("112.0")); // Tüm hak kullanılmış
        leaveEntitlementRepository.save(testEntitlement);

        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "İzin talebi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Yetersiz")));
    }

    // ========== İZİN TALEBİ LİSTELEME TESTLERİ ==========

    @Test
    @DisplayName("GET /api/leaves/me - Çalışan kendi izin taleplerini görebilmeli")
    void getMyLeaveRequests_ShouldReturnOwnRequests() throws Exception {
        // Önce bir izin talebi oluştur
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Test izin"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated());

        // Kendi izin taleplerini listele
        mockMvc.perform(get("/api/leaves/me")
                        .header("Authorization", "Bearer " + employeeToken)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].leaveTypeName").value("Yıllık İzin"));
    }

    // ========== İZİN TALEBİ ONAYLAMA TESTLERİ ==========

    @Test
    @DisplayName("POST /api/leaves/{id}/approve - HR izin talebini onaylayabilmeli")
    void approveLeaveRequest_AsHR_ShouldApproveSuccessfully() throws Exception {
        // Önce bir izin talebi oluştur
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String createRequestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Test izin"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String createResponse = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> createResponseMap = objectMapper.readValue(createResponse, Map.class);
        Long leaveRequestId = Long.valueOf(createResponseMap.get("id").toString());

        // Onayla
        mockMvc.perform(post("/api/leaves/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + hrToken)
                        .param("comments", "Onaylandı")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(in(Set.of("APPROVED", "APPROVED_HR", "APPROVED_MANAGER"))));

        // Bakiye düşmüş olmalı (final approval ise)
        // Final approval ise hoursUsed artmış olmalı
    }

    @Test
    @DisplayName("POST /api/leaves/{id}/approve - Onay sonrası audit log kaydedilmeli")
    void approveLeaveRequest_ShouldCreateAuditLog() throws Exception {
        // İzin talebi oluştur
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String createRequestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Audit log testi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String createResponse = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> createResponseMap = objectMapper.readValue(createResponse, Map.class);
        Long leaveRequestId = Long.valueOf(createResponseMap.get("id").toString());

        // Onayla
        mockMvc.perform(post("/api/leaves/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + hrToken)
                        .param("comments", "Onaylandı (audit)")
                        .with(csrf()))
                .andExpect(status().isOk());

        var histories = leaveApprovalHistoryRepository.findAll();
        assertEquals(1, histories.size());
        assertEquals(RequestStatus.APPROVED_HR, histories.get(0).getAction());
        assertEquals("Onaylandı (audit)", histories.get(0).getComments());
        assertEquals(leaveRequestId, histories.get(0).getLeaveRequest().getId());
        assertEquals(hrUser.getId(), histories.get(0).getApprover().getId());
    }

    @Test
    @DisplayName("POST /api/leaves/{id}/approve - Çok adımlı onay süreci audit log'u sıralı kaydedilmeli")
    void approveLeaveRequest_MultiStep_ShouldCreateSequentialAuditLogs() throws Exception {
        // İzin talebi oluştur
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String createRequestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Çok adımlı onay testi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String createResponse = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> createResponseMap = objectMapper.readValue(createResponse, Map.class);
        Long leaveRequestId = Long.valueOf(createResponseMap.get("id").toString());

        // HR onayı
        mockMvc.perform(post("/api/leaves/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + hrToken)
                        .param("comments", "HR onayladı")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(in(Set.of("APPROVED", "APPROVED_HR", "APPROVED_MANAGER"))));

        // MANAGER onayı
        mockMvc.perform(post("/api/leaves/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("comments", "Manager onayladı")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(in(Set.of("APPROVED", "APPROVED_MANAGER"))));

        // CEO onayı (final)
        mockMvc.perform(post("/api/leaves/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + ceoToken)
                        .param("comments", "CEO onayladı")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        var histories = leaveApprovalHistoryRepository.findAll();
        Assertions.assertThat(histories).hasSize(3);
        histories.sort(Comparator.comparing(LeaveApprovalHistory::getCreatedAt));

        Assertions.assertThat(histories.get(0).getAction())
                .isIn(RequestStatus.APPROVED_HR, RequestStatus.APPROVED);
        Assertions.assertThat(histories.get(0).getApprover().getId()).isEqualTo(hrUser.getId());
        Assertions.assertThat(histories.get(0).getComments()).isEqualTo("HR onayladı");

        Assertions.assertThat(histories.get(1).getAction())
                .isIn(RequestStatus.APPROVED_MANAGER, RequestStatus.APPROVED);
        Assertions.assertThat(histories.get(1).getApprover().getId()).isEqualTo(managerUser.getId());
        Assertions.assertThat(histories.get(1).getComments()).isEqualTo("Manager onayladı");

        Assertions.assertThat(histories.get(2).getAction())
                .isEqualTo(RequestStatus.APPROVED);
        Assertions.assertThat(histories.get(2).getApprover().getId()).isEqualTo(ceoUser.getId());
        Assertions.assertThat(histories.get(2).getComments()).isEqualTo("CEO onayladı");
    }

    @Test
    @DisplayName("GET /api/leaves/{id}/history - Çalışan kendi talebinin geçmişini görebilmeli")
    void getLeaveHistory_AsOwner_ShouldReturnChronologicalHistory() throws Exception {
        // İzin talebi oluştur
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String createRequestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "History endpoint testi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String createResponse = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> createResponseMap = objectMapper.readValue(createResponse, Map.class);
        Long leaveRequestId = Long.valueOf(createResponseMap.get("id").toString());

        // HR, Manager, CEO onayları
        mockMvc.perform(post("/api/leaves/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + hrToken)
                        .param("comments", "HR onayladı")
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/leaves/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("comments", "Manager onayladı")
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/leaves/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + ceoToken)
                        .param("comments", "CEO onayladı")
                        .with(csrf()))
                .andExpect(status().isOk());

        // Geçmişi çek (talep sahibi olarak)
        mockMvc.perform(get("/api/leaves/{id}/history", leaveRequestId)
                        .header("Authorization", "Bearer " + employeeToken)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].comments").value("HR onayladı"))
                .andExpect(jsonPath("$[1].comments").value("Manager onayladı"))
                .andExpect(jsonPath("$[2].comments").value("CEO onayladı"))
                .andExpect(jsonPath("$[0].approverFullName").value(hrUser.getFirstName() + " " + hrUser.getLastName()));
    }

    @Test
    @DisplayName("POST /api/leaves/{id}/reject - HR izin talebini reddedebilmeli")
    void rejectLeaveRequest_AsHR_ShouldRejectSuccessfully() throws Exception {
        // Önce bir izin talebi oluştur
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String createRequestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Test izin"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String createResponse = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> createResponseMap = objectMapper.readValue(createResponse, Map.class);
        Long leaveRequestId = Long.valueOf(createResponseMap.get("id").toString());

        // Reddet
        mockMvc.perform(post("/api/leaves/{id}/reject", leaveRequestId)
                        .header("Authorization", "Bearer " + hrToken)
                        .param("comments", "Reddedildi")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /api/leaves/{id}/reject - Red sonrası audit log kaydedilmeli")
    void rejectLeaveRequest_ShouldCreateAuditLog() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String createRequestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Audit log reject testi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String createResponse = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> createResponseMap = objectMapper.readValue(createResponse, Map.class);
        Long leaveRequestId = Long.valueOf(createResponseMap.get("id").toString());

        mockMvc.perform(post("/api/leaves/{id}/reject", leaveRequestId)
                        .header("Authorization", "Bearer " + hrToken)
                        .param("comments", "Red edildi (audit)")
                        .with(csrf()))
                .andExpect(status().isOk());

        var histories = leaveApprovalHistoryRepository.findAll();
        assertEquals(1, histories.size());
        assertEquals(RequestStatus.REJECTED, histories.get(0).getAction());
        assertEquals("Red edildi (audit)", histories.get(0).getComments());
        assertEquals(leaveRequestId, histories.get(0).getLeaveRequest().getId());
        assertEquals(hrUser.getId(), histories.get(0).getApprover().getId());
    }

    @Test
    @DisplayName("DELETE /api/leaves/{id} - Çalışan kendi izin talebini iptal edebilmeli")
    void cancelLeaveRequest_AsOwner_ShouldCancelSuccessfully() throws Exception {
        // Önce bir izin talebi oluştur
        LocalDateTime startDate = LocalDateTime.now().plusDays(1).withHour(9);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2).withHour(17);

        String createRequestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Test izin"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String createResponse = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> createResponseMap = objectMapper.readValue(createResponse, Map.class);
        Long leaveRequestId = Long.valueOf(createResponseMap.get("id").toString());

        // İptal et
        mockMvc.perform(delete("/api/leaves/{id}", leaveRequestId)
                        .header("Authorization", "Bearer " + employeeToken)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("İzin talebi başarıyla iptal edildi."));

        // İptal edilmiş olmalı
        var request = leaveRequestRepository.findById(leaveRequestId);
        assert request.isPresent();
        assert request.get().getRequestStatus() == com.cozumtr.leave_management_system.enums.RequestStatus.CANCELLED;
    }

    @Test
    @DisplayName("GET /api/leaves/types - Tüm aktif izin türleri listelenebilmeli")
    void getAllLeaveTypes_ShouldReturnActiveTypes() throws Exception {
        mockMvc.perform(get("/api/leaves/types")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[?(@.name == 'Yıllık İzin')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Mazeret İzni (Saatlik)')]").exists());
    }

    // ========== EKİP İZİN TAKİBİ (TEAM VISIBILITY) TESTLERİ ==========

    @Test
    @DisplayName("GET /api/leaves/team-calendar - Departmandaki onaylanmış izinleri getirmeli")
    void getTeamCalendar_ShouldReturnApprovedLeavesFromSameDepartment() throws Exception {
        // Arrange: Farklı departmanlar oluştur
        Department itDepartment = departmentRepository.findByName("Test Department")
                .orElseThrow();
        
        Department hrDepartment = new Department();
        hrDepartment.setName("HR Department");
        hrDepartment.setIsActive(true);
        hrDepartment = departmentRepository.save(hrDepartment);
        
        // Aynı departmandan başka bir çalışan oluştur
        Employee teamMember = new Employee();
        teamMember.setFirstName("Team");
        teamMember.setLastName("Member");
        teamMember.setEmail("team.member@example.com");
        teamMember.setJobTitle("Developer");
        teamMember.setBirthDate(LocalDate.of(1992, 5, 15));
        teamMember.setHireDate(LocalDate.now().minusYears(2));
        teamMember.setDailyWorkHours(new BigDecimal("8.0"));
        teamMember.setDepartment(itDepartment);
        teamMember.setIsActive(true);
        teamMember = employeeRepository.save(teamMember);
        
        // Team member için User oluştur
        User teamMemberUser = new User();
        teamMemberUser.setEmployee(teamMember);
        teamMemberUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        teamMemberUser.setIsActive(true);
        teamMemberUser.setFailedLoginAttempts(0);
        Set<Role> employeeRoles = new HashSet<>();
        employeeRoles.add(roleRepository.findByRoleName("EMPLOYEE").orElseThrow());
        teamMemberUser.setRoles(employeeRoles);
        userRepository.save(teamMemberUser);
        
        // Farklı departmandan çalışan oluştur
        Employee otherDeptEmployee = new Employee();
        otherDeptEmployee.setFirstName("Other");
        otherDeptEmployee.setLastName("Dept");
        otherDeptEmployee.setEmail("other.dept@example.com");
        otherDeptEmployee.setJobTitle("HR Specialist");
        otherDeptEmployee.setBirthDate(LocalDate.of(1990, 3, 20));
        otherDeptEmployee.setHireDate(LocalDate.now().minusYears(1));
        otherDeptEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        otherDeptEmployee.setDepartment(hrDepartment);
        otherDeptEmployee.setIsActive(true);
        otherDeptEmployee = employeeRepository.save(otherDeptEmployee);
        
        // Farklı departmandan çalışan için User oluştur
        User otherDeptUser = new User();
        otherDeptUser.setEmployee(otherDeptEmployee);
        otherDeptUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        otherDeptUser.setIsActive(true);
        otherDeptUser.setFailedLoginAttempts(0);
        otherDeptUser.setRoles(employeeRoles);
        userRepository.save(otherDeptUser);
        
        // Gelecekteki onaylanmış izin (aynı departman - gösterilmeli)
        LocalDateTime futureStart = LocalDateTime.now().plusDays(5).withHour(9).withMinute(0);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(7).withHour(17).withMinute(0);
        LeaveRequest approvedFutureLeave = new LeaveRequest();
        approvedFutureLeave.setEmployee(teamMember);
        approvedFutureLeave.setLeaveType(annualLeaveType);
        approvedFutureLeave.setRequestStatus(RequestStatus.APPROVED);
        approvedFutureLeave.setStartDateTime(futureStart);
        approvedFutureLeave.setEndDateTime(futureEnd);
        approvedFutureLeave.setDurationHours(new BigDecimal("16.0"));
        approvedFutureLeave.setReason("Gelecek izin");
        approvedFutureLeave.setWorkflowNextApproverRole("NONE");
        approvedFutureLeave = leaveRequestRepository.save(approvedFutureLeave);
        
        // Şu anda devam eden onaylanmış izin (aynı departman - gösterilmeli)
        LocalDateTime currentStart = LocalDateTime.now().minusDays(1).withHour(9).withMinute(0);
        LocalDateTime currentEnd = LocalDateTime.now().plusDays(1).withHour(17).withMinute(0);
        LeaveRequest approvedCurrentLeave = new LeaveRequest();
        approvedCurrentLeave.setEmployee(teamMember);
        approvedCurrentLeave.setLeaveType(annualLeaveType);
        approvedCurrentLeave.setRequestStatus(RequestStatus.APPROVED);
        approvedCurrentLeave.setStartDateTime(currentStart);
        approvedCurrentLeave.setEndDateTime(currentEnd);
        approvedCurrentLeave.setDurationHours(new BigDecimal("16.0"));
        approvedCurrentLeave.setReason("Devam eden izin");
        approvedCurrentLeave.setWorkflowNextApproverRole("NONE");
        approvedCurrentLeave = leaveRequestRepository.save(approvedCurrentLeave);
        
        // Geçmişteki onaylanmış izin (aynı departman - gösterilmemeli)
        LocalDateTime pastStart = LocalDateTime.now().minusDays(10).withHour(9).withMinute(0);
        LocalDateTime pastEnd = LocalDateTime.now().minusDays(8).withHour(17).withMinute(0);
        LeaveRequest approvedPastLeave = new LeaveRequest();
        approvedPastLeave.setEmployee(teamMember);
        approvedPastLeave.setLeaveType(annualLeaveType);
        approvedPastLeave.setRequestStatus(RequestStatus.APPROVED);
        approvedPastLeave.setStartDateTime(pastStart);
        approvedPastLeave.setEndDateTime(pastEnd);
        approvedPastLeave.setDurationHours(new BigDecimal("16.0"));
        approvedPastLeave.setReason("Geçmiş izin");
        approvedPastLeave.setWorkflowNextApproverRole("NONE");
        approvedPastLeave = leaveRequestRepository.save(approvedPastLeave);
        
        // Bekleyen izin (aynı departman - gösterilmemeli)
        LeaveRequest pendingLeave = new LeaveRequest();
        pendingLeave.setEmployee(teamMember);
        pendingLeave.setLeaveType(annualLeaveType);
        pendingLeave.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        pendingLeave.setStartDateTime(futureStart);
        pendingLeave.setEndDateTime(futureEnd);
        pendingLeave.setDurationHours(new BigDecimal("16.0"));
        pendingLeave.setReason("Bekleyen izin");
        pendingLeave.setWorkflowNextApproverRole("HR");
        pendingLeave = leaveRequestRepository.save(pendingLeave);
        
        // Farklı departmandan onaylanmış izin (gösterilmemeli)
        LeaveRequest otherDeptLeave = new LeaveRequest();
        otherDeptLeave.setEmployee(otherDeptEmployee);
        otherDeptLeave.setLeaveType(annualLeaveType);
        otherDeptLeave.setRequestStatus(RequestStatus.APPROVED);
        otherDeptLeave.setStartDateTime(futureStart);
        otherDeptLeave.setEndDateTime(futureEnd);
        otherDeptLeave.setDurationHours(new BigDecimal("16.0"));
        otherDeptLeave.setReason("Farklı departman izni");
        otherDeptLeave.setWorkflowNextApproverRole("NONE");
        otherDeptLeave = leaveRequestRepository.save(otherDeptLeave);
        
        // Act & Assert
        mockMvc.perform(get("/api/leaves/team-calendar")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)) // Sadece gelecek ve devam eden onaylanmış izinler
                .andExpect(jsonPath("$[?(@.employeeFullName == 'Team Member')]").exists())
                .andExpect(jsonPath("$[?(@.departmentName == 'Test Department')]").exists())
                .andExpect(jsonPath("$[?(@.leaveTypeName == 'Yıllık İzin')]").exists())
                .andExpect(jsonPath("$[?(@.employeeFullName == 'Other Dept')]").doesNotExist()) // Farklı departman gösterilmemeli
                .andExpect(jsonPath("$[?(@.status == 'PENDING_APPROVAL')]").doesNotExist()); // Bekleyen izinler gösterilmemeli
    }

    @Test
    @DisplayName("GET /api/leaves/team-calendar - Departmanda onaylanmış izin yoksa boş liste döndürmeli")
    void getTeamCalendar_NoApprovedLeaves_ShouldReturnEmptyList() throws Exception {
        // Arrange: Mevcut izinleri temizle (setUp'da zaten temizleniyor ama emin olmak için)
        leaveRequestRepository.deleteAll();
        
        // Act & Assert
        mockMvc.perform(get("/api/leaves/team-calendar")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/leaves/team-calendar - Unauthorized kullanıcı erişememeli")
    void getTeamCalendar_Unauthorized_ShouldReturn401() throws Exception {
        // Token olmadan istek yapıldığında Spring Security 403 (Forbidden) dönebilir
        // Bu durumda hem 401 hem de 403 kabul edilebilir
        mockMvc.perform(get("/api/leaves/team-calendar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError()); // 401 veya 403
    }

    @Test
    @DisplayName("GET /api/leaves/team-calendar - DTO alanları doğru map edilmeli")
    void getTeamCalendar_DTOFields_ShouldBeMappedCorrectly() throws Exception {
        // Arrange: Onaylanmış bir izin oluştur
        Employee teamMember = new Employee();
        teamMember.setFirstName("Alice");
        teamMember.setLastName("Johnson");
        teamMember.setEmail("alice.johnson@example.com");
        teamMember.setJobTitle("Senior Developer");
        teamMember.setBirthDate(LocalDate.of(1988, 6, 10));
        teamMember.setHireDate(LocalDate.now().minusYears(4));
        teamMember.setDailyWorkHours(new BigDecimal("8.0"));
        teamMember.setDepartment(testEmployee.getDepartment());
        teamMember.setIsActive(true);
        teamMember = employeeRepository.save(teamMember);
        
        // Gelecekteki bir tarih kullan (sorgu endDateTime >= şimdiki zaman kontrolü yapıyor)
        LocalDateTime startDate = LocalDateTime.now().plusDays(5).withHour(9).withMinute(0);
        LocalDateTime endDate = LocalDateTime.now().plusDays(5).withHour(17).withMinute(0);
        BigDecimal duration = new BigDecimal("8.0");
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(teamMember);
        leaveRequest.setLeaveType(excuseLeaveType);
        leaveRequest.setRequestStatus(RequestStatus.APPROVED);
        leaveRequest.setStartDateTime(startDate);
        leaveRequest.setEndDateTime(endDate);
        leaveRequest.setDurationHours(duration);
        leaveRequest.setReason("Test izin");
        leaveRequest.setWorkflowNextApproverRole("NONE");
        leaveRequestRepository.save(leaveRequest);
        
        // Act & Assert
        mockMvc.perform(get("/api/leaves/team-calendar")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeFullName").value("Alice Johnson"))
                .andExpect(jsonPath("$[0].departmentName").value("Test Department"))
                .andExpect(jsonPath("$[0].leaveTypeName").value("Mazeret İzni (Saatlik)"))
                .andExpect(jsonPath("$[0].totalHours").value(8.0))
                .andExpect(jsonPath("$[0].startDate").exists())
                .andExpect(jsonPath("$[0].endDate").exists());
    }

    // ========== TATİL HESAPLAMA ENTEGRASYON TESTLERİ ==========

    @Test
    @DisplayName("POST /api/leaves - Hafta sonu içeren izin talebi - hafta sonları düşülmeli")
    void createLeaveRequest_WithWeekend_ShouldExcludeWeekends() throws Exception {
        // Given: Cuma'dan Pazartesi'ye izin (Cumartesi-Pazar hafta sonu)
        // 2026-01-09 Cuma, 2026-01-12 Pazartesi
        LocalDate friday = LocalDate.of(2026, 1, 9); // Cuma
        LocalDate monday = LocalDate.of(2026, 1, 12); // Pazartesi
        LocalDateTime startDate = friday.atTime(9, 0);
        LocalDateTime endDate = monday.atTime(17, 0);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Hafta sonu testi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String response = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        
        // Beklenen: Cuma (8 saat) + Pazartesi (8 saat) = 16 saat
        // Cumartesi ve Pazar düşülmeli
        BigDecimal durationHours = new BigDecimal(responseMap.get("duration").toString());
        assertEquals(0, durationHours.compareTo(new BigDecimal("16.0")), 
                "Hafta sonu düşülmeli: Beklenen 16 saat, Gerçek " + durationHours + " saat");
    }

    @Test
    @DisplayName("POST /api/leaves - Resmi tatil içeren izin talebi - tam gün tatiller düşülmeli")
    void createLeaveRequest_WithPublicHoliday_ShouldExcludeHolidays() throws Exception {
        // Given: Resmi tatil günü oluştur
        LocalDate holidayDate = LocalDate.of(2026, 1, 14); // Çarşamba
        PublicHoliday holiday = new PublicHoliday();
        holiday.setDate(holidayDate);
        holiday.setName("Test Tatili");
        holiday.setHalfDay(false); // Tam gün tatil
        publicHolidayRepository.save(holiday);

        // Salı'dan Perşembe'ye izin (Çarşamba resmi tatil)
        LocalDate tuesday = LocalDate.of(2026, 1, 13); // Salı
        LocalDate thursday = LocalDate.of(2026, 1, 15); // Perşembe
        LocalDateTime startDate = tuesday.atTime(9, 0);
        LocalDateTime endDate = thursday.atTime(17, 0);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Tatil testi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String response = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        
        // Beklenen: Salı (8 saat) + Perşembe (8 saat) = 16 saat
        // Çarşamba (resmi tatil) düşülmeli
        BigDecimal durationHours = new BigDecimal(responseMap.get("duration").toString());
        assertEquals(0, durationHours.compareTo(new BigDecimal("16.0")), 
                "Resmi tatil düşülmeli: Beklenen 16 saat, Gerçek " + durationHours + " saat");
    }

    @Test
    @DisplayName("POST /api/leaves - Yarım gün tatil (arife) içeren izin talebi - yarım gün sayılmalı")
    void createLeaveRequest_WithHalfDayHoliday_ShouldCountHalfDay() throws Exception {
        // Given: Yarım gün tatil (arife) oluştur
        LocalDate arifeDate = LocalDate.of(2026, 1, 14); // Çarşamba
        PublicHoliday arife = new PublicHoliday();
        arife.setDate(arifeDate);
        arife.setName("Arife Günü");
        arife.setHalfDay(true); // Yarım gün tatil
        publicHolidayRepository.save(arife);

        // Çarşamba günü izin (yarım gün tatil)
        LocalDateTime startDate = arifeDate.atTime(9, 0);
        LocalDateTime endDate = arifeDate.atTime(17, 0);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Arife testi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String response = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        
        // Beklenen: Yarım gün tatil = 0.5 * 8 saat = 4 saat
        BigDecimal durationHours = new BigDecimal(responseMap.get("duration").toString());
        assertEquals(0, durationHours.compareTo(new BigDecimal("4.0")), 
                "Yarım gün tatil yarım gün sayılmalı: Beklenen 4 saat, Gerçek " + durationHours + " saat");
    }

    @Test
    @DisplayName("POST /api/leaves - Karmaşık senaryo: hafta sonu + tatil + normal günler")
    void createLeaveRequest_ComplexScenario_WeekendAndHoliday_ShouldCalculateCorrectly() throws Exception {
        // Given: Perşembe'den Salı'ya izin
        // Perşembe (normal), Cuma (normal), Cumartesi (hafta sonu), Pazar (hafta sonu), 
        // Pazartesi (normal), Salı (resmi tatil)
        LocalDate thursday = LocalDate.of(2026, 1, 8); // Perşembe
        LocalDate tuesday = LocalDate.of(2026, 1, 13); // Salı

        // Salı günü resmi tatil oluştur
        PublicHoliday holiday = new PublicHoliday();
        holiday.setDate(tuesday);
        holiday.setName("Test Tatili");
        holiday.setHalfDay(false); // Tam gün tatil
        publicHolidayRepository.save(holiday);

        LocalDateTime startDate = thursday.atTime(9, 0);
        LocalDateTime endDate = tuesday.atTime(17, 0);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Karmaşık senaryo testi"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        String response = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        
        // Beklenen: Perşembe (8) + Cuma (8) + Cumartesi (0) + Pazar (0) + Pazartesi (8) + Salı (0 - tatil) = 24 saat
        BigDecimal durationHours = new BigDecimal(responseMap.get("duration").toString());
        assertEquals(0, durationHours.compareTo(new BigDecimal("24.0")), 
                "Karmaşık senaryo: Beklenen 24 saat (Perşembe+Cuma+Pazartesi), Gerçek " + durationHours + " saat");
    }

    @Test
    @DisplayName("POST /api/leaves - Sadece resmi tatil gününde izin talebi - 0 saat olmalı")
    void createLeaveRequest_OnlyHoliday_ShouldReturnZeroHours() throws Exception {
        // Given: Sadece resmi tatil gününde izin
        LocalDate holidayDate = LocalDate.of(2026, 1, 14); // Çarşamba
        PublicHoliday holiday = new PublicHoliday();
        holiday.setDate(holidayDate);
        holiday.setName("Test Tatili");
        holiday.setHalfDay(false); // Tam gün tatil
        publicHolidayRepository.save(holiday);

        LocalDateTime startDate = holidayDate.atTime(9, 0);
        LocalDateTime endDate = holidayDate.atTime(17, 0);

        String requestBody = String.format("""
                {
                  "leaveTypeId": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "reason": "Sadece tatil günü"
                }
                """, annualLeaveType.getId(), startDate, endDate);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Hesaplanabilir süre bulunamadı")));
    }

    // ========== MANAGER DASHBOARD TESTLERİ ==========

    @Test
    @DisplayName("GET /api/leaves/manager/dashboard - Manager sadece kendi departmanındaki ve workflow sırası MANAGER olan talepleri görür")
    void getManagerDashboard_Manager_ShouldSeeOnlyOwnDepartmentAndWorkflowRole() throws Exception {
        Department otherDept = new Department();
        otherDept.setName("Other Dept");
        otherDept.setIsActive(true);
        otherDept = departmentRepository.save(otherDept);

        Employee deptEmployee = new Employee();
        deptEmployee.setFirstName("Dept");
        deptEmployee.setLastName("User");
        deptEmployee.setEmail("dept.user@example.com");
        deptEmployee.setJobTitle("Dev");
        deptEmployee.setBirthDate(LocalDate.of(1995, 1, 1));
        deptEmployee.setHireDate(LocalDate.now().minusYears(2));
        deptEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        deptEmployee.setDepartment(testEmployee.getDepartment());
        deptEmployee.setIsActive(true);
        deptEmployee = employeeRepository.save(deptEmployee);

        Employee otherDeptEmployee = new Employee();
        otherDeptEmployee.setFirstName("Other");
        otherDeptEmployee.setLastName("User");
        otherDeptEmployee.setEmail("other.user@example.com");
        otherDeptEmployee.setJobTitle("QA");
        otherDeptEmployee.setBirthDate(LocalDate.of(1994, 1, 1));
        otherDeptEmployee.setHireDate(LocalDate.now().minusYears(2));
        otherDeptEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        otherDeptEmployee.setDepartment(otherDept);
        otherDeptEmployee.setIsActive(true);
        otherDeptEmployee = employeeRepository.save(otherDeptEmployee);

        LeaveRequest deptLeave = new LeaveRequest();
        deptLeave.setEmployee(deptEmployee);
        deptLeave.setLeaveType(annualLeaveType);
        deptLeave.setStartDateTime(LocalDateTime.now().plusDays(2));
        deptLeave.setEndDateTime(LocalDateTime.now().plusDays(3));
        deptLeave.setDurationHours(new BigDecimal("8"));
        deptLeave.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        deptLeave.setWorkflowNextApproverRole("MANAGER");
        deptLeave.setReason("Dept leave");
        deptLeave = leaveRequestRepository.save(deptLeave);

        LeaveApprovalHistory deptHistory = new LeaveApprovalHistory();
        deptHistory.setLeaveRequest(deptLeave);
        deptHistory.setApprover(hrUser);
        deptHistory.setAction(RequestStatus.APPROVED_HR);
        deptHistory.setComments("HR onayladı");
        deptHistory.setCreatedAt(LocalDateTime.now().minusHours(1));
        deptLeave.getApprovalHistories().add(deptHistory);
        leaveApprovalHistoryRepository.save(deptHistory);

        LeaveRequest otherDeptLeave = new LeaveRequest();
        otherDeptLeave.setEmployee(otherDeptEmployee);
        otherDeptLeave.setLeaveType(annualLeaveType);
        otherDeptLeave.setStartDateTime(LocalDateTime.now().plusDays(4));
        otherDeptLeave.setEndDateTime(LocalDateTime.now().plusDays(5));
        otherDeptLeave.setDurationHours(new BigDecimal("8"));
        otherDeptLeave.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        otherDeptLeave.setWorkflowNextApproverRole("MANAGER");
        otherDeptLeave.setReason("Other dept leave");
        leaveRequestRepository.save(otherDeptLeave);

        mockMvc.perform(get("/api/leaves/manager/dashboard")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].leaveRequestId").value(deptLeave.getId()))
                .andExpect(jsonPath("$[0].approvalHistory", hasSize(1)))
                .andExpect(jsonPath("$[0].workflowNextApproverRole").value("MANAGER"));
    }

    @Test
    @DisplayName("GET /api/leaves/manager/dashboard - HR yalnızca workflow sırası HR olan talepleri (şirket geneli) görür")
    void getManagerDashboard_HR_ShouldSeeAllPendingForWorkflowRole() throws Exception {
        Employee employee1 = new Employee();
        employee1.setFirstName("Emp");
        employee1.setLastName("One");
        employee1.setEmail("emp.one@example.com");
        employee1.setJobTitle("Dev");
        employee1.setBirthDate(LocalDate.of(1992, 1, 1));
        employee1.setHireDate(LocalDate.now().minusYears(3));
        employee1.setDailyWorkHours(new BigDecimal("8.0"));
        employee1.setDepartment(testEmployee.getDepartment());
        employee1.setIsActive(true);
        employee1 = employeeRepository.save(employee1);

        Employee employee2 = new Employee();
        employee2.setFirstName("Emp");
        employee2.setLastName("Two");
        employee2.setEmail("emp.two@example.com");
        employee2.setJobTitle("Dev");
        employee2.setBirthDate(LocalDate.of(1993, 1, 1));
        employee2.setHireDate(LocalDate.now().minusYears(4));
        employee2.setDailyWorkHours(new BigDecimal("8.0"));
        employee2.setDepartment(testEmployee.getDepartment());
        employee2.setIsActive(true);
        employee2 = employeeRepository.save(employee2);

        LeaveRequest lr1 = new LeaveRequest();
        lr1.setEmployee(employee1);
        lr1.setLeaveType(annualLeaveType);
        lr1.setStartDateTime(LocalDateTime.now().plusDays(2));
        lr1.setEndDateTime(LocalDateTime.now().plusDays(4));
        lr1.setDurationHours(new BigDecimal("16"));
        lr1.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        lr1.setWorkflowNextApproverRole("HR");
        lr1.setReason("HR list leave 1");
        lr1 = leaveRequestRepository.save(lr1);

        LeaveApprovalHistory lr1History = new LeaveApprovalHistory();
        lr1History.setLeaveRequest(lr1);
        lr1History.setApprover(hrUser);
        lr1History.setAction(RequestStatus.PENDING_APPROVAL);
        lr1History.setComments("İlk kayıt");
        lr1History.setCreatedAt(LocalDateTime.now().minusHours(2));
        lr1.getApprovalHistories().add(lr1History);
        leaveApprovalHistoryRepository.save(lr1History);

        LeaveRequest lr2 = new LeaveRequest();
        lr2.setEmployee(employee2);
        lr2.setLeaveType(excuseLeaveType);
        lr2.setStartDateTime(LocalDateTime.now().plusDays(5));
        lr2.setEndDateTime(LocalDateTime.now().plusDays(5).plusHours(2));
        lr2.setDurationHours(new BigDecimal("2"));
        lr2.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        lr2.setWorkflowNextApproverRole("HR");
        lr2.setReason("HR list leave 2");
        lr2 = leaveRequestRepository.save(lr2);

        LeaveApprovalHistory lr2History = new LeaveApprovalHistory();
        lr2History.setLeaveRequest(lr2);
        lr2History.setApprover(hrUser);
        lr2History.setAction(RequestStatus.PENDING_APPROVAL);
        lr2History.setComments("İlk kayıt");
        lr2History.setCreatedAt(LocalDateTime.now().minusHours(1));
        lr2.getApprovalHistories().add(lr2History);
        leaveApprovalHistoryRepository.save(lr2History);

        // Reddedilmiş kayıt HR listesinde görünmemeli
        LeaveRequest lrRejected = new LeaveRequest();
        lrRejected.setEmployee(employee1);
        lrRejected.setLeaveType(annualLeaveType);
        lrRejected.setStartDateTime(LocalDateTime.now().plusDays(7));
        lrRejected.setEndDateTime(LocalDateTime.now().plusDays(8));
        lrRejected.setDurationHours(new BigDecimal("8"));
        lrRejected.setRequestStatus(RequestStatus.REJECTED);
        lrRejected.setWorkflowNextApproverRole("HR"); // bekleyen rol olsa bile statü reddedilmiş
        lrRejected.setReason("Red testi");
        leaveRequestRepository.save(lrRejected);

        // HR sırası dışında (MANAGER) olan bir kayıt HR listesinde görünmemeli
        LeaveRequest lr3 = new LeaveRequest();
        lr3.setEmployee(employee2);
        lr3.setLeaveType(annualLeaveType);
        lr3.setStartDateTime(LocalDateTime.now().plusDays(6));
        lr3.setEndDateTime(LocalDateTime.now().plusDays(7));
        lr3.setDurationHours(new BigDecimal("8"));
        lr3.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        lr3.setWorkflowNextApproverRole("MANAGER");
        lr3.setReason("Manager sırası, HR görmemeli");
        leaveRequestRepository.save(lr3);

        mockMvc.perform(get("/api/leaves/manager/dashboard")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].approvalHistory", hasSize(1)))
                .andExpect(jsonPath("$[1].approvalHistory", hasSize(1)))
                .andExpect(jsonPath("$[*].workflowNextApproverRole", everyItem(is("HR"))));
    }

    @Test
    @DisplayName("GET /api/leaves/company-current - HR tüm şirkette şu an izinde olanları görür")
    void getCompanyCurrentLeaves_HR_ShouldReturnCompanyWideApproved() throws Exception {
        Department otherDept = new Department();
        otherDept.setName("Other Dept");
        otherDept.setIsActive(true);
        otherDept = departmentRepository.save(otherDept);

        Employee emp1 = new Employee();
        emp1.setFirstName("Now");
        emp1.setLastName("InLeave");
        emp1.setEmail("now.inleave@example.com");
        emp1.setJobTitle("Dev");
        emp1.setBirthDate(LocalDate.of(1990, 1, 1));
        emp1.setHireDate(LocalDate.now().minusYears(3));
        emp1.setDailyWorkHours(new BigDecimal("8.0"));
        emp1.setDepartment(testEmployee.getDepartment());
        emp1.setIsActive(true);
        emp1 = employeeRepository.save(emp1);

        Employee emp2 = new Employee();
        emp2.setFirstName("Other");
        emp2.setLastName("DeptUser");
        emp2.setEmail("other.dept@example.com");
        emp2.setJobTitle("QA");
        emp2.setBirthDate(LocalDate.of(1989, 1, 1));
        emp2.setHireDate(LocalDate.now().minusYears(4));
        emp2.setDailyWorkHours(new BigDecimal("8.0"));
        emp2.setDepartment(otherDept);
        emp2.setIsActive(true);
        emp2 = employeeRepository.save(emp2);

        LocalDateTime now = LocalDateTime.now();

        LeaveRequest lr1 = new LeaveRequest();
        lr1.setEmployee(emp1);
        lr1.setLeaveType(annualLeaveType);
        lr1.setStartDateTime(now.minusHours(1));
        lr1.setEndDateTime(now.plusHours(5));
        lr1.setDurationHours(new BigDecimal("8"));
        lr1.setRequestStatus(RequestStatus.APPROVED);
        lr1.setWorkflowNextApproverRole("");
        lr1.setReason("Company current 1");
        leaveRequestRepository.save(lr1);

        LeaveRequest lr2 = new LeaveRequest();
        lr2.setEmployee(emp2);
        lr2.setLeaveType(excuseLeaveType);
        lr2.setStartDateTime(now.minusHours(2));
        lr2.setEndDateTime(now.plusHours(1));
        lr2.setDurationHours(new BigDecimal("3"));
        lr2.setRequestStatus(RequestStatus.APPROVED);
        lr2.setWorkflowNextApproverRole("");
        lr2.setReason("Company current 2");
        leaveRequestRepository.save(lr2);

        // Gelecekteki izin görünmemeli
        LeaveRequest future = new LeaveRequest();
        future.setEmployee(emp1);
        future.setLeaveType(annualLeaveType);
        future.setStartDateTime(now.plusDays(2));
        future.setEndDateTime(now.plusDays(3));
        future.setDurationHours(new BigDecimal("8"));
        future.setRequestStatus(RequestStatus.APPROVED);
        future.setWorkflowNextApproverRole("");
        future.setReason("Future leave");
        leaveRequestRepository.save(future);

        mockMvc.perform(get("/api/leaves/company-current")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].employeeFullName", containsInAnyOrder(
                        "Now InLeave", "Other DeptUser"
                )));
    }

    @Test
    @DisplayName("GET /api/leaves/manager/dashboard - CEO şirket genelindeki workflow sırası CEO olan talepleri ve tarihçelerini görebilir")
    void getManagerDashboard_CEO_ShouldSeeAllPendingForWorkflowRole() throws Exception {
        Employee employee1 = new Employee();
        employee1.setFirstName("Ceo");
        employee1.setLastName("One");
        employee1.setEmail("ceo.one@example.com");
        employee1.setJobTitle("Dev");
        employee1.setBirthDate(LocalDate.of(1991, 1, 1));
        employee1.setHireDate(LocalDate.now().minusYears(5));
        employee1.setDailyWorkHours(new BigDecimal("8.0"));
        employee1.setDepartment(testEmployee.getDepartment());
        employee1.setIsActive(true);
        employee1 = employeeRepository.save(employee1);

        Employee employee2 = new Employee();
        employee2.setFirstName("Ceo");
        employee2.setLastName("Two");
        employee2.setEmail("ceo.two@example.com");
        employee2.setJobTitle("Dev");
        employee2.setBirthDate(LocalDate.of(1990, 6, 1));
        employee2.setHireDate(LocalDate.now().minusYears(6));
        employee2.setDailyWorkHours(new BigDecimal("8.0"));
        employee2.setDepartment(testEmployee.getDepartment());
        employee2.setIsActive(true);
        employee2 = employeeRepository.save(employee2);

        LeaveRequest lr1 = new LeaveRequest();
        lr1.setEmployee(employee1);
        lr1.setLeaveType(annualLeaveType);
        lr1.setStartDateTime(LocalDateTime.now().plusDays(2));
        lr1.setEndDateTime(LocalDateTime.now().plusDays(3));
        lr1.setDurationHours(new BigDecimal("8"));
        lr1.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        lr1.setWorkflowNextApproverRole("CEO");
        lr1.setReason("CEO list leave 1");
        lr1 = leaveRequestRepository.save(lr1);

        LeaveApprovalHistory lr1History = new LeaveApprovalHistory();
        lr1History.setLeaveRequest(lr1);
        lr1History.setApprover(hrUser);
        lr1History.setAction(RequestStatus.APPROVED_HR);
        lr1History.setComments("HR onayladı");
        lr1History.setCreatedAt(LocalDateTime.now().minusHours(2));
        lr1.getApprovalHistories().add(lr1History);
        leaveApprovalHistoryRepository.save(lr1History);

        LeaveRequest lr2 = new LeaveRequest();
        lr2.setEmployee(employee2);
        lr2.setLeaveType(annualLeaveType);
        lr2.setStartDateTime(LocalDateTime.now().plusDays(4));
        lr2.setEndDateTime(LocalDateTime.now().plusDays(5));
        lr2.setDurationHours(new BigDecimal("8"));
        lr2.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        lr2.setWorkflowNextApproverRole("CEO");
        lr2.setReason("CEO list leave 2");
        lr2 = leaveRequestRepository.save(lr2);

        LeaveApprovalHistory lr2History = new LeaveApprovalHistory();
        lr2History.setLeaveRequest(lr2);
        lr2History.setApprover(hrUser);
        lr2History.setAction(RequestStatus.APPROVED_HR);
        lr2History.setComments("HR onayladı");
        lr2History.setCreatedAt(LocalDateTime.now().minusHours(1));
        lr2.getApprovalHistories().add(lr2History);
        leaveApprovalHistoryRepository.save(lr2History);

        mockMvc.perform(get("/api/leaves/manager/dashboard")
                        .header("Authorization", "Bearer " + ceoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].approvalHistory", hasSize(1)))
                .andExpect(jsonPath("$[1].approvalHistory", hasSize(1)))
                .andExpect(jsonPath("$[*].workflowNextApproverRole", everyItem(is("CEO"))));
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

