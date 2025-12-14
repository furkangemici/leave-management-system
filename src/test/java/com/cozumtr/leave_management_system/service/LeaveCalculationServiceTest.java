package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.PublicHoliday;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import org.junit.jupiter.api.DisplayName;
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

    /**
     * Hafta sonu günlerinin (Cumartesi-Pazar) izin hesaplamasından düşülmesini test eder.
     * Senaryo: Cuma'dan Pazartesi'ye kadar izin alınıyor (4 gün takvim süresi).
     * Beklenen: Cuma (8 saat) + Cumartesi (0 saat - hafta sonu) + Pazar (0 saat - hafta sonu) + Pazartesi (8 saat) = 16 saat
     */
    @Test
    @DisplayName("calculateDuration_ShouldExcludeWeekends - Hafta sonu günleri (Cumartesi-Pazar) izin hesaplamasından düşülmeli")
    void calculateDuration_ShouldExcludeWeekends() {
        // Given: Cuma'dan Pazartesi'ye izin talebi
        LocalDate start = LocalDate.of(2023, 11, 10); // Cuma
        LocalDate end = LocalDate.of(2023, 11, 13);   // Pazartesi
        BigDecimal dailyWorkHours = new BigDecimal("8");

        // Mock: Bu tarih aralığında resmi tatil yok
        when(publicHolidayRepository.findHolidaysInRange(start, end)).thenReturn(List.of());

        // When: İzin süresi hesaplanıyor
        BigDecimal result = leaveCalculationService.calculateDuration(start, end, dailyWorkHours);

        // Then: Sadece çalışma günleri sayılmalı (Cuma + Pazartesi = 2 gün * 8 saat = 16 saat)
        // BigDecimal karşılaştırması için compareTo kullanıyoruz (scale farkı olabilir)
        assertEquals(0, result.compareTo(new BigDecimal("16")), 
                "Beklenen: 16 saat, Gerçek: " + result + " saat");
        System.out.println("Hafta sonu testi geçti: 16 saat hesaplandı.");
    }

    /**
     * Yarım gün tatillerin (arife günleri) yarım gün olarak sayılmasını ve tam gün tatillerin düşülmesini test eder.
     * Senaryo: 28 Ekim (Arife - Yarım Gün) ve 29 Ekim (Cumhuriyet Bayramı - Tam Gün Tatil) tarihlerinde izin alınıyor.
     * Beklenen: Pazartesi (4 saat - yarım gün tatil) + Salı (0 saat - tam gün tatil) = 4 saat
     */
    @Test
    @DisplayName("calculateDuration_ShouldHandleHalfDayHoliday - Yarım gün tatiller yarım gün sayılmalı, tam gün tatiller düşülmeli")
    void calculateDuration_ShouldHandleHalfDayHoliday() {
        // Given: 28 Ekim (Arife) ve 29 Ekim (Cumhuriyet Bayramı) tarihlerinde izin talebi
        LocalDate start = LocalDate.of(2024, 10, 28); // Pazartesi (Arife - Yarım Gün)
        LocalDate end = LocalDate.of(2024, 10, 29);   // Salı (Cumhuriyet Bayramı - Tam Gün Tatil)
        BigDecimal dailyWorkHours = new BigDecimal("8");

        // Mock: Resmi tatil verileri hazırlanıyor
        PublicHoliday arife = new PublicHoliday();
        arife.setStartDate(start);
        arife.setEndDate(start);
        arife.setIsHalfDay(true); // Yarım Gün Tatil (Arife)

        PublicHoliday bayram = new PublicHoliday();
        bayram.setStartDate(end);
        bayram.setEndDate(end);
        bayram.setIsHalfDay(false); // Tam Gün Tatil

        // Mock: Bu tarih aralığındaki tüm resmi tatilleri döndür
        when(publicHolidayRepository.findHolidaysInRange(start, end)).thenReturn(List.of(arife, bayram));

        // When: İzin süresi hesaplanıyor
        BigDecimal result = leaveCalculationService.calculateDuration(start, end, dailyWorkHours);

        // Then: Yarım gün tatil yarım gün sayılmalı, tam gün tatil düşülmeli
        // Pazartesi (0.5 gün * 8 saat = 4 saat) + Salı (0 saat - tam gün tatil) = 4 saat
        // BigDecimal karşılaştırması için compareTo kullanıyoruz (scale farkı olabilir)
        assertEquals(0, result.compareTo(new BigDecimal("4")), 
                "Beklenen: 4 saat, Gerçek: " + result + " saat");
        System.out.println("Yarım gün tatil testi geçti: 4 saat hesaplandı.");
    }

    /**
     * Tam gün resmi tatillerin izin hesaplamasından tamamen düşülmesini test eder.
     * Senaryo: Resmi tatil gününde izin alınıyor.
     * Beklenen: Tam gün tatil olduğu için 0 saat sayılmalı
     */
    @Test
    @DisplayName("calculateDuration_ShouldExcludeFullDayHoliday - Tam gün resmi tatiller izin hesaplamasından tamamen düşülmeli")
    void calculateDuration_ShouldExcludeFullDayHoliday() {
        // Given: Resmi tatil gününde izin talebi
        LocalDate holidayDate = LocalDate.of(2024, 10, 29); // 29 Ekim Cumhuriyet Bayramı (Tam Gün Tatil)
        BigDecimal dailyWorkHours = new BigDecimal("8");

        // Mock: Resmi tatil verisi
        PublicHoliday bayram = new PublicHoliday();
        bayram.setStartDate(holidayDate);
        bayram.setEndDate(holidayDate);
        bayram.setIsHalfDay(false); // Tam Gün Tatil

        // Mock: Bu tarih aralığındaki resmi tatilleri döndür
        when(publicHolidayRepository.findHolidaysInRange(holidayDate, holidayDate)).thenReturn(List.of(bayram));

        // When: İzin süresi hesaplanıyor
        BigDecimal result = leaveCalculationService.calculateDuration(holidayDate, holidayDate, dailyWorkHours);

        // Then: Tam gün tatil olduğu için 0 saat sayılmalı
        assertEquals(0, result.compareTo(BigDecimal.ZERO), 
                "Beklenen: 0 saat (tam gün tatil), Gerçek: " + result + " saat");
        System.out.println("Tam gün tatil testi geçti: 0 saat hesaplandı.");
    }

    /**
     * Sadece normal çalışma günlerinin (tatil ve hafta sonu olmayan) doğru hesaplanmasını test eder.
     * Senaryo: Pazartesi'den Cuma'ya kadar izin alınıyor (5 gün).
     * Beklenen: 5 gün * 8 saat = 40 saat
     */
    @Test
    @DisplayName("calculateDuration_ShouldCalculateNormalWorkingDays - Sadece normal çalışma günleri doğru hesaplanmalı")
    void calculateDuration_ShouldCalculateNormalWorkingDays() {
        // Given: Pazartesi'den Cuma'ya izin talebi (5 gün)
        LocalDate start = LocalDate.of(2024, 1, 1); // Pazartesi
        LocalDate end = LocalDate.of(2024, 1, 5);   // Cuma
        BigDecimal dailyWorkHours = new BigDecimal("8");

        // Mock: Bu tarih aralığında resmi tatil yok
        when(publicHolidayRepository.findHolidaysInRange(start, end)).thenReturn(List.of());

        // When: İzin süresi hesaplanıyor
        BigDecimal result = leaveCalculationService.calculateDuration(start, end, dailyWorkHours);

        // Then: 5 gün * 8 saat = 40 saat
        assertEquals(0, result.compareTo(new BigDecimal("40")), 
                "Beklenen: 40 saat, Gerçek: " + result + " saat");
        System.out.println("Normal çalışma günleri testi geçti: 40 saat hesaplandı.");
    }

    /**
     * Karmaşık senaryo: Hafta sonu, tam gün tatil ve normal çalışma günlerinin birlikte hesaplanmasını test eder.
     * Senaryo: Perşembe'den Salı'ya kadar izin (Perşembe normal, Cuma normal, Cumartesi hafta sonu, 
     * Pazar hafta sonu, Pazartesi normal, Salı tam gün tatil).
     * Beklenen: Perşembe (8) + Cuma (8) + Cumartesi (0) + Pazar (0) + Pazartesi (8) + Salı (0 - tatil) = 24 saat
     */
    @Test
    @DisplayName("calculateDuration_ShouldHandleComplexScenario - Hafta sonu, tatil ve normal günler birlikte doğru hesaplanmalı")
    void calculateDuration_ShouldHandleComplexScenario() {
        // Given: Perşembe'den Salı'ya izin talebi
        LocalDate start = LocalDate.of(2024, 10, 24); // Perşembe
        LocalDate end = LocalDate.of(2024, 10, 29); // Salı (29 Ekim - Cumhuriyet Bayramı)
        BigDecimal dailyWorkHours = new BigDecimal("8");

        // Mock: Salı günü tam gün tatil (29 Ekim Cumhuriyet Bayramı)
        PublicHoliday bayram = new PublicHoliday();
        bayram.setStartDate(LocalDate.of(2024, 10, 29)); // 29 Ekim
        bayram.setEndDate(LocalDate.of(2024, 10, 29));
        bayram.setIsHalfDay(false); // Tam Gün Tatil

        // Mock: Bu tarih aralığındaki resmi tatilleri döndür
        when(publicHolidayRepository.findHolidaysInRange(start, end)).thenReturn(List.of(bayram));

        // When: İzin süresi hesaplanıyor
        BigDecimal result = leaveCalculationService.calculateDuration(start, end, dailyWorkHours);

        // Then: Perşembe (8) + Cuma (8) + Cumartesi (0) + Pazar (0) + Pazartesi (8) + Salı (0 - tatil) = 24 saat
        assertEquals(0, result.compareTo(new BigDecimal("24")), 
                "Beklenen: 24 saat (Perşembe+Cuma+Pazartesi), Gerçek: " + result + " saat");
        System.out.println("Karmaşık senaryo testi geçti: 24 saat hesaplandı.");
    }
}
