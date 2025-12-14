package com.cozumtr.leave_management_system.config;

import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.enums.WorkType;
import com.cozumtr.leave_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") 
public class InitialDataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveEntitlementRepository leaveEntitlementRepository;
    private final HolidayTemplateRepository holidayTemplateRepository;

    @Override
    public void run(String... args) {
        log.info("🚀 InitialDataSeeder başlatılıyor...");

        createHolidayTemplates();
        createPermissions();
        createRoles();
        createDepartments();
        createLeaveTypes();
        createUsers();
        assignDepartmentManagers();
        createLeaveEntitlements();

        log.info("✅ InitialDataSeeder tamamlandı!");
    }

    private void createPermissions() {
        if (permissionRepository.count() > 0) return;

        String[] perms = {
                "auth:token_refresh", "leave:create", "leave:view_own",
                "report:view_team", "leave:approve_hr", "user:create",
                "user:view_all", "report:view_all", "metadata:manage",
                "leave:approve_ceo", "leave:approve_manager", "report:view_accounting"
        };

        for (String pName : perms) {
            Permission p = new Permission();
            p.setPermissionName(pName);
            p.setIsActive(true);
            permissionRepository.save(p);
        }
    }

    private void createRoles() {
        if (roleRepository.count() > 0) return;

        Map<String, List<String>> rolePerms = Map.of(
                "HR", List.of("auth:token_refresh", "leave:create", "leave:view_own", "leave:approve_hr",
                        "user:create", "user:view_all", "report:view_all", "metadata:manage"),
                "CEO", List.of("auth:token_refresh", "leave:create", "leave:view_own", "leave:approve_ceo", "report:view_all"),
                "MANAGER", List.of("auth:token_refresh", "leave:create", "leave:view_own", "leave:approve_manager", "report:view_team"),
                "ACCOUNTING", List.of("auth:token_refresh", "leave:create", "leave:view_own", "report:view_accounting"),
                "EMPLOYEE", List.of("auth:token_refresh", "leave:create", "leave:view_own", "report:view_team")
        );

        rolePerms.forEach((roleName, permNames) -> {
            Role role = new Role();
            role.setRoleName(roleName);
            role.setIsActive(true);
            role = roleRepository.save(role);

            Set<Permission> permissions = new HashSet<>();
            for (String permName : permNames) {
                permissionRepository.findByPermissionName(permName).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
            roleRepository.save(role);
        });
    }

    private void createDepartments() {
        if (departmentRepository.count() > 0) return;

        String[] depts = {"Yönetim", "Ürün Geliştirme", "Satış ve Pazarlama", "İnsan Kaynakları", "Finans"};
        for (String dName : depts) {
            Department d = new Department();
            d.setName(dName);
            d.setIsActive(true);
            departmentRepository.save(d);
        }
    }

    private void createLeaveTypes() {
        if (leaveTypeRepository.count() == 0) {
            createLeaveType("Yıllık İzin", true, true, false, "HR,MANAGER,CEO", RequestUnit.DAY);
            createLeaveType("Mazeret İzni (Saatlik)", true, false, false, "MANAGER", RequestUnit.HOUR);
            createLeaveType("Hastalık İzni (Raporlu)", false, false, true, "HR,MANAGER", RequestUnit.DAY);
            createLeaveType("Ücretsiz İzin", false, false, false, "HR,MANAGER,CEO", RequestUnit.DAY);
        } else {
            updateDocumentRequiredFlags();
        }
    }

    private void updateDocumentRequiredFlags() {
        Map<String, Boolean> docRequiredMap = Map.of(
                "Yıllık İzin", false,
                "Mazeret İzni (Saatlik)", false,
                "Hastalık İzni (Raporlu)", true,
                "Ücretsiz İzin", false
        );

        docRequiredMap.forEach((name, docRequired) ->
                leaveTypeRepository.findByName(name).ifPresent(lt -> {
                    lt.setDocumentRequired(docRequired);
                    leaveTypeRepository.save(lt);
                })
        );
    }

    private void createLeaveType(String name, boolean isPaid, boolean deductsFromAnnual,
                                 boolean documentRequired, String workflowDefinition, RequestUnit requestUnit) {
        LeaveType lt = new LeaveType();
        lt.setName(name);
        lt.setPaid(isPaid);
        lt.setDeductsFromAnnual(deductsFromAnnual);
        lt.setDocumentRequired(documentRequired);
        lt.setWorkflowDefinition(workflowDefinition);
        lt.setRequestUnit(requestUnit);
        lt.setIsActive(true);
        leaveTypeRepository.save(lt);
    }

    private void createUsers() {
        if (userRepository.count() > 0) return;

        log.info("🚀 Kullanıcılar oluşturuluyor...");
        String defaultPassword = "Password123!";

        // Kullanıcı Listesi - Farklı kıdemlerle test için
        // 0 gün izin hakkı (yeni işe başlayan - 0 yıl kıdem)
        createUser("muhasebeci@sirket.com", "Muhasebeci", "User", "Muhasebe Uzmanı",
                "ACCOUNTING", "Finans", defaultPassword, LocalDate.now());

        // 14 gün izin hakkı (1-5 yıl arası kıdem)
        createUser("ik@sirket.com", "İK", "User", "İnsan Kaynakları Uzmanı",
                "HR", "İnsan Kaynakları", defaultPassword, LocalDate.now().minusYears(2));

        createUserWithMultipleRoles("ik.yonetici@sirket.com", "İK ", "Yöneticisi",
                "İnsan Kaynakları Müdürü", List.of("HR", "MANAGER"),
                "İnsan Kaynakları", defaultPassword, LocalDate.now().minusYears(3));

        createUser("pazarlama.calisan@sirket.com", "Pazarlama", "Çalışan", "Pazarlama Uzmanı",
                "EMPLOYEE", "Satış ve Pazarlama", defaultPassword, LocalDate.now().minusYears(1).minusMonths(6));

        createUser("pazarlama.yonetici@sirket.com", "Pazarlama", "Yöneticisi", "Pazarlama Müdürü",
                "MANAGER", "Satış ve Pazarlama", defaultPassword, LocalDate.now().minusYears(4));

        // 20 gün izin hakkı (5+ yıl kıdem)
        createUser("urun.gelistirme.calisan@sirket.com", "Ürün Geliştirme", "Çalışan",
                "Yazılım Geliştirici", "EMPLOYEE", "Ürün Geliştirme", defaultPassword, LocalDate.now().minusYears(6));

        createUser("urun.gelistirme.yonetici@sirket.com", "Ürün Geliştirme", "Yöneticisi",
                "Yazılım Geliştirme Müdürü", "MANAGER", "Ürün Geliştirme", defaultPassword, LocalDate.now().minusYears(7));

        createUser("genel.mudur@sirket.com", "Genel", "Müdür", "Genel Müdür",
                "CEO", "Yönetim", defaultPassword, LocalDate.now().minusYears(10));
    }

    private void createUser(String email, String firstName, String lastName, String jobTitle,
                            String roleName, String deptName, String password, LocalDate hireDate) {
        createUserWithMultipleRoles(email, firstName, lastName, jobTitle,
                Collections.singletonList(roleName), deptName, password, hireDate);
    }

    private void createUserWithMultipleRoles(String email, String firstName, String lastName,
                                             String jobTitle, List<String> roleNames,
                                             String deptName, String password, LocalDate hireDate) {
        try {
            // Department Bul
            Department dept = departmentRepository.findByName(deptName)
                    .orElse(null);

            if(dept == null) {
                log.error("❌ Departman bulunamadı, kullanıcı atlandı: {}", deptName);
                return;
            }

            // Rolleri Bul
            Set<Role> roles = new HashSet<>();
            for (String roleName : roleNames) {
                roleRepository.findByRoleName(roleName).ifPresent(roles::add);
            }
            // Default EMPLOYEE rolü
            roleRepository.findByRoleName("EMPLOYEE").ifPresent(roles::add);

            // 1. Employee nesnesini HAZIRLA
            Employee employee = new Employee();
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setEmail(email);
            employee.setJobTitle(jobTitle);
            employee.setBirthDate(LocalDate.now().minusYears(30));
            employee.setHireDate(hireDate);
            employee.setDailyWorkHours(BigDecimal.valueOf(8.0));
            employee.setIsActive(true);
            employee.setDepartment(dept);
            employee.setWorkType(WorkType.FULL_TIME);

            // 2. User nesnesini HAZIRLA
            User user = new User();
            // user.setId(...) YAPMA -> MapsId veya Cascade halledecek
            user.setEmployee(employee); // İlişkiyi kur
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setIsActive(true);
            user.setFailedLoginAttempts(0);
            user.setRoles(roles);

            // 3. SADECE USER'I KAYDET
            userRepository.save(user);

            log.info("✅ Kullanıcı oluşturuldu: {}", email);

        } catch (Exception e) {
            log.error("❌ Hata oluştu ({}): {}", email, e.getMessage());
        }
    }

    private void assignDepartmentManagers() {
        try {
            assignManager("ik.yonetici@sirket.com", "İnsan Kaynakları");
            assignManager("pazarlama.yonetici@sirket.com", "Satış ve Pazarlama");
            assignManager("urun.gelistirme.yonetici@sirket.com", "Ürün Geliştirme");
            assignManager("genel.mudur@sirket.com", "Yönetim");

        } catch (Exception e) {
            log.error("❌ Departman manager atama hatası: {}", e.getMessage());
        }
    }

    private void assignManager(String email, String deptName) {
        Optional<Employee> empOpt = employeeRepository.findByEmail(email);
        Optional<Department> deptOpt = departmentRepository.findByName(deptName);

        if (empOpt.isPresent() && deptOpt.isPresent()) {
            Department dept = deptOpt.get();
            if (dept.getManager() == null) {
                dept.setManager(empOpt.get());
                departmentRepository.save(dept);
                log.info("✅ {} yöneticisi atandı.", deptName);
            }
        }
    }

    /**
     * Test için kullanıcılara izin hakları oluşturur.
     * Kıdeme göre otomatik hesaplama yapılır:
     * - 0-1 yıl: 0 gün
     * - 1-5 yıl: 14 gün
     * - 5+ yıl: 20 gün
     */
    private void createLeaveEntitlements() {
        if (leaveEntitlementRepository.count() > 0) return;

        log.info("🚀 İzin hakları oluşturuluyor...");
        int currentYear = LocalDate.now().getYear();

        List<Employee> employees = employeeRepository.findAll();
        for (Employee employee : employees) {
            try {
                // Kıdeme göre izin günü hesapla
                long yearsOfService = employee.getYearsOfServiceAsOf(LocalDate.now());
                int daysEntitled;
                
                if (yearsOfService < 1) {
                    daysEntitled = 0;
                } else if (yearsOfService < 5) {
                    daysEntitled = 14;
                } else {
                    daysEntitled = 20;
                }

                // Günlük çalışma saati
                BigDecimal dailyWorkHours = employee.getDailyWorkHours();
                if (dailyWorkHours == null || dailyWorkHours.compareTo(BigDecimal.ZERO) <= 0) {
                    dailyWorkHours = BigDecimal.valueOf(8.0);
                }

                // Toplam saat hesapla
                BigDecimal totalHours = dailyWorkHours.multiply(BigDecimal.valueOf(daysEntitled));

                // LeaveEntitlement oluştur
                LeaveEntitlement entitlement = new LeaveEntitlement();
                entitlement.setEmployee(employee);
                entitlement.setYear(currentYear);
                entitlement.setTotalHoursEntitled(totalHours);
                entitlement.setHoursUsed(BigDecimal.ZERO);
                entitlement.setCarriedForwardHours(BigDecimal.ZERO);

                leaveEntitlementRepository.save(entitlement);
                
                log.info("✅ {} için {} gün ({} saat) izin hakkı oluşturuldu (Kıdem: {} yıl)",
                        employee.getEmail(), daysEntitled, totalHours, yearsOfService);

            } catch (Exception e) {
                log.error("❌ {} için izin hakkı oluşturulamadı: {}", employee.getEmail(), e.getMessage());
            }
        }
    }
    
    /**
     * Türkiye resmi tatil şablonlarını oluşturur.
     */
    private void createHolidayTemplates() {
        if (holidayTemplateRepository.count() > 0) {
            log.info("✅ Tatil şablonları zaten mevcut, atlanıyor...");
            return;
        }

        log.info("🚀 Tatil şablonları oluşturuluyor...");

        List<HolidayTemplate> templates = Arrays.asList(
                // Sabit tatiller
                createTemplate("Yılbaşı", "YILBASI", 1, false, false, "01-01"),
                createTemplate("Ulusal Egemenlik ve Çocuk Bayramı", "23_NISAN", 1, false, false, "04-23"),
                createTemplate("Emek ve Dayanışma Günü", "1_MAYIS", 1, false, false, "05-01"),
                createTemplate("Gençlik ve Spor Bayramı", "19_MAYIS", 1, false, false, "05-19"),
                createTemplate("Demokrasi ve Milli Birlik Günü", "15_TEMMUZ", 1, false, false, "07-15"),
                createTemplate("Zafer Bayramı", "30_AGUSTOS", 1, false, false, "08-30"),
                createTemplate("Cumhuriyet Bayramı", "29_EKIM", 1, true, false, "10-29"),

                // Hareketli tatiller (dini bayramlar)
                createTemplate("Ramazan Bayramı", "RAMAZAN_BAYRAMI", 3, true, true, null),
                createTemplate("Kurban Bayramı", "KURBAN_BAYRAMI", 4, true, true, null)
        );

        holidayTemplateRepository.saveAll(templates);
        log.info("✅ {} tatil şablonu oluşturuldu", templates.size());
    }

    private HolidayTemplate createTemplate(String name, String code, Integer durationDays,
                                           Boolean isHalfDayBefore, Boolean isMovable, String fixedDate) {
        HolidayTemplate template = new HolidayTemplate();
        template.setName(name);
        template.setCode(code);
        template.setDurationDays(durationDays);
        template.setIsHalfDayBefore(isHalfDayBefore);
        template.setIsMovable(isMovable);
        template.setFixedDate(fixedDate);
        template.setIsActive(true);
        return template;
    }
}