package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.PublicHoliday;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveCalculationServiceTest {

    @Mock
    private PublicHolidayRepository publicHolidayRepository;

    @InjectMocks
    private LeaveCalculationService leaveCalculationService;

    @Test
    void calculateDuration_ShouldExcludeWeekends() {
        // Senaryo: Cuma'dan Pazartesi'ye izin (4 gün takvim süresi)
        // Beklenen: Cuma(1) + Cmt(0) + Paz(0) + Pzt(1) = 2 Gün

        LocalDate start = LocalDate.of(2023, 11, 10); // Cuma
        LocalDate end = LocalDate.of(2023, 11, 13);   // Pazartesi

        // Mock: Tatil yok
        when(publicHolidayRepository.findAllByDateBetween(start, end)).thenReturn(List.of());

        BigDecimal result = leaveCalculationService.calculateDuration(start, end);

        assertEquals(new BigDecimal("2"), result);
        System.out.println(" Hafta sonu testi geçti: 2 gün hesaplandı.");
    }

    @Test
    void calculateDuration_ShouldHandleHalfDayHoliday() {
        // Senaryo: 28 Ekim (Yarım Gün) ve 29 Ekim (Tam Gün - Pazar)
        // 28 Ekim Cumartesi'ye denk geliyorsa hafta sonundan gider, biz hafta içi örneği yapalım.
        // Örn: 2024 yılında 28 Ekim Pazartesi (Yarım), 29 Ekim Salı (Tam)

        LocalDate start = LocalDate.of(2024, 10, 28); // Pazartesi (Arife)
        LocalDate end = LocalDate.of(2024, 10, 29);   // Salı (Cumhuriyet Bayramı)

        // Mock Verisi Hazırla
        PublicHoliday arife = new PublicHoliday();
        arife.setDate(start);
        arife.setHalfDay(true); // Yarım Gün

        PublicHoliday bayram = new PublicHoliday();
        bayram.setDate(end);
        bayram.setHalfDay(false); // Tam Gün

        when(publicHolidayRepository.findAllByDateBetween(start, end)).thenReturn(List.of(arife, bayram));

        BigDecimal result = leaveCalculationService.calculateDuration(start, end);

        // Pazartesi (0.5) + Salı (0) = 0.5 Gün olmalı
        assertEquals(new BigDecimal("0.5"), result);
        System.out.println("Yarım gün tatil testi geçti: 0.5 gün hesaplandı.");
    }
}