package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.AccountingReportRequest;
import com.cozumtr.leave_management_system.dto.response.AccountingLeaveReportResponse;
import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.ReportType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.LeaveAttachmentRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountingReportServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveAttachmentRepository leaveAttachmentRepository;

    @InjectMocks
    private AccountingReportService accountingReportService;

    private AccountingReportRequest baseRequest;
    private LeaveRequest unpaidRequest;
    private LeaveRequest docRequiredRequest;

    @BeforeEach
    void setUp() {
        baseRequest = new AccountingReportRequest();
        baseRequest.setStartDate(LocalDateTime.now().minusDays(10));
        baseRequest.setEndDate(LocalDateTime.now().plusDays(10));
        baseRequest.setType(ReportType.ALL);

        unpaidRequest = buildRequest(false, false, RequestStatus.APPROVED, 1L, "Ücretsiz İzin");
        docRequiredRequest = buildRequest(true, true, RequestStatus.APPROVED_MANAGER, 2L, "Hastalık İzni");
    }

    @Test
    @DisplayName("getReport - ALL türü tüm onaylı kayıtları döner")
    void getReport_AllTypes_ReturnsRows() {
        when(leaveRequestRepository.findForAccountingReport(anyList(), any(), any(), eq(false), eq(false), isNull(), isNull()))
                .thenReturn(List.of(unpaidRequest, docRequiredRequest));
        when(leaveAttachmentRepository.findByLeaveRequestId(1L)).thenReturn(List.of());
        when(leaveAttachmentRepository.findByLeaveRequestId(2L)).thenReturn(List.of(new com.cozumtr.leave_management_system.entities.LeaveAttachment()));

        AccountingLeaveReportResponse response = accountingReportService.getReport(baseRequest);

        assertNotNull(response.getRows());
        assertEquals(2, response.getRows().size());
        assertEquals("Ücretsiz İzin", response.getRows().get(0).getLeaveTypeName());
        assertEquals(0, response.getRows().get(0).getAttachmentCount());
        assertEquals(1, response.getRows().get(1).getAttachmentCount());
    }

    @Test
    @DisplayName("getReport - UNPAID filtresi yalnız isPaid=false kayıtları getirir")
    void getReport_UnpaidFilter() {
        baseRequest.setType(ReportType.UNPAID);
        when(leaveRequestRepository.findForAccountingReport(anyList(), any(), any(), eq(true), eq(false), isNull(), isNull()))
                .thenReturn(List.of(unpaidRequest));
        when(leaveAttachmentRepository.findByLeaveRequestId(anyLong())).thenReturn(List.of());

        AccountingLeaveReportResponse response = accountingReportService.getReport(baseRequest);

        assertEquals(1, response.getRows().size());
        assertFalse(response.getRows().get(0).isPaid());
    }

    @Test
    @DisplayName("getReport - DOCUMENT_REQUIRED filtresi yalnız documentRequired=true kayıtları getirir")
    void getReport_DocumentRequiredFilter() {
        baseRequest.setType(ReportType.DOCUMENT_REQUIRED);
        when(leaveRequestRepository.findForAccountingReport(anyList(), any(), any(), eq(false), eq(true), isNull(), isNull()))
                .thenReturn(List.of(docRequiredRequest));
        when(leaveAttachmentRepository.findByLeaveRequestId(anyLong())).thenReturn(List.of(new com.cozumtr.leave_management_system.entities.LeaveAttachment()));

        AccountingLeaveReportResponse response = accountingReportService.getReport(baseRequest);

        assertEquals(1, response.getRows().size());
        assertTrue(response.getRows().get(0).isDocumentRequired());
    }

    @Test
    @DisplayName("getReport - Tarih doğrulaması: end < start ise BusinessException")
    void getReport_InvalidDates_ShouldThrow() {
        baseRequest.setEndDate(baseRequest.getStartDate().minusDays(1));
        assertThrows(BusinessException.class, () -> accountingReportService.getReport(baseRequest));
    }

    private LeaveRequest buildRequest(boolean paid, boolean documentRequired, RequestStatus status, Long id, String typeName) {
        LeaveType lt = new LeaveType();
        lt.setId(id);
        lt.setName(typeName);
        lt.setPaid(paid);
        lt.setDocumentRequired(documentRequired);
        lt.setDeductsFromAnnual(false);
        lt.setRequestUnit(RequestUnit.DAY);

        Department d = new Department();
        d.setId(10L);
        d.setName("Depo");

        Employee e = new Employee();
        e.setId(20L);
        e.setFirstName("Test");
        e.setLastName("User");
        e.setDepartment(d);

        LeaveRequest lr = new LeaveRequest();
        lr.setId(id);
        lr.setEmployee(e);
        lr.setLeaveType(lt);
        lr.setRequestStatus(status);
        lr.setStartDateTime(LocalDateTime.now().minusDays(2));
        lr.setEndDateTime(LocalDateTime.now().minusDays(1));
        lr.setDurationHours(BigDecimal.TEN);
        return lr;
    }
}

