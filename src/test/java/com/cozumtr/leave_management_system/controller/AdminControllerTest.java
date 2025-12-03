package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.LeaveTypeRequestDto;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.service.MetadataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@TestPropertySource(properties = {
        "ADMIN_PASS=test1234",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver"
})
@AutoConfigureMockMvc(addFilters = false) // Güvenlik duvarını indir
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // Java objesini JSON'a çevirmek için

    @MockitoBean
    private MetadataService metadataService;

    @Test
    public void createLeaveType_ShouldReturnOk() throws Exception {
        // 1. HAZIRLIK
        LeaveTypeRequestDto dto = new LeaveTypeRequestDto();
        dto.setName("Mazeret İzni");
        dto.setIsPaid(true);
        dto.setDeductsFromAnnual(false);
        dto.setWorkflowDefinition("ROLE_HR");
        dto.setRequestUnit("HOUR");

        // Servis çağrılınca boş bir LeaveType dönse yeterli (Controller hatasız çalışsın)
        when(metadataService.createLeaveType(any(LeaveTypeRequestDto.class))).thenReturn(new LeaveType());

        // 2. EYLEM & KONTROL
        mockMvc.perform(post("/api/v1/admin/leave-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))) // DTO'yu JSON yap
                .andExpect(status().isOk());
    }

    @Test
    public void deleteLeaveType_ShouldReturnOk() throws Exception {
        // Silme işlemi void olduğu için return değerini mocklamaya gerek yok

        mockMvc.perform(delete("/api/v1/admin/leave-types/{id}", 1L))
                .andExpect(status().isOk());
    }
}