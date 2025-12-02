package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApprovalServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveApprovalService leaveApprovalService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    // YARDIMCI METOT: Rol Mocklama (ROLE_MANAGER -> MANAGER)
    private void mockLoginUserRole(String role) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        // Wildcard (?) kullanarak tip güvenliği sorununu aşıyoruz
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_" + role))).when(authentication).getAuthorities();
    }

    @Test
    void approveRequest_ShouldMoveToNextStep_WhenMultiStepWorkflow() {
        // Senaryo: "MANAGER,HR" akışı var. Şu an sıra MANAGER'da.
        // Beklenen: Onaylanınca sıra HR'a geçmeli, statü APPROVED olmamalı.

        Long requestId = 1L;
        LeaveRequest request = new LeaveRequest();
        request.setWorkflowNextApproverRole("MANAGER"); // Sıra Yöneticide

        LeaveType type = new LeaveType();
        type.setWorkflowDefinition("MANAGER,HR"); // Çift aşamalı
        request.setLeaveType(type);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // MANAGER giriş yapıyor
        mockLoginUserRole("MANAGER");

        // EYLEM
        leaveApprovalService.approveRequest(requestId);

        // KONTROL
        assertEquals("HR", request.getWorkflowNextApproverRole()); // Sıra HR'a geçti mi?
        verify(leaveRequestRepository).save(request);
        System.out.println(" Adım Atlama Testi Başarılı (MANAGER -> HR)");
    }

    @Test
    void approveRequest_ShouldFinish_WhenLastStep() {
        // Senaryo: "MANAGER,HR" akışı var. Şu an sıra HR'da (Son adım).
        // Beklenen: Onaylanınca statü APPROVED olmalı.

        Long requestId = 2L;
        LeaveRequest request = new LeaveRequest();
        request.setWorkflowNextApproverRole("HR"); // Sıra İK'da

        LeaveType type = new LeaveType();
        type.setWorkflowDefinition("MANAGER,HR");
        request.setLeaveType(type);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // HR giriş yapıyor
        mockLoginUserRole("HR");

        // EYLEM
        leaveApprovalService.approveRequest(requestId);

        // KONTROL
        assertEquals(RequestStatus.APPROVED, request.getRequestStatus()); // Bitti mi?
        assertEquals("NONE", request.getWorkflowNextApproverRole()); // Bekleyen kimse kalmadı mı?
        verify(leaveRequestRepository).save(request);
        System.out.println(" Bitiş Testi Başarılı (HR -> APPROVED)");
    }

    @Test
    void approveRequest_ShouldThrowException_WhenWrongRole() {
        // Senaryo: Sıra MANAGER'da ama HR onaylamaya çalışıyor.
        // Beklenen: Hata fırlatmalı.

        Long requestId = 3L;
        LeaveRequest request = new LeaveRequest();
        request.setWorkflowNextApproverRole("MANAGER"); // Beklenen: MANAGER

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // HR giriş yapıyor (Yanlış kişi)
        mockLoginUserRole("HR");

        // EYLEM & KONTROL
        assertThrows(IllegalStateException.class, () -> {
            leaveApprovalService.approveRequest(requestId);
        });

        System.out.println("Güvenlik Testi Başarılı: Yanlış rol onaylayamadı.");
    }
}