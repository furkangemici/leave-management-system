package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.response.SprintOverlapDto;
import com.cozumtr.leave_management_system.service.LeaveReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
// Konfigürasyon Hatasını Önlemek İçin:
@TestPropertySource(properties = {
        "ADMIN_PASS=test1234",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver"
})
@AutoConfigureMockMvc(addFilters = false) // Güvenliği geç
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveReportService leaveReportService;

    @Test
    public void getSprintOverlapReport_ShouldReturnOk() throws Exception {
        // 1. HAZIRLIK
        SprintOverlapDto dto = SprintOverlapDto.builder()
                .employeeName("Dilara Akınoğlu")
                .overlapHours(40L)
                .build();

        // Servis ne ile çağrılırsa çağrılsın bu listeyi dön
        when(leaveReportService.getSprintOverlapReport(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(dto));

        // 2. EYLEM
        // URL'e parametreleri (Query Params) ekliyoruz
        mockMvc.perform(get("/api/v1/reports/sprint-overlap")
                        .param("start", "2025-06-01T09:00:00")
                        .param("end", "2025-06-15T18:00:00")
                        .contentType(MediaType.APPLICATION_JSON))
                // 3. KONTROL
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeName").value("Dilara Akınoğlu"))
                .andExpect(jsonPath("$[0].overlapHours").value(40));
    }
}