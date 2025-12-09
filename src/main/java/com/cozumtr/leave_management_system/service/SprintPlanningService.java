package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.Sprint;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sprint otomatik planlama servisi.
 * Belirli aralıklarla çalışarak her departman için gelecekteki sprint'leri otomatik oluşturur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SprintPlanningService {

    private final SprintRepository sprintRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Tüm departmanlar için gelecekteki sprint'leri otomatik oluşturur.
     * Her departman için en son sprint'in bitiş tarihinden sonra, durationWeeks'e göre yeni sprint'ler oluşturur.
     * 
     * Bu metod scheduler tarafından otomatik çağrılır.
     * Ayrıca manuel olarak da çağrılabilir.
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Her gün saat 02:00'de çalışır
    public void createUpcomingSprints() {
        log.info("Sprint otomatik planlama başlatılıyor...");
        
        // 1. Veritabanındaki tüm benzersiz departman ID'lerini çek
        List<Long> departmentIds = departmentRepository.findAllDistinctDepartmentIds();
        log.info("Toplam {} departman bulundu", departmentIds.size());
        
        int totalCreated = 0;
        
        // 2. Her bir departman ID'si için
        for (Long departmentId : departmentIds) {
            try {
                int created = createSprintsForDepartment(departmentId);
                totalCreated += created;
            } catch (Exception e) {
                log.error("Departman {} için sprint oluşturulurken hata oluştu: {}", departmentId, e.getMessage());
            }
        }
        
        log.info("Sprint otomatik planlama tamamlandı. Toplam {} sprint oluşturuldu.", totalCreated);
    }

    /**
     * Belirli bir departman için gelecekteki sprint'leri oluşturur.
     * 
     * @param departmentId Departman ID
     * @return Oluşturulan sprint sayısı
     */
    @Transactional
    public int createSprintsForDepartment(Long departmentId) {
        // 1. Departmanı bul
        Department department = departmentRepository.findById(departmentId)
                .orElse(null);
        
        if (department == null || !department.getIsActive()) {
            log.warn("Departman {} bulunamadı veya aktif değil, atlanıyor", departmentId);
            return 0;
        }
        
        // 2. O departmana ait en son bitiş tarihli Sprint kaydını bul
        List<Sprint> sprints = sprintRepository.findAllByDepartmentIdOrderByEndDateDesc(departmentId);
        
        if (sprints.isEmpty()) {
            log.info("Departman {} için henüz sprint kaydı yok, ilk sprint manuel oluşturulmalı", departmentId);
            return 0;
        }
        
        Sprint latestSprint = sprints.get(0); // En son bitiş tarihli sprint (ilk eleman)
        
        // 3. En son sprint kaydından 'durationWeeks' değerini çek
        Integer durationWeeks = latestSprint.getDurationWeeks();
        
        if (durationWeeks == null || durationWeeks <= 0) {
            log.warn("Departman {} için en son sprint'te durationWeeks değeri yok veya geçersiz, atlanıyor", departmentId);
            return 0;
        }
        
        // 4. En son sprint'in adından numarayı çıkar (örn: "Sprint 3 - IT - 2024" → 3)
        int lastSprintNumber = extractSprintNumberFromName(latestSprint.getName());
        int nextSprintNumber = lastSprintNumber + 1; // Bir sonraki sprint numarası
        
        // 5. Yeni başlangıç tarihini bul (latestEndDate + 1 gün)
        LocalDate newStartDate = latestSprint.getEndDate().plusDays(1);
        
        // 6. Bugünden en az 6 ay sonrasına kadar sprint oluştur
        LocalDate targetEndDate = LocalDate.now().plusMonths(6);
        int createdCount = 0;
        
        // 7. Yeterli sayıda sprint oluşturulana kadar tekrarla
        while (newStartDate.isBefore(targetEndDate) || newStartDate.isEqual(targetEndDate)) {
            // Yeni bitiş tarihini hesapla: newStartDate + durationWeeks hafta
            LocalDate newEndDate = newStartDate.plusWeeks(durationWeeks).minusDays(1);
            
            // Sprint adı oluştur (en son sprint numarasından devam ederek)
            String sprintName = generateSprintName(department, newStartDate, nextSprintNumber);
            
            // Sprint oluştur
            Sprint newSprint = new Sprint();
            newSprint.setName(sprintName);
            newSprint.setStartDate(newStartDate);
            newSprint.setEndDate(newEndDate);
            newSprint.setDurationWeeks(durationWeeks); // Bir önceki sprint'in durationWeeks değerini miras al
            newSprint.setDepartment(department);
            
            sprintRepository.save(newSprint);
            createdCount++;
            nextSprintNumber++; // Bir sonraki sprint için numarayı artır
            
            log.debug("Departman {} için yeni sprint oluşturuldu: {} ({} - {})", 
                    departmentId, sprintName, newStartDate, newEndDate);
            
            // Bir sonraki sprint için başlangıç tarihini güncelle
            newStartDate = newEndDate.plusDays(1);
        }
        
        if (createdCount > 0) {
            log.info("Departman {} için {} adet sprint oluşturuldu", departmentId, createdCount);
        }
        
        return createdCount;
    }

    /**
     * Sprint adı oluşturur.
     * Format: "Sprint {numara} - {departman adı} - {yıl}"
     * 
     * @param department Departman
     * @param startDate Başlangıç tarihi
     * @param sprintNumber Sprint numarası (örn: 1, 2, 3...)
     * @return Sprint adı
     */
    private String generateSprintName(Department department, LocalDate startDate, int sprintNumber) {
        int year = startDate.getYear();
        String departmentName = department.getName();
        
        return String.format("Sprint %d - %s - %d", sprintNumber, departmentName, year);
    }

    /**
     * Sprint adından numarayı çıkarır.
     * Örnek: "Sprint 3 - IT - 2024" → 3
     * 
     * @param sprintName Sprint adı
     * @return Sprint numarası, bulunamazsa 0
     */
    private int extractSprintNumberFromName(String sprintName) {
        if (sprintName == null || sprintName.trim().isEmpty()) {
            return 0;
        }
        
        try {
            // "Sprint 3 - IT - 2024" formatından numarayı çıkar
            // Regex ile "Sprint " sonrasındaki sayıyı bul
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Sprint\\s+(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(sprintName);
            
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("Sprint adından numara çıkarılamadı: {}. Varsayılan olarak 0 kullanılıyor.", sprintName);
        }
        
        // Numarayı bulamazsa, mevcut sprint sayısını kullan
        return 0;
    }
}

