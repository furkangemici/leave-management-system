package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.PublicHolidayCreateRequest;
import com.cozumtr.leave_management_system.dto.request.PublicHolidayUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.PublicHolidayResponse;
import com.cozumtr.leave_management_system.entities.PublicHoliday;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicHolidayService {

    private final PublicHolidayRepository publicHolidayRepository;

    /**
     * Tüm resmi tatilleri listeler.
     */
    public List<PublicHolidayResponse> getAllPublicHolidays() {
        return publicHolidayRepository.findAll().stream()
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
     * Date alanının unique olmasını ve geçmiş bir tarihte olmamasını kontrol eder.
     */
    @Transactional
    public PublicHolidayResponse createPublicHoliday(PublicHolidayCreateRequest request) {
        // Geçmiş tarih kontrolü
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Geçmiş bir tarih için resmi tatil oluşturulamaz: " + request.getDate());
        }

        // Date unique kontrolü
        if (publicHolidayRepository.existsByDate(request.getDate())) {
            throw new BusinessException("Bu tarih için zaten bir resmi tatil kaydı mevcut: " + request.getDate());
        }

        PublicHoliday publicHoliday = new PublicHoliday();
        publicHoliday.setDate(request.getDate());
        publicHoliday.setName(request.getName());
        publicHoliday.setHalfDay(request.getIsHalfDay());
        publicHoliday.setIsActive(true);

        PublicHoliday saved = publicHolidayRepository.save(publicHoliday);
        return mapToResponse(saved);
    }

    /**
     * Resmi tatil günceller.
     * Date alanının unique olmasını kontrol eder (kendi ID'si hariç).
     */
    @Transactional
    public PublicHolidayResponse updatePublicHoliday(Long id, PublicHolidayUpdateRequest request) {
        PublicHoliday publicHoliday = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Resmi tatil bulunamadı: " + id));

        // Date unique kontrolü (kendi ID'si hariç)
        publicHolidayRepository.findByDate(request.getDate())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException("Bu tarih için zaten bir resmi tatil kaydı mevcut: " + request.getDate());
                    }
                });

        publicHoliday.setDate(request.getDate());
        publicHoliday.setName(request.getName());
        publicHoliday.setHalfDay(request.getIsHalfDay());

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
        return PublicHolidayResponse.builder()
                .id(publicHoliday.getId())
                .date(publicHoliday.getDate())
                .name(publicHoliday.getName())
                .isHalfDay(publicHoliday.isHalfDay())
                .build();
    }
}

