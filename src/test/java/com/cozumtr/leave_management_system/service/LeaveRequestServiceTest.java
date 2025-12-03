package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.LeaveTimelineDto;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveApprovalHistory;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.LeaveApprovalHistoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LeaveRequestServiceTest {

    @Mock
    private LeaveApprovalHistoryRepository leaveApprovalHistoryRepository;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    @Test
    public void getRequestTimeline_ShouldReturnDtoList_WhenHistoryExists() {
        // 1. HAZIRLIK (Arrange)
        Long requestId = 1L;

        // Sahte bir onaylayan personel (Employee) oluşturalım
        Employee approver = new Employee();
        approver.setFirstName("Ali");
        approver.setLastName("Yılmaz");
        approver.setJobTitle("Müdür");

        // Sahte bir tarihçe kaydı (History) oluşturalım
        LeaveApprovalHistory history = new LeaveApprovalHistory();
        history.setId(100L);
        history.setApprover(approver);
        history.setAction(RequestStatus.APPROVED);
        history.setComments("Uygundur");
        // BaseEntity'den gelen createdAt alanını doldurmamız gerekebilir
        // Ancak BaseEntity alanları testte null gelebilir, servis kodumuzda null check yoksa dikkat etmeliyiz.
        // Şimdilik null kabul ediyoruz veya BaseEntity'ye setter eklediğini varsayıyoruz.

        // Repository taklidi: "Bu ID ile çağrılırsan, yukarıdaki listeyi dön"
        when(leaveApprovalHistoryRepository.findByLeaveRequestIdOrderByCreatedAtAsc(requestId))
                .thenReturn(Collections.singletonList(history));

        // 2. EYLEM (Act)
        List<LeaveTimelineDto> result = leaveRequestService.getRequestTimeline(requestId);

        // 3. KONTROL (Assert)
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());

        LeaveTimelineDto dto = result.get(0);
        Assertions.assertEquals("Ali Yılmaz", dto.getActorName());
        Assertions.assertEquals("Müdür", dto.getActorJobTitle());
        Assertions.assertEquals("APPROVED", dto.getActionType());
        Assertions.assertEquals("Uygundur", dto.getComments());
    }

    @Test
    public void getRequestTimeline_ShouldReturnSystemUser_WhenApproverIsNull() {
        // 1. HAZIRLIK (Approver'ın null olduğu durum - Edge Case)
        Long requestId = 2L;

        LeaveApprovalHistory history = new LeaveApprovalHistory();
        history.setApprover(null); // Kritik nokta: Onaylayan kişi yok (Sistem otomatik yapmış olabilir)
        history.setAction(RequestStatus.PENDING_APPROVAL);

        when(leaveApprovalHistoryRepository.findByLeaveRequestIdOrderByCreatedAtAsc(requestId))
                .thenReturn(Collections.singletonList(history));

        // 2. EYLEM
        List<LeaveTimelineDto> result = leaveRequestService.getRequestTimeline(requestId);

        // 3. KONTROL
        Assertions.assertEquals("Sistem / Bilinmeyen", result.get(0).getActorName());
    }
}