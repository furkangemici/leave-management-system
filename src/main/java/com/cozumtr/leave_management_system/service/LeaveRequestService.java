package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.CreateLeaveRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveRequestResponse;
import com.cozumtr.leave_management_system.dto.response.TeamLeaveResponseDTO;
import com.cozumtr.leave_management_system.dto.response.LeaveApprovalHistoryResponse;
import com.cozumtr.leave_management_system.dto.response.SprintOverlapReportDTO;
import com.cozumtr.leave_management_system.dto.response.OverlappingLeaveDetailDTO;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveApprovalHistory;
import com.cozumtr.leave_management_system.entities.LeaveAttachment;
import com.cozumtr.leave_management_system.entities.LeaveEntitlement;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.dto.response.ApprovalHistoryDTO;
import com.cozumtr.leave_management_system.dto.response.ManagerLeaveResponseDTO;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.LeaveApprovalHistoryRepository;
import com.cozumtr.leave_management_system.repository.LeaveEntitlementRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import com.cozumtr.leave_management_system.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveCalculationService leaveCalculationService;
    private final LeaveEntitlementRepository leaveEntitlementRepository;
    private final LeaveApprovalHistoryRepository leaveApprovalHistoryRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final UserRepository userRepository;
    private final com.cozumtr.leave_management_system.service.LeaveAttachmentService leaveAttachmentService;
    private final EmailService emailService;

    // --- İZİN TALEBİ OLUŞTURMA ---
    @Transactional
    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request, MultipartFile file) {
        // 1. Güvenlik: Giriş yapanı bul
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + email));

        // 2. Tarih Kontrolü
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("Bitiş tarihi başlangıç tarihinden önce olamaz!");
        }

        // 3. Çakışma Kontrolü
        boolean hasOverlap = leaveRequestRepository.existsByEmployeeAndDateRangeOverlap(
                employee.getId(),
                request.getStartDate(),
                request.getEndDate(),
                List.of(RequestStatus.REJECTED, RequestStatus.CANCELLED)
        );

        if (hasOverlap) {
            throw new BusinessException("Seçilen tarih aralığında zaten mevcut bir izin kaydınız var!");
        }

        // 4. İzin Türü ve Süre Hesaplama
        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Geçersiz İzin Türü ID: " + request.getLeaveTypeId()));

        if (leaveType.isDocumentRequired() && (file == null || file.isEmpty())) {
            throw new BusinessException("Bu izin türü için belge yüklemek zorunludur.");
        }

        BigDecimal duration;

        // İzin türüne göre süre hesaplama
        if (leaveType.getRequestUnit() == com.cozumtr.leave_management_system.enums.RequestUnit.HOUR) {
            // Saatlik izinler için saat hesaplama
            // ÖNEMLİ: Saatlik izinler de sadece çalışma günlerinde alınabilir
            // Hafta sonu ve resmi tatil kontrolü yapılmalı

            LocalDate startDate = request.getStartDate().toLocalDate();
            LocalDate endDate = request.getEndDate().toLocalDate();

            // Hafta sonu kontrolü
            DayOfWeek startDayOfWeek = startDate.getDayOfWeek();
            DayOfWeek endDayOfWeek = endDate.getDayOfWeek();
            boolean isStartWeekend = (startDayOfWeek == DayOfWeek.SATURDAY || startDayOfWeek == DayOfWeek.SUNDAY);
            boolean isEndWeekend = (endDayOfWeek == DayOfWeek.SATURDAY || endDayOfWeek == DayOfWeek.SUNDAY);

            if (isStartWeekend || isEndWeekend) {
                throw new BusinessException("Saatlik izinler hafta sonu günlerinde alınamaz!");
            }

            // Resmi tatil kontrolü
            if (publicHolidayRepository.existsByDateInRange(startDate) ||
                    (!startDate.equals(endDate) && publicHolidayRepository.existsByDateInRange(endDate))) {
                throw new BusinessException("Saatlik izinler resmi tatil günlerinde alınamaz!");
            }

            // Hafta sonu ve tatil kontrolünden geçtiyse saat farkını hesapla
            long hoursBetween = java.time.Duration.between(
                    request.getStartDate(),
                    request.getEndDate()
            ).toHours();
            duration = BigDecimal.valueOf(hoursBetween);
        } else {
            // Günlük izinler için net çalışma saatini hesapla
            // calculateDuration artık saat döndürüyor (hafta sonları ve resmi tatiller düşülmüş)
            duration = leaveCalculationService.calculateDuration(
                    request.getStartDate().toLocalDate(),
                    request.getEndDate().toLocalDate(),
                    employee.getDailyWorkHours()
            );
        }

        if (duration.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Hesaplanabilir süre bulunamadı (Tatil veya Haftasonu).");
        }

        // 4.5. Bakiye Kontrolü - İzin türüne göre farklı kontrol
        validateLeaveBalance(employee, leaveType, duration, request.getStartDate());

        // 4.6. Workflow Başlatma
        String workflowDefinition = leaveType.getWorkflowDefinition();
        if (workflowDefinition == null || workflowDefinition.trim().isEmpty()) {
            throw new BusinessException(
                    "İzin türü için onay akışı tanımlanmamış. Lütfen İK departmanı ile iletişime geçin."
            );
        }

        // Virgülle ayrılmış rollerden ilkini al (örn: "HR,MANAGER,CEO" -> "HR")
        String[] workflowRoles = workflowDefinition.split(",");
        String firstApproverRole = workflowRoles[0].trim();

        // 5. Kayıt
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDateTime(request.getStartDate());
        leaveRequest.setEndDateTime(request.getEndDate());
        leaveRequest.setDurationHours(duration);
        leaveRequest.setReason(request.getReason() != null ? request.getReason() : "");
        leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
        leaveRequest.setWorkflowNextApproverRole(firstApproverRole);

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        if (file != null && !file.isEmpty()) {
            leaveAttachmentService.uploadAttachment(savedRequest.getId(), file);
        }

        // BİLDİRİM A: İlk onaycıya sıra geldiğini bildir
        notifyNextApprover(savedRequest, firstApproverRole);

        return mapToResponse(savedRequest);
    }

    @Transactional
    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        return createLeaveRequest(request, null);
    }

    // ---  İZİN İPTALİ  ---
    @Transactional
    public void cancelLeaveRequest(Long id) {
        // 1. Güvenlik: İşlemi yapan kim?
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. İzni bul
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("İzin talebi bulunamadı ID: " + id));

        // 3. Yetki Kontrolü: Başkasının iznini iptal edemezsin
        if (!request.getEmployee().getEmail().equals(currentEmail)) {
            throw new BusinessException("Bu işlem için yetkiniz yok! Sadece kendi izinlerinizi iptal edebilirsiniz.");
        }

        // 4. Mantık Kontrolü: Onaylanmış izinler de iptal edilebilir (bakiye geri alınır)
        // PENDING_APPROVAL, APPROVED_HR, APPROVED_MANAGER, APPROVED durumları iptal edilebilir
        if (request.getRequestStatus() == RequestStatus.REJECTED || request.getRequestStatus() == RequestStatus.CANCELLED) {
            throw new BusinessException("Bu izin talebi zaten iptal edilmiş veya reddedilmiş durumda.");
        }

        // 5. Eğer izin tam onaylanmış durumdaysa (APPROVED), bakiyeyi geri al
        // APPROVED_HR ve APPROVED_MANAGER durumlarında bakiye düşülmediği için geri alınmasına gerek yok
        if (request.getRequestStatus() == RequestStatus.APPROVED) {
            restoreLeaveBalance(request);
        }

        // 6. İptal Et (Veritabanından silmiyoruz, durumunu güncelliyoruz -> Soft Delete mantığı)
        request.setRequestStatus(RequestStatus.CANCELLED);
        request.setWorkflowNextApproverRole("");
        leaveRequestRepository.save(request);
    }

    /**
     * İzin talebini onaylar ve bakiyeyi günceller.
     *
     * @param requestId İzin talebi ID'si
     * @param comments Onay yorumu (opsiyonel)
     */
    @Transactional
    public LeaveRequestResponse approveLeaveRequest(Long requestId, String comments) {
        // 1. Güvenlik: Giriş yapan onaylayıcıyı bul
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee approver = employeeRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Onaylayıcı bulunamadı: " + currentEmail));

        // 2. İzin talebini bul
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("İzin talebi bulunamadı ID: " + requestId));

        ensureRequiredDocumentIfNeeded(leaveRequest);

        // 3. Durum kontrolü
        if (leaveRequest.getRequestStatus() == RequestStatus.APPROVED) {
            throw new BusinessException("Bu izin talebi zaten onaylanmış durumda.");
        }
        if (leaveRequest.getRequestStatus() == RequestStatus.REJECTED ||
                leaveRequest.getRequestStatus() == RequestStatus.CANCELLED) {
            throw new BusinessException("İptal edilmiş veya reddedilmiş izin talepleri onaylanamaz.");
        }

        // 4. Workflow mantığı: Bir sonraki onaycı rolünü kontrol et ve güncelle
        // User'ı roles bilgisiyle birlikte çek
        com.cozumtr.leave_management_system.entities.User approverUser = userRepository
                .findByEmployeeEmail(currentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Onaylayıcı kullanıcısı bulunamadı: " + currentEmail));

        String nextApproverRole = leaveRequest.getWorkflowNextApproverRole();

        // Kullanıcının tüm rollerini kontrol edip workflow'daki rolü bul
        String currentRole = approverUser.getRoles().stream()
                .map(role -> role.getRoleName())
                .filter(roleName -> roleName.equals(nextApproverRole))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        String.format("Bu izin talebini onaylama yetkiniz yok. Beklenen rol: %s, Sizin rolleriniz: %s",
                                nextApproverRole,
                                approverUser.getRoles().stream()
                                        .map(role -> role.getRoleName())
                                        .collect(java.util.stream.Collectors.joining(", ")))
                ));

        // 5. Workflow ilerletme
        LeaveType leaveType = leaveRequest.getLeaveType();
        String workflowDefinition = leaveType.getWorkflowDefinition();
        String[] workflowRoles = workflowDefinition.split(",");

        // Mevcut rolün index'ini bul
        int currentRoleIndex = -1;
        for (int i = 0; i < workflowRoles.length; i++) {
            if (workflowRoles[i].trim().equals(currentRole)) {
                currentRoleIndex = i;
                break;
            }
        }

        // Eğer rol workflow'da yoksa hata fırlat
        if (currentRoleIndex == -1) {
            throw new BusinessException(
                    String.format("Onaylayıcının rolü (%s) bu izin türü için workflow'da tanımlı değil.", currentRole)
            );
        }

        // Son onaylayıcı mı?
        if (currentRoleIndex == workflowRoles.length - 1) {
            // TAM ONAY - Bakiyeyi düşür
            leaveRequest.setRequestStatus(RequestStatus.APPROVED);
            leaveRequest.setWorkflowNextApproverRole(""); // Artık onaylayıcı yok (nullable değil, boş string kullanıyoruz)
            deductLeaveBalance(leaveRequest);
            
            // BİLDİRİM C: Nihai onay - Talep sahibine bildir
            String approverName = approver.getFirstName() + " " + approver.getLastName();
            emailService.sendFinalDecisionNotification(leaveRequest, true, approverName);
        } else {
            // Ara onay - Bir sonraki onaycıya geç
            String nextRole = workflowRoles[currentRoleIndex + 1].trim();
            leaveRequest.setWorkflowNextApproverRole(nextRole);

            // Ara onay durumunu set et
            if (currentRole.equals("HR")) {
                leaveRequest.setRequestStatus(RequestStatus.APPROVED_HR);
            } else if (currentRole.equals("MANAGER")) {
                leaveRequest.setRequestStatus(RequestStatus.APPROVED_MANAGER);
            } else {
                // Diğer roller için PENDING_APPROVAL kalabilir veya başka bir durum
                // Şimdilik PENDING_APPROVAL olarak bırakıyoruz
                leaveRequest.setRequestStatus(RequestStatus.PENDING_APPROVAL);
            }
            
            // BİLDİRİM B: Aşamalı ilerleme - Talep sahibine bildir
            String approverName = approver.getFirstName() + " " + approver.getLastName();
            emailService.sendProgressNotification(leaveRequest, approverName, nextRole);
            
            // BİLDİRİM A: Sıradaki onaycıya bildir
            notifyNextApprover(leaveRequest, nextRole);
        }

        // 6. Onay geçmişi kaydet
        saveApprovalHistory(leaveRequest, approver, leaveRequest.getRequestStatus(), comments);

        // 7. İzin talebini kaydet
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        return mapToResponse(savedRequest);
    }

    /**
     * İzin talebini reddeder.
     *
     * @param requestId İzin talebi ID'si
     * @param comments Red yorumu (opsiyonel)
     */
    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(Long requestId, String comments) {
        // 1. Güvenlik: Giriş yapan onaylayıcıyı bul
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee approver = employeeRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Onaylayıcı bulunamadı: " + currentEmail));

        // 2. İzin talebini bul
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("İzin talebi bulunamadı ID: " + requestId));

        // 3. Durum kontrolü
        if (leaveRequest.getRequestStatus() == RequestStatus.APPROVED) {
            // Eğer tam onaylanmış ise, bakiyeyi geri al
            // APPROVED durumunda workflow kontrolü yapılmaz (tüm onaylar tamamlanmış)
            restoreLeaveBalance(leaveRequest);
        } else if (leaveRequest.getRequestStatus() == RequestStatus.REJECTED ||
                leaveRequest.getRequestStatus() == RequestStatus.CANCELLED) {
            throw new BusinessException("Bu izin talebi zaten reddedilmiş veya iptal edilmiş durumda.");
        } else {
            // 4. Workflow kontrolü (sadece PENDING_APPROVAL, APPROVED_HR, APPROVED_MANAGER durumları için)
            // User'ı roles bilgisiyle birlikte çek
            com.cozumtr.leave_management_system.entities.User approverUser = userRepository
                    .findByEmployeeEmail(currentEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Onaylayıcı kullanıcısı bulunamadı: " + currentEmail));

            String nextApproverRole = leaveRequest.getWorkflowNextApproverRole();

            // Kullanıcının tüm rollerini kontrol edip workflow'daki rolü bul
            boolean hasRequiredRole = approverUser.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .anyMatch(roleName -> roleName.equals(nextApproverRole));

            if (!hasRequiredRole) {
                throw new BusinessException(
                        String.format("Bu izin talebini reddetme yetkiniz yok. Beklenen rol: %s, Sizin rolleriniz: %s",
                                nextApproverRole,
                                approverUser.getRoles().stream()
                                        .map(role -> role.getRoleName())
                                        .collect(java.util.stream.Collectors.joining(", ")))
                );
            }
        }

        // 5. Reddet
        leaveRequest.setRequestStatus(RequestStatus.REJECTED);
        leaveRequest.setWorkflowNextApproverRole("");

        // 6. Red geçmişi kaydet
        saveApprovalHistory(leaveRequest, approver, RequestStatus.REJECTED, comments);

        // 7. İzin talebini kaydet
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        
        // BİLDİRİM C: Nihai red - Talep sahibine bildir
        String approverName = approver.getFirstName() + " " + approver.getLastName();
        emailService.sendFinalDecisionNotification(leaveRequest, false, approverName);

        return mapToResponse(savedRequest);
    }



    private void saveApprovalHistory(LeaveRequest leaveRequest,
                                     Employee approver,
                                     RequestStatus action,
                                     String comments) {
        LeaveApprovalHistory history = new LeaveApprovalHistory();
        history.setLeaveRequest(leaveRequest);
        history.setApprover(approver);
        history.setAction(action);
        history.setComments(comments != null ? comments : "");
        leaveApprovalHistoryRepository.save(history);
    }

    // --- KENDİ İZİN TALEPLERİMİ LİSTELEME ---
    public List<LeaveRequestResponse> getMyLeaveRequests() {
        // 1. Güvenlik: Giriş yapanı bul
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + email));

        // 2. Kullanıcının tüm izin taleplerini getir
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeId(employee.getId());

        // 3. DTO'ya map et
        return leaveRequests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- EKİP İZİN TAKİBİ (TEAM VISIBILITY) ---
    /**
     * Belirli bir çalışanın departmanındaki onaylanmış izinleri getirir.
     *
     * @param employeeId Çalışan ID'si
     * @return Departmandaki onaylanmış izinlerin listesi
     * @throws EntityNotFoundException Eğer çalışan bulunamazsa
     */
    public List<TeamLeaveResponseDTO> getTeamApprovedLeaves(Long employeeId) {
        // 1. Verilen employeeId ile Employee kaydını çek ve çalışanın 'departmentId' bilgisini al
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Çalışan bulunamadı ID: " + employeeId));

        Long departmentId = employee.getDepartment().getId();

        // 2. Repository metodunu çağırırken departmentId ve LocalDateTime.now() parametrelerini kullan
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeavesByDepartment(
                departmentId,
                LocalDateTime.now()
        );

        // 3. Dönüşleri TeamLeaveResponseDTO'ya manuel olarak map'le ve List<TeamLeaveResponseDTO> olarak döndür
        return approvedLeaves.stream()
                .map(this::mapToTeamLeaveResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ManagerLeaveResponseDTO> getManagerDashboardRequests() {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        com.cozumtr.leave_management_system.entities.User currentUser = userRepository.findByEmployeeEmail(currentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + currentEmail));

        Employee currentEmployee = currentUser.getEmployee();

        List<String> approverRoles = currentUser.getRoles().stream()
                .map(role -> role.getRoleName())
                .filter(roleName -> roleName.equals("MANAGER") || roleName.equals("HR") || roleName.equals("CEO"))
                .toList();

        if (approverRoles.isEmpty()) {
            throw new BusinessException("Bu ekranı görüntüleme yetkiniz yok.");
        }

        List<LeaveRequest> leaveRequests;
        if (approverRoles.contains("HR") || approverRoles.contains("CEO")) {
            leaveRequests = leaveRequestRepository.findByWorkflowNextApproverRoleIn(approverRoles);
        } else {
            if (currentEmployee == null || currentEmployee.getDepartment() == null) {
                throw new BusinessException("Departman bilgisi bulunamadı.");
            }
            leaveRequests = leaveRequestRepository.findByWorkflowNextApproverRoleInAndDepartmentId(
                    approverRoles,
                    currentEmployee.getDepartment().getId()
            );
        }

        Set<RequestStatus> excludedStatuses = Set.of(
                RequestStatus.REJECTED,
                RequestStatus.CANCELLED,
                RequestStatus.APPROVED
        );

        return leaveRequests.stream()
                .filter(lr -> lr.getRequestStatus() == null || !excludedStatuses.contains(lr.getRequestStatus()))
                .sorted(Comparator.comparing(LeaveRequest::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::mapToManagerResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ManagerLeaveResponseDTO> getManagerAllRequests() {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        com.cozumtr.leave_management_system.entities.User currentUser = userRepository.findByEmployeeEmail(currentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı: " + currentEmail));

        Employee currentEmployee = currentUser.getEmployee();

        boolean isHrOrCeo = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("HR") || r.getRoleName().equals("CEO") || r.getRoleName().equals("ADMIN"));

        boolean isManager = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("MANAGER"));

        if (!isHrOrCeo && !isManager) {
            throw new BusinessException("Bu ekranı görüntüleme yetkiniz yok.");
        }

        List<LeaveRequest> leaveRequests;
        if (isHrOrCeo) {
            // HR and CEO see everything
            leaveRequests = leaveRequestRepository.findAllWithDetails();
        } else {
            // Managers see their department
            if (currentEmployee == null || currentEmployee.getDepartment() == null) {
                throw new BusinessException("Departman bilgisi bulunamadı.");
            }
            leaveRequests = leaveRequestRepository.findAllByDepartmentId(currentEmployee.getDepartment().getId());
        }

        return leaveRequests.stream()
                .map(this::mapToManagerResponse)
                .collect(Collectors.toList());
    }

    /**
     * İzin türüne göre bakiye kontrolü yapar.
     *
     * @param employee Çalışan
     * @param leaveType İzin türü
     * @param duration Talep edilen süre (saat)
     * @param startDate İzin başlangıç tarihi
     */
    private void validateLeaveBalance(Employee employee, LeaveType leaveType, BigDecimal duration, LocalDateTime startDate) {
        // Eğer izin türü yıllık izin bakiyesinden düşüyorsa
        if (leaveType.isDeductsFromAnnual()) {
            // Yıllık izin bakiyesi kontrolü
            int currentYear = LocalDate.now().getYear();
            LeaveEntitlement entitlement = leaveEntitlementRepository
                    .findByEmployeeIdAndYear(employee.getId(), currentYear)
                    .orElseThrow(() -> new BusinessException(
                            "Yıllık izin bakiyesi bulunamadı. Lütfen İK departmanı ile iletişime geçin."
                    ));

            BigDecimal remainingHours = entitlement.getRemainingHours();
            if (duration.compareTo(remainingHours) > 0) {
                throw new BusinessException(
                        String.format(
                                "Yetersiz yıllık izin bakiyesi! Talep edilen: %s saat, Kalan: %s saat",
                                duration, remainingHours
                        )
                );
            }
        } else {
            // Yıllık izin bakiyesinden düşmeyen izinler için kontrol
            // Mazeret İzni gibi aylık limitli izinler için aylık kullanım kontrolü
            if ("Mazeret İzni (Saatlik)".equals(leaveType.getName())) {
                int year = startDate.getYear();
                int month = startDate.getMonthValue();

                // 1. Mazeret izni her seferinde tam 2 saat olmalı
                BigDecimal requiredHours = new BigDecimal("2");
                if (duration.compareTo(requiredHours) != 0) {
                    throw new BusinessException(
                            String.format(
                                    "Mazeret izni sadece 2 saat olarak alınabilir! Talep edilen: %s saat",
                                    duration
                            )
                    );
                }

                // 2. Ayda maksimum 4 kere mazeret izni alınabilir
                Long monthlyRequestCount = leaveRequestRepository.countMonthlyUsageByLeaveType(
                        employee.getId(),
                        leaveType.getId(),
                        year,
                        month
                );

                int maxRequestsPerMonth = 4;
                if (monthlyRequestCount >= maxRequestsPerMonth) {
                    throw new BusinessException(
                            String.format(
                                    "Aylık mazeret izni sayısı limiti aşıldı! Bu ay %d kere mazeret izni aldınız (Maksimum: %d kere)",
                                    monthlyRequestCount, maxRequestsPerMonth
                            )
                    );
                }

                // 3. Aylık toplam saat limiti (8 saat = 4 kere × 2 saat)
                BigDecimal monthlyLimit = new BigDecimal("8");

                // O ay için kullanılan mazeret izni
                BigDecimal monthlyUsed = leaveRequestRepository.calculateMonthlyUsageByLeaveType(
                        employee.getId(),
                        leaveType.getId(),
                        year,
                        month
                );

                // Kalan mazeret izni
                BigDecimal remainingMonthlyHours = monthlyLimit.subtract(monthlyUsed);

                if (duration.compareTo(remainingMonthlyHours) > 0) {
                    throw new BusinessException(
                            String.format(
                                    "Aylık mazeret izni saat limiti aşıldı! Talep edilen: %s saat, Bu ay kalan: %s saat (Aylık limit: %s saat)",
                                    duration, remainingMonthlyHours, monthlyLimit
                            )
                    );
                }
            }
            // Diğer izin türleri (Hastalık İzni, Ücretsiz İzin vb.) için limit kontrolü yok
            // Bu izinler sınırsız olarak kabul edilir (iş kurallarına göre değişebilir)
        }
    }

    /**
     * İzin onaylandığında bakiyeyi düşürür (sadece deductsFromAnnual = true olan izinler için).
     *
     * @param leaveRequest İzin talebi
     */
    private void deductLeaveBalance(LeaveRequest leaveRequest) {
        LeaveType leaveType = leaveRequest.getLeaveType();

        // Sadece yıllık izin bakiyesinden düşen izinler için bakiye düşür
        if (leaveType.isDeductsFromAnnual()) {
            int currentYear = LocalDate.now().getYear();
            LeaveEntitlement entitlement = leaveEntitlementRepository
                    .findByEmployeeIdAndYear(leaveRequest.getEmployee().getId(), currentYear)
                    .orElseThrow(() -> new BusinessException(
                            "Yıllık izin bakiyesi bulunamadı. Lütfen İK departmanı ile iletişime geçin."
                    ));

            // Kullanılan saati artır
            BigDecimal newHoursUsed = entitlement.getHoursUsed().add(leaveRequest.getDurationHours());
            entitlement.setHoursUsed(newHoursUsed);
            leaveEntitlementRepository.save(entitlement);
        }
        // Mazeret izni gibi deductsFromAnnual = false olanlar için bakiye düşürülmez
        // Çünkü onlar zaten dinamik olarak hesaplanıyor (aylık limit üzerinden)
    }

    /**
     * İzin iptal/red edildiğinde bakiyeyi geri alır (sadece tam onaylanmış ve deductsFromAnnual = true olan izinler için).
     *
     * NOT: Bu metod sadece APPROVED durumundaki izinler için çağrılmalıdır.
     * APPROVED_HR ve APPROVED_MANAGER durumlarında bakiye düşülmediği için geri alınmasına gerek yoktur.
     *
     * @param leaveRequest İzin talebi (durumu APPROVED olmalı)
     */
    private void restoreLeaveBalance(LeaveRequest leaveRequest) {
        LeaveType leaveType = leaveRequest.getLeaveType();

        // Sadece yıllık izin bakiyesinden düşen ve tam onaylanmış izinler için bakiye geri al
        if (leaveType.isDeductsFromAnnual() && leaveRequest.getRequestStatus() == RequestStatus.APPROVED) {
            int currentYear = LocalDate.now().getYear();
            LeaveEntitlement entitlement = leaveEntitlementRepository
                    .findByEmployeeIdAndYear(leaveRequest.getEmployee().getId(), currentYear)
                    .orElseThrow(() -> new BusinessException(
                            "Yıllık izin bakiyesi bulunamadı. Lütfen İK departmanı ile iletişime geçin."
                    ));

            // Kullanılan saatten düş
            BigDecimal newHoursUsed = entitlement.getHoursUsed().subtract(leaveRequest.getDurationHours());

            // Negatif olamaz kontrolü
            if (newHoursUsed.compareTo(BigDecimal.ZERO) < 0) {
                newHoursUsed = BigDecimal.ZERO;
            }

            entitlement.setHoursUsed(newHoursUsed);
            leaveEntitlementRepository.save(entitlement);
        }
        // Mazeret izni gibi deductsFromAnnual = false olanlar için geri alma gerekmez
        // Çünkü onlar zaten dinamik olarak hesaplanıyor
    }

    private LeaveRequestResponse mapToResponse(LeaveRequest leaveRequest) {
        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .leaveTypeName(leaveRequest.getLeaveType().getName())
                .startDate(leaveRequest.getStartDateTime())
                .endDate(leaveRequest.getEndDateTime())
                .duration(leaveRequest.getDurationHours())
                .status(leaveRequest.getRequestStatus())
                .reason(leaveRequest.getReason())
                .workflowNextApproverRole(leaveRequest.getWorkflowNextApproverRole())
                .createdAt(leaveRequest.getCreatedAt())
                .build();
    }

    /**
     * LeaveRequest entity'sini TeamLeaveResponseDTO'ya map eder.
     *
     * @param leaveRequest İzin talebi entity'si
     * @return TeamLeaveResponseDTO
     */
    private TeamLeaveResponseDTO mapToTeamLeaveResponse(LeaveRequest leaveRequest) {
        Employee employee = leaveRequest.getEmployee();
        String employeeFullName = employee.getFirstName() + " " + employee.getLastName();
        String departmentName = employee.getDepartment().getName();
        String leaveTypeName = leaveRequest.getLeaveType().getName();

        return TeamLeaveResponseDTO.builder()
                .employeeFullName(employeeFullName)
                .departmentName(departmentName)
                .leaveTypeName(leaveTypeName)
                .startDate(leaveRequest.getStartDateTime())
                .endDate(leaveRequest.getEndDateTime())
                .totalHours(leaveRequest.getDurationHours())
                .build();
    }

    private ManagerLeaveResponseDTO mapToManagerResponse(LeaveRequest leaveRequest) {
        Employee employee = leaveRequest.getEmployee();

        List<ApprovalHistoryDTO> history = leaveRequest.getApprovalHistories().stream()
                .sorted(Comparator.comparing(LeaveApprovalHistory::getCreatedAt))
                .map(record -> ApprovalHistoryDTO.builder()
                        .approverFullName(record.getApprover().getFirstName() + " " + record.getApprover().getLastName())
                        .action(record.getAction())
                        .comments(record.getComments())
                        .actionDate(record.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ManagerLeaveResponseDTO.builder()
                .leaveRequestId(leaveRequest.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .employeeDepartmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .leaveTypeName(leaveRequest.getLeaveType().getName())
                .startDate(leaveRequest.getStartDateTime())
                .endDate(leaveRequest.getEndDateTime())
                .duration(leaveRequest.getDurationHours())
                .reason(leaveRequest.getReason())  
                .currentStatus(leaveRequest.getRequestStatus())
                .workflowNextApproverRole(leaveRequest.getWorkflowNextApproverRole())
                .approvalHistory(history)
                .build();
    }

    // --- GEÇMİŞ (AUDIT) ---
    public List<LeaveApprovalHistoryResponse> getLeaveApprovalHistory(Long leaveRequestId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new EntityNotFoundException("İzin talebi bulunamadı ID: " + leaveRequestId));

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isOwner = leaveRequest.getEmployee().getEmail().equals(currentEmail);
        boolean isPrivileged = hasAnyRole(currentEmail, Set.of("HR", "MANAGER", "CEO"));

        if (!isOwner && !isPrivileged) {
            throw new BusinessException("Bu izin talebinin geçmişini görüntüleme yetkiniz yok.");
        }

        List<LeaveApprovalHistory> histories =
                leaveApprovalHistoryRepository.findByLeaveRequestIdOrderByCreatedAtAsc(leaveRequestId);

        return histories.stream()
                .map(history -> LeaveApprovalHistoryResponse.builder()
                        .id(history.getId())
                        .approverFullName(history.getApprover().getFirstName() + " " + history.getApprover().getLastName())
                        .action(history.getAction())
                        .comments(history.getComments())
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean hasAnyRole(String email, Set<String> roles) {
        return userRepository.findByEmployeeEmail(email)
                .map(user -> user.getRoles().stream()
                        .map(role -> role.getRoleName())
                        .anyMatch(roles::contains))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<TeamLeaveResponseDTO> getCompanyCurrentApprovedLeaves() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean privileged = hasAnyRole(email, Set.of("HR", "CEO"));
        if (!privileged) {
            throw new BusinessException("Bu işlem için yetkiniz yok.");
        }

        List<LeaveRequest> leaves = leaveRequestRepository.findCurrentlyOnLeave(LocalDateTime.now());
        return leaves.stream()
                .map(this::mapToTeamLeaveResponse)
                .collect(Collectors.toList());
    }

    private void ensureRequiredDocumentIfNeeded(LeaveRequest leaveRequest) {
        LeaveType leaveType = leaveRequest.getLeaveType();
        if (leaveType != null && leaveType.isDocumentRequired()) {
            List<LeaveAttachment> attachments = leaveRequest.getAttachments();
            if (attachments == null || attachments.isEmpty()) {
                throw new BusinessException("Bu izin türü için zorunlu olan rapor eksiktir.");
            }
        }
    }

    /**
     * Sprint çakışma raporu oluşturur.
     * Verilen sprint tarih aralığı ile çakışan onaylanmış izinleri bulur ve
     * toplam kapasite kaybını (saat olarak) hesaplar.
     *
     * @param sprintStart Sprint başlangıç tarihi
     * @param sprintEnd Sprint bitiş tarihi
     * @return SprintOverlapReportDTO
     */
    @Transactional(readOnly = true)
    public SprintOverlapReportDTO generateSprintOverlapReport(LocalDateTime sprintStart, LocalDateTime sprintEnd) {
        // 1. Çakışan onaylı izinleri bul
        List<LeaveRequest> overlappingLeaves = leaveRequestRepository.findOverlappingApprovedLeaves(
                sprintStart, sprintEnd
        );

        // 2. Toplam kayıp saati ve detay listesini hesapla
        List<OverlappingLeaveDetailDTO> detailList = overlappingLeaves.stream()
                .map(leaveRequest -> {
                    // a. Çakışma aralığını belirle (MAX ve MIN tarih karşılaştırması)
                    // Gerçek çakışma başlangıcı: İzin başlangıcı ve sprint başlangıcından daha GEÇ olan (MAX)
                    // Mantık: İki tarihten hangisi daha geç ise o kullanılır
                    // Örnek: İzin 5 Ocak, Sprint 10 Ocak → MAX(5 Ocak, 10 Ocak) = 10 Ocak
                    LocalDateTime overlapStart = leaveRequest.getStartDateTime().isAfter(sprintStart)
                            ? leaveRequest.getStartDateTime()  // İzin daha geç başlıyorsa izin başlangıcı
                            : sprintStart;                      // Sprint daha geç başlıyorsa sprint başlangıcı

                    // Gerçek çakışma bitişi: İzin bitişi ve sprint bitişinden daha ERKEN olan (MIN)
                    // Mantık: İki tarihten hangisi daha erken ise o kullanılır
                    // Örnek: İzin 25 Ocak, Sprint 20 Ocak → MIN(25 Ocak, 20 Ocak) = 20 Ocak
                    LocalDateTime overlapEnd = leaveRequest.getEndDateTime().isBefore(sprintEnd)
                            ? leaveRequest.getEndDateTime()    // İzin daha erken bitiyorsa izin bitişi
                            : sprintEnd;                        // Sprint daha erken bitiyorsa sprint bitişi

                    // b. Çakışma aralığı için net çalışma saatini hesapla
                    Employee employee = leaveRequest.getEmployee();
                    BigDecimal overlappingHours = leaveCalculationService.calculateDuration(
                            overlapStart.toLocalDate(),
                            overlapEnd.toLocalDate(),
                            employee.getDailyWorkHours()
                    );

                    // c. DTO oluştur
                    String employeeFullName = employee.getFirstName() + " " + employee.getLastName();
                    String leaveTypeName = leaveRequest.getLeaveType().getName();

                    return OverlappingLeaveDetailDTO.builder()
                            .employeeFullName(employeeFullName)
                            .leaveTypeName(leaveTypeName)
                            .leaveStartDate(leaveRequest.getStartDateTime())
                            .leaveEndDate(leaveRequest.getEndDateTime())
                            .overlappingHours(overlappingHours)
                            .build();
                })
                .collect(Collectors.toList());

        // 3. Toplam kayıp saatini hesapla
        BigDecimal totalLossHours = detailList.stream()
                .map(OverlappingLeaveDetailDTO::getOverlappingHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Rapor DTO'sunu oluştur ve döndür
        return SprintOverlapReportDTO.builder()
                .totalLossHours(totalLossHours)
                .overlappingLeaves(detailList)
                .build();
    }
    
    /**
     * Sıradaki onaycıya bildirim gönderir
     * MANAGER rolü için: Sadece talep sahibinin departmanındaki manager'lara bildirim gönderilir
     * HR/CEO rolleri için: Tüm HR/CEO kullanıcılarına bildirim gönderilir
     * 
     * @param leaveRequest İzin talebi
     * @param approverRole Onaycının rolü
     */
    private void notifyNextApprover(LeaveRequest leaveRequest, String approverRole) {
        try {
            List<com.cozumtr.leave_management_system.entities.User> approvers;
            
            // MANAGER rolü için departman bazlı filtreleme
            if ("MANAGER".equals(approverRole)) {
                Employee employee = leaveRequest.getEmployee();
                if (employee.getDepartment() == null) {
                    log.warn("⚠️ Çalışanın departmanı bulunamadı. Email bildirimi gönderilemedi. Talep: #{}", 
                            leaveRequest.getId());
                    return;
                }
                
                Long departmentId = employee.getDepartment().getId();
                approvers = userRepository.findActiveUsersByRoleAndDepartment(approverRole, departmentId);
                
                log.debug("🔍 MANAGER bildirimi: Departman ID={}, Bulunan manager sayısı={}", 
                        departmentId, approvers.size());
            } else {
                // HR, CEO gibi roller için tüm kullanıcıları bul
                approvers = userRepository.findActiveUsersByRole(approverRole);
                
                log.debug("🔍 {} bildirimi: Bulunan kullanıcı sayısı={}", 
                        approverRole, approvers.size());
            }
            
            if (approvers.isEmpty()) {
                log.warn("⚠️ Rol '{}' için aktif onaycı bulunamadı. Email bildirimi gönderilemedi. Talep: #{}", 
                        approverRole, leaveRequest.getId());
                return;
            }
            
            // Tüm onaycılara bildirim gönder
            for (com.cozumtr.leave_management_system.entities.User approver : approvers) {
                if (approver.getEmployee() != null && approver.getEmployee().getEmail() != null) {
                    emailService.sendApprovalNotification(
                        approver.getEmployee().getEmail(), 
                        leaveRequest, 
                        approverRole
                    );
                }
            }
            
            log.info("✅ {} adet '{}' rolündeki onaycıya bildirim gönderildi. Talep: #{}", 
                    approvers.size(), approverRole, leaveRequest.getId());
        } catch (Exception e) {
            log.error("❌ Onaycıya bildirim gönderilirken hata oluştu: {}", e.getMessage(), e);
            // Email hatası kritik değil, işlem devam etsin
        }
    }
}