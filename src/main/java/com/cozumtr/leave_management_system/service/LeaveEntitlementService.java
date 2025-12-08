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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveEntitlementService {

    private final LeaveEntitlementRepository leaveEntitlementRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    /**
     * Çalışanın yıllık izin bakiyesini döndürür (deductsFromAnnual = true olan izinler için).
     * 
     * @param employeeId Çalışan ID'si
     * @return LeaveBalanceResponse (kalan saat, toplam saat, kalan gün)
     * @throws EntityNotFoundException Eğer çalışan veya izin bakiyesi bulunamazsa
     */
    public LeaveBalanceResponse getEmployeeLeaveBalance(Long employeeId) {
        // 1. Çalışanı bul
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Çalışan bulunamadı: " + employeeId));

        // 2. Mevcut yıl için izin bakiyesini bul
        int currentYear = LocalDate.now().getYear();
        LeaveEntitlement entitlement = leaveEntitlementRepository
                .findByEmployeeIdAndYear(employeeId, currentYear)
                .orElse(null);
        
        // Eğer entitlement yoksa, otomatik oluştur (kıdeme göre + aktarım)
        if (entitlement == null) {
            entitlement = createLeaveEntitlementForYear(employee, currentYear);
        }

        // 3. DTO'ya map'le ve döndür
        return mapToResponse(entitlement, employee);
    }

    /**
     * Çalışanın belirli bir izin türü için bakiyesini döndürür.
     * 
     * @param employeeId Çalışan ID'si
     * @param leaveTypeId İzin türü ID'si
     * @return LeaveBalanceResponse (kalan saat, toplam saat, kalan gün)
     * @throws EntityNotFoundException Eğer çalışan veya izin türü bulunamazsa
     */
    public LeaveBalanceResponse getEmployeeLeaveBalanceByType(Long employeeId, Long leaveTypeId) {
        // 1. Çalışanı bul
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Çalışan bulunamadı: " + employeeId));

        // 2. İzin türünü bul
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new EntityNotFoundException("İzin türü bulunamadı: " + leaveTypeId));

        // 3. İzin türüne göre bakiye hesapla
        if (leaveType.isDeductsFromAnnual()) {
            // Yıllık izin bakiyesi (LeaveEntitlement'tan)
            return getEmployeeLeaveBalance(employeeId);
        } else {
            // Yıllık izin bakiyesinden düşmeyen izinler için dinamik hesaplama
            return calculateNonAnnualLeaveBalance(employee, leaveType);
        }
    }

    /**
     * Yıllık izin bakiyesinden düşmeyen izinler için bakiye hesaplar (örn: Mazeret İzni).
     * 
     * @param employee Çalışan
     * @param leaveType İzin türü
     * @return LeaveBalanceResponse
     */
    private LeaveBalanceResponse calculateNonAnnualLeaveBalance(Employee employee, LeaveType leaveType) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        // Mazeret İzni için aylık limit
        if ("Mazeret İzni (Saatlik)".equals(leaveType.getName())) {
            BigDecimal monthlyLimit = new BigDecimal("8"); // Aylık 8 saat limit
            
            // Bu ay için kullanılan mazeret izni
            BigDecimal monthlyUsed = leaveRequestRepository.calculateMonthlyUsageByLeaveType(
                    employee.getId(),
                    leaveType.getId(),
                    currentYear,
                    currentMonth
            );
            
            // Kalan mazeret izni
            BigDecimal remainingHours = monthlyLimit.subtract(monthlyUsed);
            if (remainingHours.compareTo(BigDecimal.ZERO) < 0) {
                remainingHours = BigDecimal.ZERO;
            }

            // Günlük mesai saatini al
            BigDecimal dailyWorkHours = employee.getDailyWorkHours();
            
            // Gün hesaplama (mazeret izni saatlik olduğu için genelde null kalır)
            Integer remainingDays = null;
            if (dailyWorkHours != null && dailyWorkHours.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal daysDecimal = remainingHours.divide(dailyWorkHours, 2, RoundingMode.DOWN);
                remainingDays = daysDecimal.intValue();
            }

            return LeaveBalanceResponse.builder()
                    .leaveTypeId(leaveType.getId())
                    .leaveTypeName(leaveType.getName())
                    .year(currentYear)
                    .totalHours(monthlyLimit)
                    .hoursUsed(monthlyUsed)
                    .remainingHours(remainingHours)
                    .totalDays(null) // Aylık limit olduğu için toplam gün anlamsız
                    .daysUsed(null)
                    .remainingDays(remainingDays)
                    .build();
        } else {
            // Diğer izin türleri için (Hastalık İzni, Ücretsiz İzin vb.)
            // Limit yok, sınırsız olarak kabul edilir
            // Bu durumda kullanım bilgisi gösterilebilir
            BigDecimal yearlyUsed = leaveRequestRepository.calculateYearlyUsageByLeaveType(
                    employee.getId(),
                    leaveType.getId(),
                    currentYear
            );

            return LeaveBalanceResponse.builder()
                    .leaveTypeId(leaveType.getId())
                    .leaveTypeName(leaveType.getName())
                    .year(currentYear)
                    .totalHours(null) // Limit yok
                    .hoursUsed(yearlyUsed)
                    .remainingHours(null) // Limit yok
                    .totalDays(null)
                    .daysUsed(null)
                    .remainingDays(null)
                    .build();
        }
    }

    /**
     * LeaveEntitlement ve Employee bilgilerini LeaveBalanceResponse DTO'suna çevirir.
     * 
     * @param entitlement İzin bakiyesi entity
     * @param employee Çalışan entity (günlük mesai saati için)
     * @return LeaveBalanceResponse DTO
     */
    private LeaveBalanceResponse mapToResponse(LeaveEntitlement entitlement, Employee employee) {
        // Yıl bilgisi
        Integer year = entitlement.getYear();

        // Toplam saat
        BigDecimal totalHours = entitlement.getTotalHoursEntitled();

        // Kullanılan saat
        BigDecimal hoursUsed = entitlement.getHoursUsed();

        // Kalan saati hesapla
        BigDecimal remainingHours = entitlement.getRemainingHours();

        // Günlük mesai saatini al
        BigDecimal dailyWorkHours = employee.getDailyWorkHours();

        // Saatleri güne çevir
        // Eğer dailyWorkHours 0 veya null ise, gün hesabı yapılamaz
        Integer totalDays = null;
        Integer daysUsed = null;
        Integer remainingDays = null;
        
        if (dailyWorkHours != null && dailyWorkHours.compareTo(BigDecimal.ZERO) > 0) {
            // Toplam gün: totalHours / dailyWorkHours
            BigDecimal totalDaysDecimal = totalHours.divide(dailyWorkHours, 2, RoundingMode.DOWN);
            totalDays = totalDaysDecimal.intValue();
            
            // Kullanılan gün: hoursUsed / dailyWorkHours
            BigDecimal daysUsedDecimal = hoursUsed.divide(dailyWorkHours, 2, RoundingMode.DOWN);
            daysUsed = daysUsedDecimal.intValue();
            
            // Kalan gün: remainingHours / dailyWorkHours
            BigDecimal remainingDaysDecimal = remainingHours.divide(dailyWorkHours, 2, RoundingMode.DOWN);
            remainingDays = remainingDaysDecimal.intValue();
        }

        return LeaveBalanceResponse.builder()
                .year(year)
                .totalHours(totalHours)
                .hoursUsed(hoursUsed)
                .remainingHours(remainingHours)
                .totalDays(totalDays)
                .daysUsed(daysUsed)
                .remainingDays(remainingDays)
                .build();
    }

    /**
     * Çalışanın tüm izin türleri için bakiyelerini döndürür.
     * Kullanıcı bu metod ile tüm izin türlerinde ne kadar hakkı kaldığını görebilir.
     * 
     * @param employeeId Çalışan ID'si
     * @return Tüm izin türleri için bakiyeler listesi
     */
    public List<LeaveBalanceResponse> getAllEmployeeLeaveBalances(Long employeeId) {
        // 1. Çalışanı bul
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Çalışan bulunamadı: " + employeeId));

        // 2. Tüm aktif izin türlerini getir
        List<LeaveType> activeLeaveTypes = leaveTypeRepository.findAll().stream()
                .filter(LeaveType::getIsActive)
                .collect(Collectors.toList());

        // 3. Her izin türü için bakiyeyi hesapla
        List<LeaveBalanceResponse> balances = new ArrayList<>();
        for (LeaveType leaveType : activeLeaveTypes) {
            LeaveBalanceResponse balance;
            
            if (leaveType.isDeductsFromAnnual()) {
                // Yıllık izin bakiyesi (LeaveEntitlement'tan)
                int currentYear = LocalDate.now().getYear();
                LeaveEntitlement entitlement = leaveEntitlementRepository
                        .findByEmployeeIdAndYear(employeeId, currentYear)
                        .orElse(null);
                
                // Eğer entitlement yoksa, otomatik oluştur (kıdeme göre + aktarım)
                if (entitlement == null) {
                    entitlement = createLeaveEntitlementForYear(employee, currentYear);
                }
                
                balance = mapToResponse(entitlement, employee);
                balance.setLeaveTypeId(leaveType.getId());
                balance.setLeaveTypeName(leaveType.getName());
            } else {
                // Yıllık izin bakiyesinden düşmeyen izinler (dinamik hesaplama)
                balance = calculateNonAnnualLeaveBalance(employee, leaveType);
            }
            
            balances.add(balance);
        }

        return balances;
    }

    /**
     * Çalışanın kıdemine göre yıllık izin hakedişini hesaplar (saat cinsinden).
     * Kurallar:
     * - İlk 1 sene (0-1 yıl): 0 gün (izin kullanılamaz)
     * - 1-5 sene arası: 14 gün
     * - 5+ sene: 20 gün
     *
     * @param employee Çalışan
     * @param referenceDate Referans tarih (yıl sonu işlemleri için)
     * @return Yıllık izin hakedişi (saat cinsinden)
     */
    public BigDecimal calculateAnnualLeaveEntitlement(Employee employee, LocalDate referenceDate) {
        long yearsOfService = employee.getYearsOfServiceAsOf(referenceDate);
        BigDecimal dailyWorkHours = employee.getDailyWorkHours();
        
        if (dailyWorkHours == null || dailyWorkHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        int daysEntitled;
        if (yearsOfService < 1) {
            // İlk 1 sene: izin kullanılamaz
            daysEntitled = 0;
        } else if (yearsOfService < 5) {
            // 1-5 sene arası: 14 gün
            daysEntitled = 14;
        } else {
            // 5+ sene: 20 gün
            daysEntitled = 20;
        }

        // Günleri saate çevir
        return dailyWorkHours.multiply(BigDecimal.valueOf(daysEntitled));
    }

    /**
     * Belirli bir yıl için bir önceki yıldan kalan izni hesaplar.
     * Aktarım kuralı: Sadece bir sonraki yıla aktarılır, daha fazla aktarılmaz.
     * ÖNEMLİ: Aktarılan izinler (carriedForwardHours) bir sonraki yıla aktarılmaz,
     * sadece o yılın kendi hakedişinden kalan aktarılır.
     *
     * @param employee Çalışan
     * @param previousYear Önceki yıl
     * @return Aktarılabilir izin miktarı (saat cinsinden) - Sadece o yılın kendi hakedişinden kalan
     */
    public BigDecimal calculateCarryForwardFromPreviousYear(Employee employee, int previousYear) {
        LeaveEntitlement previousYearEntitlement = leaveEntitlementRepository
                .findByEmployeeIdAndYear(employee.getId(), previousYear)
                .orElse(null);

        if (previousYearEntitlement == null) {
            return BigDecimal.ZERO;
        }

        // O yılın kendi hakedişi = Total - Aktarılan izinler
        BigDecimal annualEntitlementOnly = previousYearEntitlement.getTotalHoursEntitled()
                .subtract(previousYearEntitlement.getCarriedForwardHours());

        // O yılda kullanılan izin miktarı
        BigDecimal hoursUsed = previousYearEntitlement.getHoursUsed();

        // Aktarılan izinler önce kullanıldığı varsayılır (best practice)
        BigDecimal carriedForwardHours = previousYearEntitlement.getCarriedForwardHours();
        BigDecimal usedFromCarriedForward = hoursUsed.min(carriedForwardHours); // Aktarılan izinlerden kullanılan
        BigDecimal usedFromAnnual = hoursUsed.subtract(usedFromCarriedForward); // Kendi hakedişinden kullanılan

        // O yılın kendi hakedişinden kalan = Kendi hakedişi - Kendi hakedişinden kullanılan
        BigDecimal remainingFromAnnual = annualEntitlementOnly.subtract(usedFromAnnual);
        
        // Eğer kendi hakedişinden kalan varsa, sadece onu aktar (aktarılan izinler dahil edilmez)
        if (remainingFromAnnual.compareTo(BigDecimal.ZERO) > 0) {
            return remainingFromAnnual;
        }

        return BigDecimal.ZERO;
    }

    /**
     * Bir çalışan için yeni yıl için LeaveEntitlement oluşturur.
     * Kıdeme göre hakediş + önceki yıldan aktarılan izin hesaplanır.
     *
     * @param employee Çalışan
     * @param year Yıl
     * @return Oluşturulan LeaveEntitlement
     */
    public LeaveEntitlement createLeaveEntitlementForYear(Employee employee, int year) {
        // Aynı yıl için zaten kayıt varsa, güncelleme yapma (idempotent olmalı)
        LeaveEntitlement existing = leaveEntitlementRepository
                .findByEmployeeIdAndYear(employee.getId(), year)
                .orElse(null);

        if (existing != null) {
            return existing;
        }

        // Referans tarihi: Yılın başlangıcı
        LocalDate yearStart = LocalDate.of(year, 1, 1);

        // 1. Kıdeme göre yıllık izin hakedişi
        BigDecimal annualEntitlement = calculateAnnualLeaveEntitlement(employee, yearStart);

        // 2. Önceki yıldan aktarılan izin (sadece 1 yıl öncesinden)
        BigDecimal carriedForward = calculateCarryForwardFromPreviousYear(employee, year - 1);

        // 3. Toplam hakediş = Yıllık hakediş + Aktarılan izin
        BigDecimal totalHoursEntitled = annualEntitlement.add(carriedForward);

        // 4. LeaveEntitlement oluştur
        LeaveEntitlement entitlement = new LeaveEntitlement();
        entitlement.setEmployee(employee);
        entitlement.setYear(year);
        entitlement.setTotalHoursEntitled(totalHoursEntitled);
        entitlement.setHoursUsed(BigDecimal.ZERO);
        entitlement.setCarriedForwardHours(carriedForward); // Aktarılan izin bilgisi

        return leaveEntitlementRepository.save(entitlement);
    }

    /**
     * Tüm çalışanlar için yeni yıl LeaveEntitlement kayıtlarını oluşturur.
     * Yıl sonu işlemi için kullanılır (cron job veya manuel trigger).
     *
     * @param year Yeni yıl
     */
    public void createLeaveEntitlementsForAllEmployees(int year) {
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(Employee::getIsActive)
                .collect(Collectors.toList());

        for (Employee employee : activeEmployees) {
            try {
                createLeaveEntitlementForYear(employee, year);
            } catch (Exception e) {
                // Log hatası ama devam et
                // TODO: Logger kullanılabilir
                System.err.println("Hata: " + employee.getEmail() + " için " + year + " yılı entitlement oluşturulamadı: " + e.getMessage());
            }
        }
    }
}

