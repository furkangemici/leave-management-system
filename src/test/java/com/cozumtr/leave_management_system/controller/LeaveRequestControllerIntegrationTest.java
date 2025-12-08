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
    private PublicHolidayRepository publicHolidayRepository;

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
        publicHolidayRepository.deleteAll();
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

