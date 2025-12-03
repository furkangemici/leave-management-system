package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.response.LeaveTimelineDto;
import com.cozumtr.leave_management_system.service.LeaveRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // Eklendi
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource; // Eklendi
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveRequestController.class)
// 1. ÇÖZÜM: Eksik olan değişkeni burada sahte olarak veriyoruz
@TestPropertySource(properties = {
        "ADMIN_PASS=test_sifresi_123",
        "spring.datasource.url=jdbc:h2:mem:testdb", // Gerekirse veritabanı url hatası almamak için
        "spring.datasource.driverClassName=org.h2.Driver"
})
// 2. ÖNERİ: Spring Security login ekranına takılmamak için filtreleri kapatıyoruz
@AutoConfigureMockMvc(addFilters = false)
public class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    @Test
    public void getLeaveTimeline_ShouldReturnOkAndList() throws Exception {
        // 1. HAZIRLIK
        Long requestId = 1L;

        LeaveTimelineDto dto = LeaveTimelineDto.builder()
                .actorName("Ayşe Demir")
                .actionType("APPROVED")
                .build();

        List<LeaveTimelineDto> dtoList = Collections.singletonList(dto);

        // Servis taklidi
        when(leaveRequestService.getRequestTimeline(requestId)).thenReturn(dtoList);

        // 2. EYLEM & 3. KONTROL
        mockMvc.perform(get("/api/leaves/{id}/timeline", requestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actorName").value("Ayşe Demir"))
                .andExpect(jsonPath("$[0].actionType").value("APPROVED"));
    }
}