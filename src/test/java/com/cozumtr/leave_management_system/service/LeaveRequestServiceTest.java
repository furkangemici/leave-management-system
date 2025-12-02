package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveCalculationService leaveCalculationService;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    // --- TASK 7 TESTİ ---
    @Test
    void createLeaveRequest_ShouldCreateSuccessfully() {
        String email = "test@sirket.com";
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusDays(2);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmail(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        LeaveType leaveType = new LeaveType();
        leaveType.setId(1L);
        leaveType.setName("Yıllık İzin");
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(any(), any(), any(), any())).thenReturn(false);
        when(leaveCalculationService.calculateDuration(any(), any())).thenReturn(new BigDecimal("2.0"));

        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> {
            LeaveRequest req = (LeaveRequest) i.getArguments()[0];
            req.setId(100L);
            return req;
        });

        CreateLeaveRequest request = new CreateLeaveRequest();
        request.setLeaveTypeId(1L);
        request.setStartDate(start);
        request.setEndDate(end);

        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Yıllık İzin", response.getLeaveTypeName());
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
        System.out.println(" İzin Oluşturma Testi Başarılı!");
    }

    // --- TASK 9 TESTİ (İPTAL) ---
    @Test
    void cancelLeaveRequest_ShouldCancel_WhenPendingAndOwner() {
        Long requestId = 55L;
        String email = "ali@sirket.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);

        LeaveRequest request = new LeaveRequest();
        request.setId(requestId);
        request.setRequestStatus(RequestStatus.PENDING_APPROVAL);

        Employee owner = new Employee();
        owner.setEmail(email);
        request.setEmployee(owner);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // EYLEM
        leaveRequestService.cancelLeaveRequest(requestId);

        // KONTROL
        assertEquals(RequestStatus.CANCELLED, request.getRequestStatus());
        verify(leaveRequestRepository).save(request);
        System.out.println(" İzin İptal Testi Başarılı!");
    }
}