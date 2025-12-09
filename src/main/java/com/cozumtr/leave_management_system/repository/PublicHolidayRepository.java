package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    /**
     * Verilen başlangıç ve bitiş tarihleri (dahil) arasındaki tüm resmi tatilleri getirir.
     * Bu metot, hesaplama sırasında gereksiz veri çekilmesini önler.
     *
     * @param startDate Sorgu başlangıç tarihi
     * @param endDate   Sorgu bitiş tarihi
     * @return O aralıktaki tatil listesi
     */
    List<PublicHoliday> findAllByDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Belirli bir tarihin resmi tatil olup olmadığını kontrol eder.
     *
     * @param date Kontrol edilecek tarih
     * @return Tarih resmi tatil ise true, değilse false
     */
    boolean existsByDate(LocalDate date);

    /**
     * Belirli bir tarihe göre resmi tatil getirir.
     *
     * @param date Tarih
     * @return Resmi tatil (varsa)
     */
    Optional<PublicHoliday> findByDate(LocalDate date);
}