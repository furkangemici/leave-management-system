package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.HolidayTemplateResponse;
import com.cozumtr.leave_management_system.entities.HolidayTemplate;
import com.cozumtr.leave_management_system.repository.HolidayTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HolidayTemplateService {
    
    private final HolidayTemplateRepository templateRepository;
    
    public List<HolidayTemplateResponse> getAllTemplates() {
        return templateRepository.findAllByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private HolidayTemplateResponse mapToResponse(HolidayTemplate template) {
        return HolidayTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .code(template.getCode())
                .durationDays(template.getDurationDays())
                .isHalfDayBefore(template.getIsHalfDayBefore())
                .isMovable(template.getIsMovable())
                .fixedDate(template.getFixedDate())
                .build();
    }
}
