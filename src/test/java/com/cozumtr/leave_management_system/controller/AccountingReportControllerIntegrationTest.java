package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.AccountingReportRequest;
import com.cozumtr.leave_management_system.enums.ReportType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.enums.WorkType;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.upload.dir=target/test-uploads"
})
@Transactional
class AccountingReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private LeaveTypeRepository leaveTypeRepository;
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private LeaveType unpaidType;
    private LeaveType docRequiredType;
    private Employee employee;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        leaveTypeRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        Department dep = new Department();
        dep.setName("Finans");
        dep.setIsActive(true);
        dep = departmentRepository.save(dep);

        employee = new Employee();
        employee.setFirstName("Hasan");
        employee.setLastName("Muhasebe");
        employee.setEmail("hasan@company.com");
        employee.setDailyWorkHours(BigDecimal.valueOf(8));
        employee.setDepartment(dep);
        employee.setBirthDate(LocalDate.now().minusYears(30));
        employee.setHireDate(LocalDate.now().minusYears(5));
        employee.setJobTitle("Muhasebe Uzmanı");
        employee.setWorkType(WorkType.FULL_TIME);
        employee.setIsActive(true);
        employee = employeeRepository.save(employee);

        unpaidType = new LeaveType();
        unpaidType.setName("Ücretsiz İzin");
        unpaidType.setPaid(false);
        unpaidType.setDeductsFromAnnual(false);
        unpaidType.setDocumentRequired(false);
        unpaidType.setWorkflowDefinition("HR,MANAGER");
        unpaidType.setRequestUnit(RequestUnit.DAY);
        unpaidType.setIsActive(true);
        unpaidType = leaveTypeRepository.save(unpaidType);

        docRequiredType = new LeaveType();
        docRequiredType.setName("Raporlu İzin");
        docRequiredType.setPaid(false);
        docRequiredType.setDeductsFromAnnual(false);
        docRequiredType.setDocumentRequired(true);
        docRequiredType.setWorkflowDefinition("HR");
        docRequiredType.setRequestUnit(RequestUnit.DAY);
        docRequiredType.setIsActive(true);
        docRequiredType = leaveTypeRepository.save(docRequiredType);

        insertLeave(unpaidType, RequestStatus.APPROVED, LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(3));
        insertLeave(docRequiredType, RequestStatus.APPROVED_MANAGER, LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(2));
    }

    @Test
    @DisplayName("POST /api/reports/accounting/leaves - ACCOUNTING rolü JSON rapor alır")
    @WithMockUser(username = "muhasebe@test.com", roles = {"ACCOUNTING"})
    void getReport_ShouldReturnRows() throws Exception {
        AccountingReportRequest req = new AccountingReportRequest();
        req.setStartDate(LocalDateTime.now().minusDays(10));
        req.setEndDate(LocalDateTime.now());
        req.setType(ReportType.ALL);

        String response = mockMvc.perform(post("/api/reports/accounting/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("Ücretsiz İzin");
    }

    @Test
    @DisplayName("POST /api/reports/accounting/leaves/export - ACCOUNTING rolü Excel indirir")
    @WithMockUser(username = "muhasebe@test.com", roles = {"ACCOUNTING"})
    void exportReport_ShouldReturnExcel() throws Exception {
        AccountingReportRequest req = new AccountingReportRequest();
        req.setStartDate(LocalDateTime.now().minusDays(10));
        req.setEndDate(LocalDateTime.now());
        req.setType(ReportType.DOCUMENT_REQUIRED);

        mockMvc.perform(post("/api/reports/accounting/leaves/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("leave-report.xlsx")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/reports/accounting/leaves - NON-ACCOUNTING erişemez (403)")
    @WithMockUser(username = "emp@test.com", roles = {"EMPLOYEE"})
    void getReport_NonAccounting_ShouldForbid() throws Exception {
        AccountingReportRequest req = new AccountingReportRequest();
        req.setStartDate(LocalDateTime.now().minusDays(10));
        req.setEndDate(LocalDateTime.now());
        req.setType(ReportType.ALL);

        mockMvc.perform(post("/api/reports/accounting/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private void insertLeave(LeaveType type, RequestStatus status, LocalDateTime start, LocalDateTime end) {
        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(employee);
        lr.setLeaveType(type);
        lr.setRequestStatus(status);
        lr.setStartDateTime(start);
        lr.setEndDateTime(end);
        lr.setDurationHours(BigDecimal.valueOf(16));
        lr.setReason("Test reason");
        lr.setWorkflowNextApproverRole("");
        leaveRequestRepository.save(lr);
    }
}


