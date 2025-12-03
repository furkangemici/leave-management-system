package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.LeaveTypeRequestDto;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MetadataServiceTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private PublicHolidayRepository publicHolidayRepository;

    @InjectMocks
    private MetadataService metadataService;

    // 1. TEST: Yeni İzin Türü Oluşturma (Mapping ve Enum Dönüşümü)
    @Test
    public void createLeaveType_ShouldMapDtoToEntityCorrectly() {
        // HAZIRLIK
        LeaveTypeRequestDto dto = new LeaveTypeRequestDto();
        dto.setName("Yıllık İzin");
        dto.setIsPaid(true);
        dto.setDeductsFromAnnual(true);
        dto.setWorkflowDefinition("ROLE_HR");
        dto.setRequestUnit("DAY"); // String olarak geliyor

        // Repository save metoduna ne gelirse onu geri dönsün
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(i -> i.getArguments()[0]);

        // EYLEM
        LeaveType result = metadataService.createLeaveType(dto);

        // KONTROL
        Assertions.assertEquals("Yıllık İzin", result.getName());
        Assertions.assertTrue(result.getIsPaid());
        Assertions.assertTrue(result.getIsActive()); // Varsayılan true olmalı

        // KRİTİK KONTROL: String "DAY", Enum RequestUnit.DAY oldu mu?
        Assertions.assertEquals(RequestUnit.DAY, result.getRequestUnit());
    }

    // 2. TEST: İzin Türü Silme (Soft Delete Kontrolü)
    @Test
    public void deleteLeaveType_ShouldPerformSoftDelete() {
        // HAZIRLIK
        Long id = 1L;
        LeaveType existingType = new LeaveType();
        existingType.setId(id);
        existingType.setIsActive(true); // Şu an aktif

        // Find çağrılınca bunu bul
        when(leaveTypeRepository.findById(id)).thenReturn(Optional.of(existingType));

        // EYLEM
        metadataService.deleteLeaveType(id);

        // KONTROL
        // Repository'nin save metodu çağrıldı mı? Ve çağrılan objenin isActive'i false mu?
        ArgumentCaptor<LeaveType> captor = ArgumentCaptor.forClass(LeaveType.class);
        verify(leaveTypeRepository).save(captor.capture());

        LeaveType savedType = captor.getValue();
        Assertions.assertFalse(savedType.getIsActive()); // False olmuş olmalı!
    }
}