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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveCalculationService leaveCalculationService;

    // --- TASK 7: İZİN TALEBİ OLUŞTURMA ---
    @Transactional
    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        // 1. Güvenlik: Giriş yapanı bul
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + email));

        // 2. Tarih Kontrolü
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Bitiş tarihi başlangıç tarihinden önce olamaz!");
        }

        // 3. Çakışma Kontrolü
        boolean hasOverlap = leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                employee.getId(),
                request.getStartDate(),
                request.getEndDate(),
                List.of(RequestStatus.REJECTED, RequestStatus.CANCELLED)
        );

        if (hasOverlap) {
            throw new IllegalStateException("Seçilen tarih aralığında zaten mevcut bir izin kaydınız var!");
        }

        // 4. İzin Türü ve Süre Hesaplama
        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Geçersiz İzin Türü ID: " + request.getLeaveTypeId()));

        BigDecimal duration = leaveCalculationService.calculateDuration(
                request.getStartDate().toLocalDate(),
                request.getEndDate().toLocalDate()
        );

        if (duration.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hesaplanabilir iş günü bulunamadı (Tatil veya Haftasonu).");
        }

        // 5. Kayıt
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDateTime(request.getStartDate());
        leaveRequest.setEndDateTime(request.getEndDate());
        leaveRequest.setDurationHours(duration);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        leaveRequest.setWorkflowNextApproverRole("MANAGER");

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        return LeaveRequestResponse.builder()
                .id(savedRequest.getId())
                .leaveTypeName(leaveType.getName())
                .startDate(savedRequest.getStartDateTime())
                .endDate(savedRequest.getEndDateTime())
                .duration(savedRequest.getDurationHours())
                .status(savedRequest.getRequestStatus())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- TASK 9: İZİN İPTALİ  ---
    @Transactional
    public void cancelLeaveRequest(Long id) {
        // 1. Güvenlik: İşlemi yapan kim?
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. İzni bul
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("İzin talebi bulunamadı ID: " + id));

        // 3. Yetki Kontrolü: Başkasının iznini iptal edemezsin
        if (!request.getEmployee().getEmail().equals(currentEmail)) {
            throw new IllegalStateException("Bu işlem için yetkiniz yok! Sadece kendi izinlerinizi iptal edebilirsiniz.");
        }

        // 4. Mantık Kontrolü: Sadece 'Bekleyen' izinler iptal edilebilir
        if (request.getRequestStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Sadece onay bekleyen izin talepleri iptal edilebilir. Şu anki durum: " + request.getRequestStatus());
        }

        // 5. İptal Et (Veritabanından silmiyoruz, durumunu güncelliyoruz -> Soft Delete mantığı)
        request.setRequestStatus(RequestStatus.CANCELLED);
        leaveRequestRepository.save(request);
    }
}