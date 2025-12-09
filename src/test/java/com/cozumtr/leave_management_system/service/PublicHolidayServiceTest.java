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
        testPublicHoliday.setDate(futureDate);
        testPublicHoliday.setName("Yeni Yıl");
        testPublicHoliday.setHalfDay(false);
        testPublicHoliday.setIsActive(true);

        createRequest = PublicHolidayCreateRequest.builder()
                .date(futureDate)
                .name("Cumhuriyet Bayramı")
                .isHalfDay(false)
                .build();

        updateRequest = PublicHolidayUpdateRequest.builder()
                .date(futureDate.plusDays(10))
                .name("Güncellenmiş Tatil")
                .isHalfDay(true)
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("createPublicHoliday - Başarılı oluşturma")
    void createPublicHoliday_Success() {
        // Arrange
        when(publicHolidayRepository.existsByDate(createRequest.getDate())).thenReturn(false);
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenAnswer(invocation -> {
            PublicHoliday saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        PublicHolidayResponse response = publicHolidayService.createPublicHoliday(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(createRequest.getDate(), response.getDate());
        assertEquals(createRequest.getName(), response.getName());
        assertEquals(createRequest.getIsHalfDay(), response.getIsHalfDay());
        verify(publicHolidayRepository).existsByDate(createRequest.getDate());
        verify(publicHolidayRepository).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("createPublicHoliday - Geçmiş tarih kontrolü başarısız")
    void createPublicHoliday_PastDate_ShouldThrowException() {
        // Arrange
        createRequest.setDate(pastDate);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.createPublicHoliday(createRequest);
        });

        assertEquals("Geçmiş bir tarih için resmi tatil oluşturulamaz: " + pastDate, exception.getMessage());
        verify(publicHolidayRepository, never()).existsByDate(any());
        verify(publicHolidayRepository, never()).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("createPublicHoliday - Bugünün tarihi kabul edilmeli")
    void createPublicHoliday_TodayDate_Success() {
        // Arrange
        LocalDate today = LocalDate.now();
        createRequest.setDate(today);
        when(publicHolidayRepository.existsByDate(today)).thenReturn(false);
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenAnswer(invocation -> {
            PublicHoliday saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        PublicHolidayResponse response = publicHolidayService.createPublicHoliday(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(today, response.getDate());
        verify(publicHolidayRepository).existsByDate(today);
        verify(publicHolidayRepository).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("createPublicHoliday - Date unique kontrolü başarısız")
    void createPublicHoliday_DuplicateDate_ShouldThrowException() {
        // Arrange
        when(publicHolidayRepository.existsByDate(createRequest.getDate())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.createPublicHoliday(createRequest);
        });

        assertEquals("Bu tarih için zaten bir resmi tatil kaydı mevcut: " + createRequest.getDate(), exception.getMessage());
        verify(publicHolidayRepository).existsByDate(createRequest.getDate());
        verify(publicHolidayRepository, never()).save(any(PublicHoliday.class));
    }

    // ========== READ TESTS ==========

    @Test
    @DisplayName("getAllPublicHolidays - Tüm resmi tatilleri getirir")
    void getAllPublicHolidays_Success() {
        // Arrange
        PublicHoliday holiday2 = new PublicHoliday();
        holiday2.setId(2L);
        holiday2.setDate(futureDate.plusDays(20));
        holiday2.setName("İşçi Bayramı");
        holiday2.setHalfDay(false);
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
        assertEquals(testPublicHoliday.getDate(), response.getDate());
        assertEquals(testPublicHoliday.getName(), response.getName());
        assertEquals(testPublicHoliday.isHalfDay(), response.getIsHalfDay());
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
        when(publicHolidayRepository.findByDate(updateRequest.getDate())).thenReturn(Optional.empty());
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenReturn(testPublicHoliday);

        // Act
        PublicHolidayResponse response = publicHolidayService.updatePublicHoliday(1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(publicHolidayRepository).findById(1L);
        verify(publicHolidayRepository).findByDate(updateRequest.getDate());
        verify(publicHolidayRepository).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("updatePublicHoliday - Aynı tarihle güncelleme (kendi ID'si)")
    void updatePublicHoliday_SameDate_Success() {
        // Arrange
        updateRequest.setDate(testPublicHoliday.getDate());
        when(publicHolidayRepository.findById(1L)).thenReturn(Optional.of(testPublicHoliday));
        when(publicHolidayRepository.findByDate(testPublicHoliday.getDate())).thenReturn(Optional.of(testPublicHoliday));
        when(publicHolidayRepository.save(any(PublicHoliday.class))).thenReturn(testPublicHoliday);

        // Act
        PublicHolidayResponse response = publicHolidayService.updatePublicHoliday(1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(publicHolidayRepository).findById(1L);
        verify(publicHolidayRepository).findByDate(testPublicHoliday.getDate());
        verify(publicHolidayRepository).save(any(PublicHoliday.class));
    }

    @Test
    @DisplayName("updatePublicHoliday - Başka bir tatilin tarihiyle güncelleme (duplicate)")
    void updatePublicHoliday_DuplicateDate_ShouldThrowException() {
        // Arrange
        PublicHoliday otherHoliday = new PublicHoliday();
        otherHoliday.setId(2L);
        otherHoliday.setDate(updateRequest.getDate());

        when(publicHolidayRepository.findById(1L)).thenReturn(Optional.of(testPublicHoliday));
        when(publicHolidayRepository.findByDate(updateRequest.getDate())).thenReturn(Optional.of(otherHoliday));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            publicHolidayService.updatePublicHoliday(1L, updateRequest);
        });

        assertEquals("Bu tarih için zaten bir resmi tatil kaydı mevcut: " + updateRequest.getDate(), exception.getMessage());
        verify(publicHolidayRepository).findById(1L);
        verify(publicHolidayRepository).findByDate(updateRequest.getDate());
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

