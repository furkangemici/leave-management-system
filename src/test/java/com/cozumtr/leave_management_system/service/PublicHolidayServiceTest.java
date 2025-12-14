package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.request.PublicHolidayCreateRequest;
import com.cozumtr.leave_management_system.dto.request.PublicHolidayUpdateRequest;
import com.cozumtr.leave_management_system.dto.response.PublicHolidayResponse;
import com.cozumtr.leave_management_system.entities.PublicHoliday;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.PublicHolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicHolidayService Unit Tests")
class PublicHolidayServiceTest {

    @Mock
    private PublicHolidayRepository publicHolidayRepository;

    @InjectMocks
    private PublicHolidayService publicHolidayService;

    private PublicHoliday testPublicHoliday;
    private PublicHolidayCreateRequest createRequest;
    private PublicHolidayUpdateRequest updateRequest;
    private LocalDate futureDate;
    private LocalDate pastDate;

    @BeforeEach
    void setUp() {
        futureDate = LocalDate.now().plusDays(30);
        pastDate = LocalDate.now().minusDays(10);

        testPublicHoliday = new PublicHoliday();
        testPublicHoliday.setId(1L);
        testPublicHoliday.setStartDate(futureDate);
        testPublicHoliday.setEndDate(futureDate);
        testPublicHoliday.setYear(futureDate.getYear());
        testPublicHoliday.setName("Yeni Yıl");
        testPublicHoliday.setIsHalfDay(false);
        testPublicHoliday.setIsActive(true);

        createRequest = PublicHolidayCreateRequest.builder()
                .startDate(futureDate)
                .name("Cumhuriyet Bayramı")
                .isHalfDay(false)
                .build();

        updateRequest = PublicHolidayUpdateRequest.builder()
                .startDate(futureDate.plusDays(10))
                .name("Güncellenmiş Tatil")
                .isHalfDay(true)
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("createPublicHoliday - Başarılı oluşturma")
    void createPublicHoliday_Success() {
        // Arrange
        when(publicHolidayRepository.existsByDateInRange(createRequest.getStartDate())).thenReturn(false);
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenAnswer(invocation -> {
            PublicHoliday saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        PublicHolidayResponse response = publicHolidayService.createPublicHoliday(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(createRequest.getStartDate(), response.getStartDate());
        assertEquals(createRequest.getName(), response.getName());
        assertEquals(createRequest.getIsHalfDay(), response.getIsHalfDay());
        verify(publicHolidayRepository).existsByDateInRange(createRequest.getStartDate());
        verify(publicHolidayRepository).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("createPublicHoliday - Geçmiş tarih kontrolü başarısız")
    void createPublicHoliday_PastDate_ShouldThrowException() {
        // Arrange
        createRequest.setStartDate(pastDate);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.createPublicHoliday(createRequest);
        });

        assertEquals("Geçmiş bir tarih için resmi tatil oluşturulamaz: " + pastDate, exception.getMessage());
        verify(publicHolidayRepository, never()).existsByDateInRange(any());
        verify(publicHolidayRepository, never()).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("createPublicHoliday - Bugünün tarihi kabul edilmeli")
    void createPublicHoliday_TodayDate_Success() {
        // Arrange
        LocalDate today = LocalDate.now();
        createRequest.setStartDate(today);
        when(publicHolidayRepository.existsByDateInRange(today)).thenReturn(false);
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenAnswer(invocation -> {
            PublicHoliday saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        PublicHolidayResponse response = publicHolidayService.createPublicHoliday(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(today, response.getStartDate());
        verify(publicHolidayRepository).existsByDateInRange(today);
        verify(publicHolidayRepository).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("createPublicHoliday - Date unique kontrolü başarısız")
    void createPublicHoliday_DuplicateDate_ShouldThrowException() {
        // Arrange
        when(publicHolidayRepository.existsByDateInRange(createRequest.getStartDate())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.createPublicHoliday(createRequest);
        });

        assertEquals("Bu tarih için zaten bir resmi tatil kaydı mevcut: " + createRequest.getStartDate(), exception.getMessage());
        verify(publicHolidayRepository).existsByDateInRange(createRequest.getStartDate());
        verify(publicHolidayRepository, never()).save(any(PublicHoliday.class));
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("getAllPublicHolidays - Tüm resmi tatilleri getirir")
    void getAllPublicHolidays_Success() {
        // Arrange
        PublicHoliday holiday2 = new PublicHoliday();
        holiday2.setId(2L);
        holiday2.setStartDate(futureDate.plusDays(20));
        holiday2.setEndDate(futureDate.plusDays(20));
        holiday2.setYear(futureDate.plusDays(20).getYear());
        holiday2.setName("İşçi Bayramı");
        holiday2.setIsHalfDay(false);
        holiday2.setIsActive(true);

        when(publicHolidayRepository.findAll()).thenReturn(Arrays.asList(testPublicHoliday, holiday2));

        // Act
        List<PublicHolidayResponse> responses = publicHolidayService.getAllPublicHolidays();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(publicHolidayRepository).findAll();
    }

    @Test
    @DisplayName("getPublicHolidayById - Başarılı getirme")
    void getPublicHolidayById_Success() {
        // Arrange
        when(publicHolidayRepository.findById(1L)).thenReturn(Optional.of(testPublicHoliday));

        // Act
        PublicHolidayResponse response = publicHolidayService.getPublicHolidayById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testPublicHoliday.getId(), response.getId());
        assertEquals(testPublicHoliday.getStartDate(), response.getStartDate());
        assertEquals(testPublicHoliday.getName(), response.getName());
        assertEquals(testPublicHoliday.getIsHalfDay(), response.getIsHalfDay());
        verify(publicHolidayRepository).findById(1L);
    }

    @Test
    @DisplayName("getPublicHolidayById - Bulunamayan ID için exception")
    void getPublicHolidayById_NotFound_ShouldThrowException() {
        // Arrange
        when(publicHolidayRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.getPublicHolidayById(999L);
        });

        assertEquals("Resmi tatil bulunamadı: 999", exception.getMessage());
        verify(publicHolidayRepository).findById(999L);
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("updatePublicHoliday - Başarılı güncelleme")
    void updatePublicHoliday_Success() {
        // Arrange
        when(publicHolidayRepository.findById(1L)).thenReturn(Optional.of(testPublicHoliday));
        when(publicHolidayRepository.findByDateInRange(updateRequest.getStartDate())).thenReturn(Optional.empty());
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenReturn(testPublicHoliday);

        // Act
        PublicHolidayResponse response = publicHolidayService.updatePublicHoliday(1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(publicHolidayRepository).findById(1L);
        verify(publicHolidayRepository).findByDateInRange(updateRequest.getStartDate());
        verify(publicHolidayRepository).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("updatePublicHoliday - Aynı tarihle güncelleme (kendi ID'si)")
    void updatePublicHoliday_SameDate_Success() {
        // Arrange
        updateRequest.setStartDate(testPublicHoliday.getStartDate());
        when(publicHolidayRepository.findById(1L)).thenReturn(Optional.of(testPublicHoliday));
        when(publicHolidayRepository.findByDateInRange(testPublicHoliday.getStartDate())).thenReturn(Optional.of(testPublicHoliday));
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenReturn(testPublicHoliday);

        // Act
        PublicHolidayResponse response = publicHolidayService.updatePublicHoliday(1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(publicHolidayRepository).findById(1L);
        verify(publicHolidayRepository).findByDateInRange(testPublicHoliday.getStartDate());
        verify(publicHolidayRepository).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("updatePublicHoliday - Başka bir tatilin tarihiyle güncelleme (duplicate)")
    void updatePublicHoliday_DuplicateDate_ShouldThrowException() {
        // Arrange
        PublicHoliday otherHoliday = new PublicHoliday();
        otherHoliday.setId(2L);
        otherHoliday.setStartDate(updateRequest.getStartDate());
        otherHoliday.setEndDate(updateRequest.getStartDate());

        when(publicHolidayRepository.findById(1L)).thenReturn(Optional.of(testPublicHoliday));
        when(publicHolidayRepository.findByDateInRange(updateRequest.getStartDate())).thenReturn(Optional.of(otherHoliday));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.updatePublicHoliday(1L, updateRequest);
        });

        assertEquals("Bu tarih için zaten bir resmi tatil kaydı mevcut: " + updateRequest.getStartDate(), exception.getMessage());
        verify(publicHolidayRepository).findById(1L);
        verify(publicHolidayRepository).findByDateInRange(updateRequest.getStartDate());
        verify(publicHolidayRepository, never()).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("updatePublicHoliday - Bulunamayan ID için exception")
    void updatePublicHoliday_NotFound_ShouldThrowException() {
        // Arrange
        when(publicHolidayRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.updatePublicHoliday(999L, updateRequest);
        });

        assertEquals("Resmi tatil bulunamadı: 999", exception.getMessage());
        verify(publicHolidayRepository).findById(999L);
        verify(publicHolidayRepository, never()).save(any(PublicHoliday.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("deletePublicHoliday - Başarılı silme (soft delete)")
    void deletePublicHoliday_Success() {
        // Arrange
        when(publicHolidayRepository.findById(1L)).thenReturn(Optional.of(testPublicHoliday));
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenReturn(testPublicHoliday);

        // Act
        publicHolidayService.deletePublicHoliday(1L);

        // Assert
        verify(publicHolidayRepository).findById(1L);
        verify(publicHolidayRepository).save(argThat(holiday -> !holiday.getIsActive()));
    }

    @Test
    @DisplayName("deletePublicHoliday - Bulunamayan ID için exception")
    void deletePublicHoliday_NotFound_ShouldThrowException() {
        // Arrange
        when(publicHolidayRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.deletePublicHoliday(999L);
        });

        assertEquals("Resmi tatil bulunamadı: 999", exception.getMessage());
        verify(publicHolidayRepository).findById(999L);
        verify(publicHolidayRepository, never()).save(any(PublicHoliday.class));
    }
}


