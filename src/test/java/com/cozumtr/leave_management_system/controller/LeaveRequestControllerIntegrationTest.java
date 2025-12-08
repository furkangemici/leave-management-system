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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee testEmployee;
    private User testUser;
    private Employee hrUser;
    private User hrUserEntity;
    private LeaveType annualLeaveType;
    private LeaveType excuseLeaveType;
    private LeaveEntitlement testEntitlement;
    private String employeeToken;
    private String hrToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        leaveRequestRepository.deleteAll();
        leaveEntitlementRepository.deleteAll();
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
        LocalDateTime baseDate = LocalDateTime.now().withHour(9).withMinute(0);
        for (int i = 1; i <= 4; i++) {
            LocalDateTime startDate = baseDate.plusDays(i).withHour(9);
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
        LocalDateTime startDate = baseDate.plusDays(10).withHour(9);
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

