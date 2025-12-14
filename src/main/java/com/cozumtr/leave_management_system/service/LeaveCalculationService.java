package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.PublicHoliday;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveCalculationService {

    private final PublicHolidayRepository publicHolidayRepository;

    /**
     * İki tarih arasındaki net çalışma saatini hesaplar.
     * Hafta sonları (Cumartesi-Pazar) ve resmi tatiller düşülür.
     * Yarım gün tatiller (arife) yarım gün olarak sayılır.
     *
     * @param startDate Başlangıç tarihi
     * @param endDate Bitiş tarihi
     * @param dailyWorkHours Günlük mesai saati
     * @return Net kullanılacak izin saati (BigDecimal)
     */
    public BigDecimal calculateDuration(LocalDate startDate, LocalDate endDate, BigDecimal dailyWorkHours) {
        // --- 1. GÜVENLİK VE VALİDASYON ---
        if (startDate == null || endDate == null) {
            log.warn("Hesaplama hatası: Tarihler boş olamaz.");
            return BigDecimal.ZERO;
        }
        if (dailyWorkHours == null || dailyWorkHours.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Hesaplama hatası: Günlük mesai saati geçersiz.");
            return BigDecimal.ZERO;
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("Bitiş tarihi başlangıçtan önce olamaz!");
        }

        log.info("İzin süresi hesaplanıyor: {} - {}, Günlük mesai: {} saat", startDate, endDate, dailyWorkHours);

        // --- 2. VERİ HAZIRLIĞI (Performans Optimizasyonu) ---
        // Tüm resmi tatilleri bir kere çekip List'e al
        List<PublicHoliday> holidays = publicHolidayRepository.findHolidaysInRange(startDate, endDate);

        // --- 3. HESAPLAMA DÖNGÜSÜ ---
        BigDecimal netWorkingHours = BigDecimal.ZERO;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // A) Hafta Sonu Kontrolü
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            if (isWeekend) {
                currentDate = currentDate.plusDays(1);
                continue; // Hafta sonu izin sayılmaz, sonraki güne geç
            }

            // B) Resmi Tatil Kontrolü (tarih aralığında mı?)
            final LocalDate checkDate = currentDate;
            PublicHoliday holiday = holidays.stream()
                    .filter(h -> !checkDate.isBefore(h.getStartDate()) && !checkDate.isAfter(h.getEndDate()))
                    .findFirst()
                    .orElse(null);

            if (holiday != null) {
                if (holiday.getIsHalfDay()) {
                    // Yarım gün tatil (Arife) -> 0.5 gün * dailyWorkHours saat ekle
                    BigDecimal halfDayHours = dailyWorkHours.multiply(new BigDecimal("0.5"));
                    netWorkingHours = netWorkingHours.add(halfDayHours);
                    log.debug("{} tarihi Arife günü ({} saat) olarak hesaplandı.", currentDate, halfDayHours);
                }
                // Tam gün tatilse hiçbir şey ekleme (0 saat sayılır).
            }
            // C) Normal İş Günü
            else {
                netWorkingHours = netWorkingHours.add(dailyWorkHours);
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("Hesaplama tamamlandı. Toplam İzin: {} saat", netWorkingHours);
        return netWorkingHours;
    }
}
