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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveCalculationService {

    private final PublicHolidayRepository publicHolidayRepository;

    // OPTİMİZASYON (Madde 4):
    // Döngü içinde sürekli "new" yapmamak için sabitleri en başta tanımlıyoruz.
    // Bu sayede bellek (RAM) ve işlemciyi yormuyoruz.
    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");
    private static final BigDecimal FULL_DAY = BigDecimal.ONE;

    public BigDecimal calculateDuration(LocalDate startDate, LocalDate endDate) {
        // --- 1. GÜVENLİK VE VALİDASYON ---
        if (startDate == null || endDate == null) {
            log.warn("Hesaplama hatası: Tarihler boş olamaz.");
            return BigDecimal.ZERO;
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("Bitiş tarihi başlangıçtan önce olamaz!");
        }

        log.info("İzin süresi hesaplanıyor: {} - {}", startDate, endDate);

        // --- 2. VERİ HAZIRLIĞI ---
        List<PublicHoliday> holidays = publicHolidayRepository.findAllByDateBetween(startDate, endDate);

        // OPTİMİZASYON (Madde 2): (h1, h2) -> h1
        // Eğer veritabanında yanlışlıkla aynı güne 2 tatil girilmişse program çökmesin,
        // ilk bulduğu tatili kullansın. (Defensive Coding)
        Map<LocalDate, PublicHoliday> holidayMap = holidays.stream()
                .collect(Collectors.toMap(
                        PublicHoliday::getDate,
                        Function.identity(),
                        (existing, replacement) -> existing // Çakışma varsa mevcut olanı koru
                ));

        BigDecimal totalLeaveDays = BigDecimal.ZERO;
        LocalDate currentDate = startDate;

        // --- 3. HESAPLAMA DÖNGÜSÜ ---
        while (!currentDate.isAfter(endDate)) {

            // A) Hafta Sonu Kontrolü
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            if (isWeekend) {
                currentDate = currentDate.plusDays(1);
                continue; // Hafta sonu izin sayılmaz, sonraki güne geç
            }

            // B) Resmi Tatil Kontrolü
            if (holidayMap.containsKey(currentDate)) {
                PublicHoliday holiday = holidayMap.get(currentDate);

                if (holiday.isHalfDay()) {
                    // Yarım gün tatil (Arife) -> 0.5 gün ekle
                    totalLeaveDays = totalLeaveDays.add(HALF_DAY); // Sabit değişken kullandık
                    log.debug("{} tarihi Arife günü (0.5) olarak hesaplandı.", currentDate);
                }
                // Tam gün tatilse hiçbir şey ekleme (0 gün sayılır).
            }
            // C) Normal İş Günü
            else {
                totalLeaveDays = totalLeaveDays.add(FULL_DAY); // Sabit değişken kullandık
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("Hesaplama tamamlandı. Toplam İzin: {} gün", totalLeaveDays);
        return totalLeaveDays;
    }
}