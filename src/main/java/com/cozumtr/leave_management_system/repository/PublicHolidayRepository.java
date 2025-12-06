package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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
}