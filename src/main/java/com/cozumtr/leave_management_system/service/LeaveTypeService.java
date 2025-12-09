package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.LeaveTypeCreateRequest;
import com.cozumtr.leave_management_system.dto.request.LeaveTypeUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveTypeResponse;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    /**
     * Aktif tüm izin türlerini listeler.
     * Frontend'de dropdown, radio button, card vb. için kullanılır.
     */
    public List<LeaveTypeResponse> getAllActiveLeaveTypes() {
        return leaveTypeRepository.findAll().stream()
                .filter(LeaveType::getIsActive) // Sadece aktif olanları getir
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tüm izin türlerini listeler (aktif ve pasif).
     */
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * ID'ye göre izin türü getirir.
     */
    public LeaveTypeResponse getLeaveTypeById(Long id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("İzin türü bulunamadı: " + id));
        return mapToResponse(leaveType);
    }

    /**
     * Yeni izin türü oluşturur.
     * Name alanının unique olmasını kontrol eder.
     */
    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeCreateRequest request) {
        // Name unique kontrolü
        if (leaveTypeRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("Bu isimde bir izin türü zaten mevcut: " + request.getName());
        }

        LeaveType leaveType = new LeaveType();
        leaveType.setName(request.getName());
        leaveType.setPaid(request.getIsPaid());
        leaveType.setDeductsFromAnnual(request.getDeductsFromAnnual());
        leaveType.setWorkflowDefinition(request.getWorkflowDefinition());
        leaveType.setRequestUnit(request.getRequestUnit());
        leaveType.setIsActive(true);

        LeaveType saved = leaveTypeRepository.save(leaveType);
        return mapToResponse(saved);
    }

    /**
     * İzin türü günceller.
     * Name alanının unique olmasını kontrol eder (kendi ID'si hariç).
     */
    @Transactional
    public LeaveTypeResponse updateLeaveType(Long id, LeaveTypeUpdateRequest request) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("İzin türü bulunamadı: " + id));

        // Name unique kontrolü (kendi ID'si hariç)
        leaveTypeRepository.findByName(request.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException("Bu isimde bir izin türü zaten mevcut: " + request.getName());
                    }
                });

        leaveType.setName(request.getName());
        leaveType.setPaid(request.getIsPaid());
        leaveType.setDeductsFromAnnual(request.getDeductsFromAnnual());
        leaveType.setWorkflowDefinition(request.getWorkflowDefinition());
        leaveType.setRequestUnit(request.getRequestUnit());

        LeaveType updated = leaveTypeRepository.save(leaveType);
        return mapToResponse(updated);
    }

    /**
     * İzin türü siler (soft delete - isActive = false).
     */
    @Transactional
    public void deleteLeaveType(Long id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("İzin türü bulunamadı: " + id));

        leaveType.setIsActive(false);
        leaveTypeRepository.save(leaveType);
    }

    private LeaveTypeResponse mapToResponse(LeaveType leaveType) {
        return LeaveTypeResponse.builder()
                .id(leaveType.getId())
                .name(leaveType.getName())
                .isPaid(leaveType.isPaid())
                .deductsFromAnnual(leaveType.isDeductsFromAnnual())
                .workflowDefinition(leaveType.getWorkflowDefinition())
                .requestUnit(leaveType.getRequestUnit() != null ? leaveType.getRequestUnit().name() : null)
                .build();
    }
}
