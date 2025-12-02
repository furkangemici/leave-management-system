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

    // Security Context Mockları
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    @BeforeEach
    void setUp() {
        // Her testten önce SecurityContext'i ayarla (Login olmuş gibi)
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createLeaveRequest_ShouldCreateSuccessfully() {
        // 1. HAZIRLIK (GIVEN)
        String email = "test@sirket.com";
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusDays(2);

        // Login Mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);

        // Employee Mock
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmail(email);
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        // LeaveType Mock
        LeaveType leaveType = new LeaveType();
        leaveType.setId(1L);
        leaveType.setName("Yıllık İzin");
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        // Çakışma Yok Mock
        when(leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(any(), any(), any(), any()))
                .thenReturn(false);

        // Hesaplama Motoru Mock (Task 8'i taklit ediyoruz)
        when(leaveCalculationService.calculateDuration(any(), any()))
                .thenReturn(new BigDecimal("2.0"));

        // Kayıt Mock (Kaydedileni geri dön)
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> {
            LeaveRequest req = (LeaveRequest) i.getArguments()[0];
            req.setId(100L); // Sanki veritabanı ID vermiş gibi
            return req;
        });

        // İstek DTO'su
        CreateLeaveRequest request = new CreateLeaveRequest();
        request.setLeaveTypeId(1L);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setReason("Tatil");

        // 2. EYLEM (WHEN)
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

        // 3. KONTROL (THEN)
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Yıllık İzin", response.getLeaveTypeName());
        assertEquals(RequestStatus.PENDING_APPROVAL, response.getStatus()); // Enum ismine dikkat
        assertEquals(new BigDecimal("2.0"), response.getDuration());

        // Veritabanına gerçekten kayıt atıldı mı?
        verify(leaveRequestRepository).save(any(LeaveRequest.class));

        System.out.println(" İzin Talebi Testi Başarılı!");
    }
}