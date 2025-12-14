package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    
    /**
     * Belirli bir yıla ait aktif tatilleri getirir.
     */
    List<PublicHoliday> findAllByYearAndIsActiveTrue(Integer year);
    
    /**
     * Belirli yıl aralığındaki aktif tatilleri getirir.
     */
    List<PublicHoliday> findAllByYearBetweenAndIsActiveTrue(Integer startYear, Integer endYear);
    
    /**
     * Verilen tarih aralığında kesişen tatilleri getirir.
     */
    @Query("SELECT ph FROM PublicHoliday ph WHERE ph.isActive = true AND " +
           "((ph.startDate BETWEEN :startDate AND :endDate) OR " +
           "(ph.endDate BETWEEN :startDate AND :endDate) OR " +
           "(ph.startDate <= :startDate AND ph.endDate >= :endDate))")
    List<PublicHoliday> findHolidaysInRange(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);
    
    /**
     * Belirli bir tarihin resmi tatil olup olmadığını kontrol eder.
     */
    @Query("SELECT CASE WHEN COUNT(ph) > 0 THEN true ELSE false END FROM PublicHoliday ph " +
           "WHERE ph.isActive = true AND :date BETWEEN ph.startDate AND ph.endDate")
    boolean existsByDateInRange(@Param("date") LocalDate date);
    
    /**
     * Belirli bir tarihe göre resmi tatil getirir.
     */
    @Query("SELECT ph FROM PublicHoliday ph WHERE ph.isActive = true AND " +
           ":date BETWEEN ph.startDate AND ph.endDate")
    Optional<PublicHoliday> findByDateInRange(@Param("date") LocalDate date);
}
