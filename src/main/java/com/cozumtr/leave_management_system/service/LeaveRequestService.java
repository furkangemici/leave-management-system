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

    @Transactional
    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        
        // 1. GÜVENLİK: Giriş yapan kullanıcıyı Token'dan bul
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + email));

        // 2. VALIDASYON: Tarih Kontrolü
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Bitiş tarihi başlangıç tarihinden önce olamaz!");
        }

        // 3. ÇAKIŞMA KONTROLÜ
        boolean hasOverlap = leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                employee.getId(),
                request.getStartDate(),
                request.getEndDate(),
                List.of(RequestStatus.REJECTED, RequestStatus.CANCELLED)
        );

        if (hasOverlap) {
            throw new IllegalStateException("Seçilen tarih aralığında zaten mevcut bir izin kaydınız var!");
        }

        // 4. İzin Türünü Bul
        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Geçersiz İzin Türü ID: " + request.getLeaveTypeId()));

        // 5. HESAPLAMA: Task 8'deki Motoru Çağır
        BigDecimal duration = leaveCalculationService.calculateDuration(
                request.getStartDate().toLocalDate(),
                request.getEndDate().toLocalDate()
        );

        if (duration.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hesaplanabilir iş günü bulunamadı (Tatil veya Haftasonu).");
        }

        // 6. Entity Oluştur ve Kaydet
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDateTime(request.getStartDate());
        leaveRequest.setEndDateTime(request.getEndDate());
        
        // DİKKAT: Senin Entity'de isim "durationHours" olduğu için bunu kullanıyoruz.
        leaveRequest.setDurationHours(duration); 
        
        leaveRequest.setReason(request.getReason());
        
        // DİKKAT: Senin Entity'de varsayılan statü bu.
        leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);

        // DİKKAT: Entity'de "nullable = false" olduğu için bunu set etmeliyiz.
        // Task 10'da burası dinamik olacak, şimdilik "MANAGER" atıyoruz.
        leaveRequest.setWorkflowNextApproverRole("MANAGER"); 

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        // 7. Cevap Dön (DTO)
        return LeaveRequestResponse.builder()
                .id(savedRequest.getId())
                .leaveTypeName(leaveType.getName())
                .startDate(savedRequest.getStartDateTime())
                .endDate(savedRequest.getEndDateTime())
                .duration(savedRequest.getDurationHours())
                .status(savedRequest.getRequestStatus())
                .createdAt(LocalDateTime.now()) // BaseEntity'den gelmiyorsa manuel set ettim
                .build();
    }
}