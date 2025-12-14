package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.BulkHolidayCreateRequest;
import com.cozumtr.leave_management_system.dto.request.PublicHolidayCreateRequest;
import com.cozumtr.leave_management_system.dto.request.PublicHolidayUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.PublicHolidayResponse;
import com.cozumtr.leave_management_system.entities.HolidayTemplate;
import com.cozumtr.leave_management_system.entities.PublicHoliday;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.HolidayTemplateRepository;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicHolidayService {

    private final PublicHolidayRepository publicHolidayRepository;
    private final HolidayTemplateRepository holidayTemplateRepository;

    /**
     * Tüm resmi tatilleri listeler.
     */
    public List<PublicHolidayResponse> getAllPublicHolidays() {
        return publicHolidayRepository.findAll().stream()
                .filter(PublicHoliday::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Belirli bir yıla ait resmi tatilleri getirir.
     */
    public List<PublicHolidayResponse> getHolidaysByYear(Integer year) {
        return publicHolidayRepository.findAllByYearAndIsActiveTrue(year).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Yaklaşan resmi tatilleri getirir (bugünden itibaren 90 gün içinde).
     */
    public List<PublicHolidayResponse> getUpcomingHolidays() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(90);

        return publicHolidayRepository.findAll().stream()
                .filter(PublicHoliday::getIsActive)
                .filter(h -> !h.getStartDate().isBefore(today) && !h.getStartDate().isAfter(endDate))
                .sorted((h1, h2) -> h1.getStartDate().compareTo(h2.getStartDate()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * ID'ye göre resmi tatil getirir.
     */
    public PublicHolidayResponse getPublicHolidayById(Long id) {
        PublicHoliday publicHoliday = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Resmi tatil bulunamadı: " + id));
        return mapToResponse(publicHoliday);
    }

    /**
     * Yeni resmi tatil oluşturur.
     */
    @Transactional
    public PublicHolidayResponse createPublicHoliday(PublicHolidayCreateRequest request) {
        // Geçmiş tarih kontrolü
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Geçmiş bir tarih için resmi tatil oluşturulamaz: " + request.getStartDate());
        }

        // Tarih çakışma kontrolü
        if (publicHolidayRepository.existsByDateInRange(request.getStartDate())) {
            throw new BusinessException("Bu tarih için zaten bir resmi tatil kaydı mevcut: " + request.getStartDate());
        }

        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : request.getStartDate();

        PublicHoliday publicHoliday = PublicHoliday.builder()
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(endDate)
                .year(request.getStartDate().getYear())
                .isHalfDay(request.getIsHalfDay())
                .build();
        publicHoliday.setIsActive(true);

        PublicHoliday saved = publicHolidayRepository.save(publicHoliday);
        return mapToResponse(saved);
    }

    /**
     * Toplu resmi tatil oluşturur (şablon tabanlı).
     */
    @Transactional
    public List<PublicHolidayResponse> createBulkHolidays(BulkHolidayCreateRequest request) {
        List<PublicHoliday> holidays = new ArrayList<>();

        for (BulkHolidayCreateRequest.HolidayDateMapping mapping : request.getHolidays()) {
            HolidayTemplate template = holidayTemplateRepository.findById(mapping.getTemplateId())
                    .orElseThrow(() -> new BusinessException("Tatil şablonu bulunamadı: " + mapping.getTemplateId()));

            LocalDate startDate = mapping.getStartDate();
            LocalDate endDate = mapping.getEndDate() != null ? mapping.getEndDate() : startDate;

            // Arife günü varsa, bir gün öncesini yarım gün tatil olarak ekle
            if (template.getIsHalfDayBefore()) {
                PublicHoliday halfDayHoliday = PublicHoliday.builder()
                        .template(template)
                        .name(template.getName() + " Arife - " + request.getYear())
                        .startDate(startDate.minusDays(1))
                        .endDate(startDate.minusDays(1))
                        .year(request.getYear())
                        .isHalfDay(true)
                        .build();
                halfDayHoliday.setIsActive(true);
                holidays.add(halfDayHoliday);
            }

            // Ana tatil
            PublicHoliday holiday = PublicHoliday.builder()
                    .template(template)
                    .name(template.getName() + " " + request.getYear())
                    .startDate(startDate)
                    .endDate(endDate)
                    .year(request.getYear())
                    .isHalfDay(false)
                    .build();
            holiday.setIsActive(true);
            holidays.add(holiday);
        }

        List<PublicHoliday> saved = publicHolidayRepository.saveAll(holidays);
        return saved.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Resmi tatil günceller.
     */
    @Transactional
    public PublicHolidayResponse updatePublicHoliday(Long id, PublicHolidayUpdateRequest request) {
        PublicHoliday publicHoliday = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Resmi tatil bulunamadı: " + id));

        // Tarih çakışma kontrolü (kendi ID'si hariç)
        publicHolidayRepository.findByDateInRange(request.getStartDate())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException("Bu tarih için zaten bir resmi tatil kaydı mevcut: " + request.getStartDate());
                    }
                });

        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : request.getStartDate();

        publicHoliday.setName(request.getName());
        publicHoliday.setStartDate(request.getStartDate());
        publicHoliday.setEndDate(endDate);
        publicHoliday.setYear(request.getStartDate().getYear());
        publicHoliday.setIsHalfDay(request.getIsHalfDay());

        PublicHoliday updated = publicHolidayRepository.save(publicHoliday);
        return mapToResponse(updated);
    }

    /**
     * Resmi tatil siler (soft delete - isActive = false).
     */
    @Transactional
    public void deletePublicHoliday(Long id) {
        PublicHoliday publicHoliday = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Resmi tatil bulunamadı: " + id));

        publicHoliday.setIsActive(false);
        publicHolidayRepository.save(publicHoliday);
    }

    private PublicHolidayResponse mapToResponse(PublicHoliday publicHoliday) {
        long durationDays = ChronoUnit.DAYS.between(publicHoliday.getStartDate(), publicHoliday.getEndDate()) + 1;

        return PublicHolidayResponse.builder()
                .id(publicHoliday.getId())
                .name(publicHoliday.getName())
                .startDate(publicHoliday.getStartDate())
                .endDate(publicHoliday.getEndDate())
                .year(publicHoliday.getYear())
                .durationDays((int) durationDays)
                .isHalfDay(publicHoliday.getIsHalfDay())
                .build();
    }
}


