package com.cozumtr.leave_management_system.repository;

import com.cozumtr.leave_management_system.entities.HolidayTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayTemplateRepository extends JpaRepository<HolidayTemplate, Long> {
    List<HolidayTemplate> findAllByIsActiveTrue();
    Optional<HolidayTemplate> findByCode(String code);
}
