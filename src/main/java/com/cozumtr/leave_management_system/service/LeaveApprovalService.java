package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApprovalService {

    private final LeaveRequestRepository leaveRequestRepository;

    /**
     * İzin talebini onaylar ve bir sonraki aşamaya geçirir.
     * Dinamik Workflow (İş Akışı) motoru burada çalışır.
     */
    @Transactional
    public void approveRequest(Long requestId) {
        // 1. İzni Bul
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("İzin talebi bulunamadı ID: " + requestId));

        // 2. GÜVENLİK: İşlemi yapan kişinin rolünü al
        String currentUserRole = getCurrentUserRole(); // Örn: "MANAGER"
        log.info("Onay İşlemi Başladı. RequestID: {}, İşlemi Yapan Rol: {}", requestId, currentUserRole);

        // 3. YETKİ KONTROLÜ: Sıra gerçekten bu kişide mi?
        // Veritabanı "Sıra MANAGER'da" diyorsa ve giren kişi "HR" ise hata ver.
        if (!request.getWorkflowNextApproverRole().equalsIgnoreCase(currentUserRole)) {
            throw new IllegalStateException("Bu talebi onaylama yetkiniz yok veya sıranız gelmedi. Beklenen Rol: " + request.getWorkflowNextApproverRole());
        }

        // 4. WORKFLOW MOTORU (Dinamik Karar Mekanizması)
        // Kural Kitabını Oku: "MANAGER,HR"
        String workflowDefinition = request.getLeaveType().getWorkflowDefinition();

        // Adımları ayır: ["MANAGER", "HR"]
        String[] steps = workflowDefinition.split(",");

        String nextRole = null;
        boolean foundCurrentStep = false;

        // Döngü ile sıradaki kişiyi bul
        for (String step : steps) {
            if (foundCurrentStep) {
                nextRole = step; // Bir sonraki adımı yakaladık! (Örn: HR)
                break;
            }
            if (step.equalsIgnoreCase(currentUserRole)) {
                foundCurrentStep = true; // Şu anki adımı bulduk, bir sonrakine bakacağız.
            }
        }

        // 5. DURUM GÜNCELLEME
        if (nextRole != null) {
            // Sırada başka biri var -> Ona pasla
            request.setWorkflowNextApproverRole(nextRole);

            // İstersen durumu "APPROVED_MANAGER" gibi ara statülere çekebilirsin.
            // Şimdilik "PENDING_APPROVAL" kalmasında sakınca yok, çünkü onaycı rolü değişti.
            log.info("Talep bir sonraki aşamaya geçti. Yeni Onaycı: {}", nextRole);
        } else {
            // Sırada kimse kalmadı -> İŞLEM BİTTİ, TAM ONAYLANDI 🎉
            request.setRequestStatus(RequestStatus.APPROVED);
            request.setWorkflowNextApproverRole("NONE"); // Artık kimse beklemiyor
            log.info("Workflow tamamlandı. İzin tamamen ONAYLANDI.");
        }

        leaveRequestRepository.save(request);
    }

    // --- YARDIMCI METOT: Kullanıcı Rolünü Bul ---
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getAuthorities().isEmpty()) {
            // Spring Security rolleri genelde "ROLE_MANAGER" diye tutar.
            // Biz veritabanında "MANAGER" tuttuğumuz için "ROLE_" kısmını siliyoruz.
            String role = auth.getAuthorities().iterator().next().getAuthority();
            return role.replace("ROLE_", "");
        }
        // Testler veya anonim durumlar için:
        return "UNKNOWN";
    }
}