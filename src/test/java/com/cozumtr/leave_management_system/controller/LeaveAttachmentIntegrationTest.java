package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.upload.dir=target/test-uploads",
        "app.upload.allowed-content-types=application/pdf,image/jpeg,image/png",
        "app.upload.max-file-size=1048576"
})
@Transactional
class LeaveAttachmentIntegrationTest {

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

    private Employee employee;
    private LeaveType documentRequiredLeave;

    @BeforeEach
    void setUp() throws Exception {
        leaveRequestRepository.deleteAll();
        leaveTypeRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        Files.createDirectories(Path.of("target/test-uploads"));

        Department department = new Department();
        department.setName("Test");
        department.setIsActive(true);
        department = departmentRepository.save(department);

        employee = new Employee();
        employee.setFirstName("Test");
        employee.setLastName("User");
        employee.setEmail("emp@test.com");
        employee.setDailyWorkHours(new BigDecimal("8"));
        employee.setDepartment(department);
        employee.setBirthDate(LocalDateTime.now().minusYears(30).toLocalDate());
        employee.setHireDate(LocalDateTime.now().minusYears(1).toLocalDate());
        employee.setJobTitle("Tester");
        employee.setWorkType(com.cozumtr.leave_management_system.enums.WorkType.FULL_TIME);
        employee.setIsActive(true);
        employeeRepository.save(employee);

        documentRequiredLeave = new LeaveType();
        documentRequiredLeave.setName("Raporlu İzin");
        documentRequiredLeave.setPaid(false);
        documentRequiredLeave.setDeductsFromAnnual(false);
        documentRequiredLeave.setDocumentRequired(true);
        documentRequiredLeave.setWorkflowDefinition("HR");
        documentRequiredLeave.setRequestUnit(RequestUnit.DAY);
        documentRequiredLeave.setIsActive(true);
        leaveTypeRepository.save(documentRequiredLeave);
    }

    /**
     * Hafta içi bir gün döndürür (Pazartesi-Cuma).
     */
    private LocalDate getNextWeekday(LocalDate from) {
        LocalDate date = from.plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
    
    /**
     * Belirtilen günden itibaren N gün sonraki hafta içi günü döndürür.
     */
    private LocalDate getNextWeekdayAfterDays(LocalDate from, int days) {
        LocalDate date = from.plusDays(days);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private CreateLeaveRequest buildRequestDto() {
        CreateLeaveRequest dto = new CreateLeaveRequest();
        dto.setLeaveTypeId(documentRequiredLeave.getId());
        LocalDate startDateLocal = getNextWeekday(LocalDate.now());
        LocalDate endDateLocal = getNextWeekdayAfterDays(startDateLocal, 1);
        dto.setStartDate(startDateLocal.atTime(9, 0));
        dto.setEndDate(endDateLocal.atTime(17, 0));
        dto.setReason("Sağlık raporu");
        return dto;
    }

    @Test
    @DisplayName("POST /api/leaves - Belge zorunlu izinde dosya olmadan talep 400 döner")
    @WithMockUser(username = "emp@test.com", roles = {"EMPLOYEE"})
    void createDocumentRequiredWithoutFile_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(buildRequestDto())
        );

        mockMvc.perform(multipart("/api/leaves")
                        .file(requestPart)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/leaves - Belge zorunlu izinde dosyayla talep, ek listeleme ve indirme başarılı")
    @WithMockUser(username = "emp@test.com", roles = {"EMPLOYEE"})
    void createWithFile_ThenListAndDownload_ShouldSucceed() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(buildRequestDto())
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "rapor.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        String createResponse = mockMvc.perform(multipart("/api/leaves")
                        .file(requestPart)
                        .file(filePart)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        long leaveRequestId = created.get("id").asLong();

        String listResponse = mockMvc.perform(get("/api/leaves/{id}/attachments", leaveRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("rapor.pdf"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode list = objectMapper.readTree(listResponse);
        long attachmentId = list.get(0).get("id").asLong();

        mockMvc.perform(get("/api/leaves/attachments/{attachmentId}/download", attachmentId))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentType()).isEqualTo("application/pdf"));
    }

    @Test
    @DisplayName("GET /api/leaves/{id}/attachments - Yüklenen ek listelenmeli")
    @WithMockUser(username = "emp@test.com", roles = {"EMPLOYEE"})
    void listAttachments_ShouldReturnUploadedFile() throws Exception {
        long[] ids = createRequestWithFile();
        long leaveRequestId = ids[0];

        mockMvc.perform(get("/api/leaves/{id}/attachments", leaveRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName").value("rapor.pdf"));
    }

    @Test
    @DisplayName("GET /api/leaves/attachments/{attachmentId}/download - Dosya indirilebilmeli")
    @WithMockUser(username = "emp@test.com", roles = {"EMPLOYEE"})
    void downloadAttachment_ShouldReturnFileContent() throws Exception {
        long[] ids = createRequestWithFile();
        long attachmentId = ids[1];

        mockMvc.perform(get("/api/leaves/attachments/{attachmentId}/download", attachmentId))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentType()).isEqualTo("application/pdf"))
                .andExpect(r -> assertThat(r.getResponse().getContentAsByteArray()).isNotEmpty());
    }

    private long[] createRequestWithFile() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(buildRequestDto())
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "rapor.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        String createResponse = mockMvc.perform(multipart("/api/leaves")
                        .file(requestPart)
                        .file(filePart)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        long leaveRequestId = created.get("id").asLong();

        String listResponse = mockMvc.perform(get("/api/leaves/{id}/attachments", leaveRequestId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode list = objectMapper.readTree(listResponse);
        long attachmentId = list.get(0).get("id").asLong();
        return new long[]{leaveRequestId, attachmentId};
    }
}


