package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.LeaveBalanceResponse;
import com.cozumtr.leave_management_system.entities.Employee;
import com.cozumtr.leave_management_system.entities.LeaveEntitlement;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.repository.EmployeeRepository;
import com.cozumtr.leave_management_system.repository.LeaveEntitlementRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveEntitlementService Unit Tests")
class LeaveEntitlementServiceTest {

    @Mock
    private LeaveEntitlementRepository leaveEntitlementRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private LeaveEntitlementService leaveEntitlementService;

    private Employee testEmployee;
    private LeaveType annualLeaveType;
    private LeaveType excuseLeaveType;
    private LeaveEntitlement testEntitlement2024;
    private LeaveEntitlement testEntitlement2025;

    @BeforeEach
    void setUp() {
        // Test Employee - 3 yıl kıdem (2021'de işe başladı)
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setEmail("test@example.com");
        testEmployee.setFirstName("Test");
        testEmployee.setLastName("User");
        testEmployee.setHireDate(LocalDate.of(2021, 1, 15));
        testEmployee.setDailyWorkHours(new BigDecimal("8.0"));

        // Yıllık İzin Türü
        annualLeaveType = new LeaveType();
        annualLeaveType.setId(1L);
        annualLeaveType.setName("Yıllık İzin");
        annualLeaveType.setIsActive(true);
        annualLeaveType.setDeductsFromAnnual(true);

        // Mazeret İzni Türü
        excuseLeaveType = new LeaveType();
        excuseLeaveType.setId(2L);
        excuseLeaveType.setName("Mazeret İzni (Saatlik)");
        excuseLeaveType.setIsActive(true);
        excuseLeaveType.setDeductsFromAnnual(false);

        // 2024 Entitlement
        testEntitlement2024 = new LeaveEntitlement();
        testEntitlement2024.setId(1L);
        testEntitlement2024.setEmployee(testEmployee);
        testEntitlement2024.setYear(2024);
        testEntitlement2024.setTotalHoursEntitled(new BigDecimal("112.0")); // 14 gün × 8 saat
        testEntitlement2024.setHoursUsed(new BigDecimal("80.0"));
        testEntitlement2024.setCarriedForwardHours(BigDecimal.ZERO); // 2024'te aktarılan yok

        // 2025 Entitlement
        testEntitlement2025 = new LeaveEntitlement();
        testEntitlement2025.setId(2L);
        testEntitlement2025.setEmployee(testEmployee);
        testEntitlement2025.setYear(2025);
        testEntitlement2025.setTotalHoursEntitled(new BigDecimal("120.0")); // 14 gün + 4 gün (2024'ten aktarılan)
        testEntitlement2025.setHoursUsed(new BigDecimal("0.0"));
        testEntitlement2025.setCarriedForwardHours(new BigDecimal("32.0")); // 2024'ten aktarılan 4 gün (32 saat)
    }

    // ========== KIDEME GÖRE YILLIK İZİN HESAPLAMA TESTLERİ ==========

    @Test
    @DisplayName("Kıdem 0-1 yıl: Yıllık izin hakedişi 0 gün olmalı")
    void calculateAnnualLeaveEntitlement_LessThan1Year_ShouldReturnZero() {
        // Given
        Employee newEmployee = new Employee();
        // 6 ay kıdem - ChronoUnit.YEARS.between tam yıl sayısını verir, 6 ay = 0 yıl
        newEmployee.setHireDate(LocalDate.now().minusMonths(6));
        newEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        LocalDate referenceDate = LocalDate.now();

        // When
        BigDecimal entitlement = leaveEntitlementService.calculateAnnualLeaveEntitlement(newEmployee, referenceDate);

        // Then
        // 6 ay kıdem = 0 yıl (ChronoUnit.YEARS.between tam yıl sayar)
        assertEquals(0, entitlement.compareTo(BigDecimal.ZERO), 
                "6 ay kıdem için izin hakkı 0 olmalı çünkü 1 yıl dolmamış");
    }

    @Test
    @DisplayName("Kıdem 1-5 yıl: Yıllık izin hakedişi 14 gün (112 saat) olmalı")
    void calculateAnnualLeaveEntitlement_1To5Years_ShouldReturn14Days() {
        // Given
        Employee employee = new Employee();
        employee.setHireDate(LocalDate.now().minusYears(3)); // 3 yıl kıdem
        employee.setDailyWorkHours(new BigDecimal("8.0"));
        LocalDate referenceDate = LocalDate.now();

        // When
        BigDecimal entitlement = leaveEntitlementService.calculateAnnualLeaveEntitlement(employee, referenceDate);

        // Then
        assertEquals(0, entitlement.compareTo(new BigDecimal("112.0")), 
                "3 yıl kıdem için 14 gün × 8 saat = 112 saat olmalı");
    }

    @Test
    @DisplayName("Kıdem 5+ yıl: Yıllık izin hakedişi 20 gün (160 saat) olmalı")
    void calculateAnnualLeaveEntitlement_MoreThan5Years_ShouldReturn20Days() {
        // Given
        Employee seniorEmployee = new Employee();
        seniorEmployee.setHireDate(LocalDate.now().minusYears(7)); // 7 yıl kıdem
        seniorEmployee.setDailyWorkHours(new BigDecimal("8.0"));
        LocalDate referenceDate = LocalDate.now();

        // When
        BigDecimal entitlement = leaveEntitlementService.calculateAnnualLeaveEntitlement(seniorEmployee, referenceDate);

        // Then
        assertEquals(0, entitlement.compareTo(new BigDecimal("160.0")), 
                "7 yıl kıdem için 20 gün × 8 saat = 160 saat olmalı");
    }

    @Test
    @DisplayName("Günlük mesai saati 7.5 saat: Hesaplama doğru olmalı")
    void calculateAnnualLeaveEntitlement_CustomWorkHours_ShouldCalculateCorrectly() {
        // Given
        Employee employee = new Employee();
        employee.setHireDate(LocalDate.now().minusYears(3)); // 3 yıl kıdem
        employee.setDailyWorkHours(new BigDecimal("7.5"));
        LocalDate referenceDate = LocalDate.now();

        // When
        BigDecimal entitlement = leaveEntitlementService.calculateAnnualLeaveEntitlement(employee, referenceDate);

        // Then
        assertEquals(0, entitlement.compareTo(new BigDecimal("105.0")), 
                "3 yıl kıdem için 14 gün × 7.5 saat = 105 saat olmalı");
    }

    // ========== YILDAN YILA AKTARIM TESTLERİ ==========

    @Test
    @DisplayName("Önceki yıldan kalan izin bir sonraki yıla aktarılmalı")
    void calculateCarryForwardFromPreviousYear_WithRemainingLeave_ShouldCarryForward() {
        // Given
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, 2024))
                .thenReturn(Optional.of(testEntitlement2024));
        // 2024'te kalan: 112 - 80 = 32 saat (4 gün)

        // When
        BigDecimal carriedForward = leaveEntitlementService.calculateCarryForwardFromPreviousYear(
                testEmployee, 2024);

        // Then
        // 2024'ün kendi hakedişinden kalan (aktarılan izinler hariç)
        // Kendi hakedişi: 112 saat
        // Kullanılan: 80 saat (tamamı kendi hakedişinden kullanıldı varsayımı)
        // Kalan: 32 saat
        assertEquals(0, carriedForward.compareTo(new BigDecimal("32.0")),
                "2024'ten 32 saat aktarılmalı (kendi hakedişinden kalan)");
    }

    @Test
    @DisplayName("Aktarılan izinler bir sonraki yıla aktarılmamalı (sadece kendi hakedişinden kalan)")
    void calculateCarryForwardFromPreviousYear_WithCarriedForwardHours_ShouldNotCarryForwardAgain() {
        // Given
        // 2025'te kendi hakedişi: 14 gün (112 saat), Aktarılan: 8 gün (64 saat), Toplam: 176 saat
        // Kullanılan: 10 gün (80 saat) - önce aktarılan izinlerden kullanıldı varsayımı
        LeaveEntitlement entitlement2025 = new LeaveEntitlement();
        entitlement2025.setEmployee(testEmployee);
        entitlement2025.setYear(2025);
        entitlement2025.setTotalHoursEntitled(new BigDecimal("176.0")); // 14 gün + 8 gün aktarılan
        entitlement2025.setHoursUsed(new BigDecimal("80.0")); // 10 gün kullanıldı
        entitlement2025.setCarriedForwardHours(new BigDecimal("64.0")); // 2024'ten 8 gün aktarıldı

        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, 2025))
                .thenReturn(Optional.of(entitlement2025));

        // When - 2026 için 2025'ten aktarım hesapla
        BigDecimal carriedForward = leaveEntitlementService.calculateCarryForwardFromPreviousYear(
                testEmployee, 2025);

        // Then
        // 2025'in kendi hakedişinden kalan: 112 - (80 - 64) = 112 - 16 = 96 saat (12 gün)
        // Aktarılan izinler (64 saat) bir sonraki yıla aktarılmamalı
        assertEquals(0, carriedForward.compareTo(new BigDecimal("96.0")), 
                "2025'ten sadece kendi hakedişinden kalan 96 saat aktarılmalı (aktarılan izinler dahil edilmemeli)");
    }

    @Test
    @DisplayName("Önceki yıl entitlement yoksa aktarım 0 olmalı")
    void calculateCarryForwardFromPreviousYear_NoPreviousYear_ShouldReturnZero() {
        // Given
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, 2023))
                .thenReturn(Optional.empty());

        // When
        BigDecimal carriedForward = leaveEntitlementService.calculateCarryForwardFromPreviousYear(
                testEmployee, 2023);

        // Then
        assertEquals(BigDecimal.ZERO, carriedForward);
    }

    // ========== LEAVE ENTITLEMENT OLUŞTURMA TESTLERİ ==========

    @Test
    @DisplayName("Yeni yıl için entitlement oluşturulmalı (kıdem + aktarım)")
    void createLeaveEntitlementForYear_NewYear_ShouldCreateWithEntitlementAndCarryForward() {
        // Given
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, 2026))
                .thenReturn(Optional.empty());
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, 2025))
                .thenReturn(Optional.of(testEntitlement2025));
        when(leaveEntitlementRepository.save(any(LeaveEntitlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LeaveEntitlement created = leaveEntitlementService.createLeaveEntitlementForYear(
                testEmployee, 2026);

        // Then
        assertNotNull(created);
        assertEquals(2026, created.getYear());
        // 2026 için kıdem hesaplaması (3 yıl kıdem = 14 gün = 112 saat)
        // + 2025'ten aktarım (hesaplanacak)
        assertTrue(created.getTotalHoursEntitled().compareTo(new BigDecimal("112.0")) >= 0);
        verify(leaveEntitlementRepository, times(1)).save(any(LeaveEntitlement.class));
    }

    @Test
    @DisplayName("Zaten var olan entitlement oluşturulmamalı (idempotent)")
    void createLeaveEntitlementForYear_ExistingEntitlement_ShouldReturnExisting() {
        // Given
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, 2025))
                .thenReturn(Optional.of(testEntitlement2025));

        // When
        LeaveEntitlement result = leaveEntitlementService.createLeaveEntitlementForYear(
                testEmployee, 2025);

        // Then
        assertEquals(testEntitlement2025, result);
        verify(leaveEntitlementRepository, never()).save(any(LeaveEntitlement.class));
    }

    // ========== BAKİYE SORGULAMA TESTLERİ ==========

    @Test
    @DisplayName("Yıllık izin bakiyesi sorgulandığında entitlement yoksa otomatik oluşturulmalı")
    void getEmployeeLeaveBalance_NoEntitlement_ShouldCreateAutomatically() {
        // Given
        int currentYear = LocalDate.now().getYear();
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, currentYear))
                .thenReturn(Optional.empty());
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, currentYear - 1))
                .thenReturn(Optional.empty());
        when(leaveEntitlementRepository.save(any(LeaveEntitlement.class)))
                .thenAnswer(invocation -> {
                    LeaveEntitlement ent = invocation.getArgument(0);
                    ent.setId(999L);
                    return ent;
                });

        // When
        LeaveBalanceResponse response = leaveEntitlementService.getEmployeeLeaveBalance(1L);

        // Then
        assertNotNull(response);
        verify(leaveEntitlementRepository, times(1)).save(any(LeaveEntitlement.class));
    }

    @Test
    @DisplayName("Mazeret izni bakiyesi sorgulama - aylık limit kontrolü")
    void getAllEmployeeLeaveBalances_WithExcuseLeave_ShouldIncludeMonthlyLimit() {
        // Given
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
        when(leaveTypeRepository.findAll())
                .thenReturn(List.of(annualLeaveType, excuseLeaveType));
        
        // Yıllık izin için entitlement yok, otomatik oluşturulacak
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, currentYear))
                .thenReturn(Optional.empty());
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(1L, currentYear - 1))
                .thenReturn(Optional.empty());
        when(leaveEntitlementRepository.save(any(LeaveEntitlement.class)))
                .thenAnswer(invocation -> {
                    LeaveEntitlement ent = invocation.getArgument(0);
                    ent.setId(999L);
                    return ent;
                });
        
        // Mazeret izni için aylık kullanım: 4 saat (2 kere × 2 saat)
        when(leaveRequestRepository.calculateMonthlyUsageByLeaveType(
                eq(1L), eq(2L), eq(currentYear), eq(currentMonth)))
                .thenReturn(new BigDecimal("4.0"));

        // When
        List<LeaveBalanceResponse> balances = leaveEntitlementService.getAllEmployeeLeaveBalances(1L);

        // Then
        assertNotNull(balances, "Bakiye listesi null olmamalı");
        assertTrue(balances.size() >= 2, "En az 2 izin türü dönmeli");
        
        // Mazeret izni bakiyesi kontrolü
        LeaveBalanceResponse excuseBalance = balances.stream()
                .filter(b -> "Mazeret İzni (Saatlik)".equals(b.getLeaveTypeName()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(excuseBalance, "Mazeret izni bakiyesi bulunamadı");
        assertEquals(0, excuseBalance.getTotalHours().compareTo(new BigDecimal("8")), 
                "Mazeret izni aylık limit 8 saat olmalı"); // Aylık limit
        assertEquals(0, excuseBalance.getHoursUsed().compareTo(new BigDecimal("4")),
                "Kullanılan mazeret izni 4 saat olmalı"); // Kullanılan
        assertEquals(0, excuseBalance.getRemainingHours().compareTo(new BigDecimal("4")),
                "Kalan mazeret izni 4 saat olmalı"); // Kalan (8 - 4 = 4)
    }

    @Test
    @DisplayName("Çalışan bulunamadığında EntityNotFoundException fırlatılmalı")
    void getEmployeeLeaveBalance_EmployeeNotFound_ShouldThrowException() {
        // Given
        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> {
            leaveEntitlementService.getEmployeeLeaveBalance(999L);
        });
    }

    // ========== TÜM ÇALIŞANLAR İÇİN YIL SONU İŞLEMİ ==========

    @Test
    @DisplayName("Tüm çalışanlar için yeni yıl entitlement'ları oluşturulmalı")
    void createLeaveEntitlementsForAllEmployees_ShouldCreateForAllActiveEmployees() {
        // Given
        Employee employee1 = new Employee();
        employee1.setId(1L);
        employee1.setHireDate(LocalDate.of(2021, 1, 1));
        employee1.setDailyWorkHours(new BigDecimal("8.0"));
        employee1.setIsActive(true);

        Employee employee2 = new Employee();
        employee2.setId(2L);
        employee2.setHireDate(LocalDate.of(2018, 1, 1)); // 5+ yıl kıdem
        employee2.setDailyWorkHours(new BigDecimal("8.0"));
        employee2.setIsActive(true);

        when(employeeRepository.findAll())
                .thenReturn(List.of(employee1, employee2));
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), eq(2026)))
                .thenReturn(Optional.empty());
        when(leaveEntitlementRepository.findByEmployeeIdAndYear(anyLong(), eq(2025)))
                .thenReturn(Optional.empty());
        when(leaveEntitlementRepository.save(any(LeaveEntitlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        leaveEntitlementService.createLeaveEntitlementsForAllEmployees(2026);

        // Then
        verify(leaveEntitlementRepository, times(2)).save(any(LeaveEntitlement.class));
    }
}


