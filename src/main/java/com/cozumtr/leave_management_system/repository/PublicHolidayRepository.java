package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    // Verilen iki tarih (başlangıç ve bitiş dahil) arasındaki tatilleri getirir.
    // SQL: SELECT * FROM public_holidays WHERE holiday_date BETWEEN start AND end
    List<PublicHoliday> findAllByDateBetween(LocalDate startDate, LocalDate endDate);
}