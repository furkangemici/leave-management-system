package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.LeaveTypeResponse;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private LeaveTypeResponse mapToResponse(LeaveType leaveType) {
        return LeaveTypeResponse.builder()
                .id(leaveType.getId())
                .name(leaveType.getName())
                .isPaid(leaveType.isPaid())
                .requestUnit(leaveType.getRequestUnit().name()) // "DAY" veya "HOUR"
                .build();
    }
}
