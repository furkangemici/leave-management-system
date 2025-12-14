package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.LeaveTypeCreateRequest;
import com.cozumtr.leave_management_system.dto.request.LeaveTypeUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.LeaveTypeResponse;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveTypeService Unit Tests")
class LeaveTypeServiceTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @InjectMocks
    private LeaveTypeService leaveTypeService;

    private LeaveType testLeaveType;
    private LeaveTypeCreateRequest createRequest;
    private LeaveTypeUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testLeaveType = new LeaveType();
        testLeaveType.setId(1L);
        testLeaveType.setName("Yıllık İzin");
        testLeaveType.setPaid(true);
        testLeaveType.setDeductsFromAnnual(true);
        testLeaveType.setDocumentRequired(false);
        testLeaveType.setWorkflowDefinition("ROLE_MANAGER,ROLE_HR");
        testLeaveType.setRequestUnit(RequestUnit.DAY);
        testLeaveType.setIsActive(true);

        createRequest = LeaveTypeCreateRequest.builder()
                .name("Mazeret İzni")
                .isPaid(false)
                .deductsFromAnnual(false)
                .documentRequired(true)
                .workflowDefinition("ROLE_MANAGER")
                .requestUnit(RequestUnit.HOUR)
                .build();

        updateRequest = LeaveTypeUpdateRequest.builder()
                .name("Güncellenmiş İzin")
                .isPaid(true)
                .deductsFromAnnual(true)
                .documentRequired(false)
                .workflowDefinition("ROLE_MANAGER")
                .requestUnit(RequestUnit.DAY)
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("createLeaveType - Başarılı oluşturma")
    void createLeaveType_Success() {
        // Arrange
        when(leaveTypeRepository.findByName(createRequest.getName())).thenReturn(Optional.empty());
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> {
            LeaveType saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        LeaveTypeResponse response = leaveTypeService.createLeaveType(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(createRequest.getName(), response.getName());
        assertEquals(createRequest.getIsPaid(), response.getIsPaid());
        assertEquals(createRequest.getDeductsFromAnnual(), response.getDeductsFromAnnual());
        assertEquals(createRequest.getDocumentRequired(), response.getDocumentRequired());
        assertEquals(createRequest.getWorkflowDefinition(), response.getWorkflowDefinition());
        assertEquals(createRequest.getRequestUnit().name(), response.getRequestUnit());
        verify(leaveTypeRepository).findByName(createRequest.getName());
        verify(leaveTypeRepository).save(any(LeaveType.class));
    }

    @Test
    @DisplayName("createLeaveType - Name unique kontrolü başarısız")
    void createLeaveType_DuplicateName_ShouldThrowException() {
        // Arrange
        when(leaveTypeRepository.findByName(createRequest.getName())).thenReturn(Optional.of(testLeaveType));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveTypeService.createLeaveType(createRequest);
        });

        assertEquals("Bu isimde bir izin türü zaten mevcut: " + createRequest.getName(), exception.getMessage());
        verify(leaveTypeRepository).findByName(createRequest.getName());
        verify(leaveTypeRepository, never()).save(any(LeaveType.class));
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("getAllLeaveTypes - Tüm izin türlerini getirir")
    void getAllLeaveTypes_Success() {
        // Arrange
        LeaveType leaveType2 = new LeaveType();
        leaveType2.setId(2L);
        leaveType2.setName("Hastalık İzni");
        leaveType2.setPaid(true);
        leaveType2.setDeductsFromAnnual(true);
        leaveType2.setRequestUnit(RequestUnit.DAY);
        leaveType2.setIsActive(true);

        when(leaveTypeRepository.findAll()).thenReturn(Arrays.asList(testLeaveType, leaveType2));

        // Act
        List<LeaveTypeResponse> responses = leaveTypeService.getAllLeaveTypes();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(leaveTypeRepository).findAll();
    }

    @Test
    @DisplayName("getAllActiveLeaveTypes - Sadece aktif izin türlerini getirir")
    void getAllActiveLeaveTypes_OnlyActive() {
        // Arrange
        LeaveType inactiveLeaveType = new LeaveType();
        inactiveLeaveType.setId(2L);
        inactiveLeaveType.setName("Pasif İzin");
        inactiveLeaveType.setIsActive(false);

        when(leaveTypeRepository.findAll()).thenReturn(Arrays.asList(testLeaveType, inactiveLeaveType));

        // Act
        List<LeaveTypeResponse> responses = leaveTypeService.getAllActiveLeaveTypes();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testLeaveType.getName(), responses.get(0).getName());
        verify(leaveTypeRepository).findAll();
    }

    @Test
    @DisplayName("getLeaveTypeById - Başarılı getirme")
    void getLeaveTypeById_Success() {
        // Arrange
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));

        // Act
        LeaveTypeResponse response = leaveTypeService.getLeaveTypeById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testLeaveType.getId(), response.getId());
        assertEquals(testLeaveType.getName(), response.getName());
        assertEquals(testLeaveType.isDocumentRequired(), response.getDocumentRequired());
        verify(leaveTypeRepository).findById(1L);
    }

    @Test
    @DisplayName("getLeaveTypeById - Bulunamayan ID için exception")
    void getLeaveTypeById_NotFound_ShouldThrowException() {
        // Arrange
        when(leaveTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveTypeService.getLeaveTypeById(999L);
        });

        assertEquals("İzin türü bulunamadı: 999", exception.getMessage());
        verify(leaveTypeRepository).findById(999L);
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("updateLeaveType - Başarılı güncelleme")
    void updateLeaveType_Success() {
        // Arrange
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveTypeRepository.findByName(updateRequest.getName())).thenReturn(Optional.empty());
        when(leaveTypeRepository.save(any(LeaveType.class))).thenReturn(testLeaveType);

        // Act
        LeaveTypeResponse response = leaveTypeService.updateLeaveType(1L, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(updateRequest.getName(), response.getName());
        assertEquals(updateRequest.getIsPaid(), response.getIsPaid());
        assertEquals(updateRequest.getDeductsFromAnnual(), response.getDeductsFromAnnual());
        assertEquals(updateRequest.getDocumentRequired(), response.getDocumentRequired());
        assertEquals(updateRequest.getWorkflowDefinition(), response.getWorkflowDefinition());
        assertEquals(updateRequest.getRequestUnit().name(), response.getRequestUnit());
        verify(leaveTypeRepository).findById(1L);
        verify(leaveTypeRepository).findByName(updateRequest.getName());
        verify(leaveTypeRepository).save(any(LeaveType.class));
    }

    @Test
    @DisplayName("updateLeaveType - Aynı isimle güncelleme (kendi ID'si)")
    void updateLeaveType_SameName_Success() {
        // Arrange
        updateRequest.setName(testLeaveType.getName()); // Aynı isim
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveTypeRepository.findByName(testLeaveType.getName())).thenReturn(Optional.of(testLeaveType));
        when(leaveTypeRepository.save(any(LeaveType.class))).thenReturn(testLeaveType);

        // Act
        LeaveTypeResponse response = leaveTypeService.updateLeaveType(1L, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testLeaveType.isDocumentRequired(), response.getDocumentRequired());
        verify(leaveTypeRepository).findById(1L);
        verify(leaveTypeRepository).findByName(testLeaveType.getName());
        verify(leaveTypeRepository).save(any(LeaveType.class));
    }

    @Test
    @DisplayName("updateLeaveType - Başka bir izin türünün ismiyle güncelleme (duplicate)")
    void updateLeaveType_DuplicateName_ShouldThrowException() {
        // Arrange
        LeaveType otherLeaveType = new LeaveType();
        otherLeaveType.setId(2L);
        otherLeaveType.setName("Başka İzin");

        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveTypeRepository.findByName(updateRequest.getName())).thenReturn(Optional.of(otherLeaveType));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveTypeService.updateLeaveType(1L, updateRequest);
        });

        assertEquals("Bu isimde bir izin türü zaten mevcut: " + updateRequest.getName(), exception.getMessage());
        verify(leaveTypeRepository).findById(1L);
        verify(leaveTypeRepository).findByName(updateRequest.getName());
        verify(leaveTypeRepository, never()).save(any(LeaveType.class));
    }

    @Test
    @DisplayName("updateLeaveType - Bulunamayan ID için exception")
    void updateLeaveType_NotFound_ShouldThrowException() {
        // Arrange
        when(leaveTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveTypeService.updateLeaveType(999L, updateRequest);
        });

        assertEquals("İzin türü bulunamadı: 999", exception.getMessage());
        verify(leaveTypeRepository).findById(999L);
        verify(leaveTypeRepository, never()).save(any(LeaveType.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deleteLeaveType - Başarılı silme (soft delete)")
    void deleteLeaveType_Success() {
        // Arrange
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(testLeaveType));
        when(leaveTypeRepository.save(any(LeaveType.class))).thenReturn(testLeaveType);

        // Act
        leaveTypeService.deleteLeaveType(1L);

        // Assert
        verify(leaveTypeRepository).findById(1L);
        verify(leaveTypeRepository).save(argThat(leaveType -> !leaveType.getIsActive()));
    }

    @Test
    @DisplayName("deleteLeaveType - Bulunamayan ID için exception")
    void deleteLeaveType_NotFound_ShouldThrowException() {
        // Arrange
        when(leaveTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            leaveTypeService.deleteLeaveType(999L);
        });

        assertEquals("İzin türü bulunamadı: 999", exception.getMessage());
        verify(leaveTypeRepository).findById(999L);
        verify(leaveTypeRepository, never()).save(any(LeaveType.class));
    }
}


