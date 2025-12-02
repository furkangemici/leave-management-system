package com.cozumtr.leave_management_system.config;

import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.enums.RequestUnit;
import com.cozumtr.leave_management_system.enums.WorkType;
import com.cozumtr.leave_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitialDataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("🚀 InitialDataSeeder başlatılıyor...");

        createPermissions();
        createRoles();
        createDepartments();
        createLeaveTypes();
        createUsers();
        assignDepartmentManagers();

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
        if (leaveTypeRepository.count() > 0) return;

        createLeaveType("Yıllık İzin", true, true, "HR,MANAGER,CEO", RequestUnit.DAY);
        createLeaveType("Mazeret İzni (Saatlik)", true, false, "MANAGER", RequestUnit.HOUR);
        createLeaveType("Hastalık İzni (Raporlu)", false, false, "HR,MANAGER", RequestUnit.DAY);
        createLeaveType("Ücretsiz İzin", false, false, "HR,MANAGER,CEO", RequestUnit.DAY);
    }

    private void createLeaveType(String name, boolean isPaid, boolean deductsFromAnnual,
                                 String workflowDefinition, RequestUnit requestUnit) {
        LeaveType lt = new LeaveType();
        lt.setName(name);
        lt.setPaid(isPaid);
        lt.setDeductsFromAnnual(deductsFromAnnual);
        lt.setWorkflowDefinition(workflowDefinition);
        lt.setRequestUnit(requestUnit);
        lt.setIsActive(true);
        leaveTypeRepository.save(lt);
    }

    private void createUsers() {
        if (userRepository.count() > 0) return;

        log.info("🚀 Kullanıcılar oluşturuluyor...");
        String defaultPassword = "Password123!";

        // Kullanıcı Listesi
        createUser("muhasebeci@sirket.com", "Muhasebeci", "User", "Muhasebe Uzmanı",
                "ACCOUNTING", "Finans", defaultPassword);

        createUser("ik@sirket.com", "İK", "User", "İnsan Kaynakları Uzmanı",
                "HR", "İnsan Kaynakları", defaultPassword);

        createUserWithMultipleRoles("ik.yonetici@sirket.com", "İK ", "Yöneticisi",
                "İnsan Kaynakları Müdürü", List.of("HR", "MANAGER"),
                "İnsan Kaynakları", defaultPassword);

        createUser("pazarlama.calisan@sirket.com", "Pazarlama", "Çalışan", "Pazarlama Uzmanı",
                "EMPLOYEE", "Satış ve Pazarlama", defaultPassword);

        createUser("pazarlama.yonetici@sirket.com", "Pazarlama", "Yöneticisi", "Pazarlama Müdürü",
                "MANAGER", "Satış ve Pazarlama", defaultPassword);

        createUser("urun.gelistirme.calisan@sirket.com", "Ürün Geliştirme", "Çalışan",
                "Yazılım Geliştirici", "EMPLOYEE", "Ürün Geliştirme", defaultPassword);

        createUser("urun.gelistirme.yonetici@sirket.com", "Ürün Geliştirme", "Yöneticisi",
                "Yazılım Geliştirme Müdürü", "MANAGER", "Ürün Geliştirme", defaultPassword);

        createUser("genel.mudur@sirket.com", "Genel", "Müdür", "Genel Müdür",
                "CEO", "Yönetim", defaultPassword);
    }

    private void createUser(String email, String firstName, String lastName, String jobTitle,
                            String roleName, String deptName, String password) {
        createUserWithMultipleRoles(email, firstName, lastName, jobTitle,
                Collections.singletonList(roleName), deptName, password);
    }

    private void createUserWithMultipleRoles(String email, String firstName, String lastName,
                                             String jobTitle, List<String> roleNames,
                                             String deptName, String password) {
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
            employee.setHireDate(LocalDate.now());
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
}