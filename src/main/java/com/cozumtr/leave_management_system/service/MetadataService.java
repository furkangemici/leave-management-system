package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.LeaveTypeRequestDto;
import com.cozumtr.leave_management_system.dto.request.PublicHolidayRequestDto;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.entities.PublicHoliday;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final PublicHolidayRepository publicHolidayRepository;

    // --- İZİN TÜRLERİ YÖNETİMİ ---

    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    @Transactional
    public LeaveType createLeaveType(LeaveTypeRequestDto dto) {
        LeaveType type = new LeaveType();
        type.setName(dto.getName());

        // DÜZELTME: Artık setIsPaid kullanıyoruz
        type.setIsPaid(dto.getIsPaid());
        type.setDeductsFromAnnual(dto.getDeductsFromAnnual());
        type.setWorkflowDefinition(dto.getWorkflowDefinition());

        // String -> Enum çevrimi
        type.setRequestUnit(RequestUnit.valueOf(dto.getRequestUnit()));

        // DÜZELTME: Eksik olan alan artık geldi ve setIsActive kullanıyoruz
        type.setIsActive(true);

        return leaveTypeRepository.save(type);
    }

    @Transactional
    public void deleteLeaveType(Long id) {
        leaveTypeRepository.findById(id).ifPresent(type -> {
            // DÜZELTME: setIsActive kullanıyoruz
            type.setIsActive(false);
            leaveTypeRepository.save(type);
        });
    }

    // --- RESMİ TATİL YÖNETİMİ ---

    public List<PublicHoliday> getAllHolidays() {
        return publicHolidayRepository.findAll();
    }

    @Transactional
    public PublicHoliday createHoliday(PublicHolidayRequestDto dto) {
        PublicHoliday holiday = new PublicHoliday();
        holiday.setName(dto.getName());

        // Eğer PublicHoliday sınıfın primitive boolean kaldıysa burası setHalfDay kalabilir
        // Ama wrapper Boolean ise setIsHalfDay olmalıdır.
        // Senin son attığın PublicHoliday primitive idi, o yüzden burayı primitive bırakıyorum:
        holiday.setDate(dto.getDate());
        holiday.setHalfDay(dto.getIsHalfDay());

        return publicHolidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        publicHolidayRepository.deleteById(id);
    }
}