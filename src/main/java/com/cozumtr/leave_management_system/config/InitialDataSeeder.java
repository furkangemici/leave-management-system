package com.cozumtr.leave_management_system.config;

import com.cozumtr.leave_management_system.entities.Department;
import com.cozumtr.leave_management_system.entities.LeaveType;
import com.cozumtr.leave_management_system.entities.Permission;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.repository.DepartmentRepository;
import com.cozumtr.leave_management_system.repository.PermissionRepository;
import com.cozumtr.leave_management_system.repository.LeaveTypeRepository;
import com.cozumtr.leave_management_system.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Uygulama ayağa kalktığında kritik lookup verilerini (rol, permission, departman)
 * otomatik oluşturan seeder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitialDataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    private static final List<String> PERMISSIONS = List.of(
            "auth:token_refresh",
            "leave:create",
            "leave:view_own",
            "report:view_team",
            "leave:approve_hr",
            "user:create",
            "user:view_all",
            "report:view_all",
            "metadata:manage",
            "leave:approve_ceo",
            "leave:approve_manager",
            "report:view_accounting"
    );

    private static final Map<String, List<String>> ROLE_PERMISSION_MAP = Map.of(
            "HR", List.of(
                    "auth:token_refresh",
                    "leave:create",
                    "leave:view_own",
                    "leave:approve_hr",
                    "user:create",
                    "user:view_all",
                    "report:view_all",
                    "metadata:manage"
            ),
            "CEO", List.of(
                    "auth:token_refresh",
                    "leave:create",
                    "leave:view_own",
                    "leave:approve_ceo",
                    "report:view_all"
            ),
            "MANAGER", List.of(
                    "auth:token_refresh",
                    "leave:create",
                    "leave:view_own",
                    "leave:approve_manager",
                    "report:view_team"
            ),
            "ACCOUNTING", List.of(
                    "auth:token_refresh",
                    "leave:create",
                    "leave:view_own",
                    "report:view_accounting"
            ),
            "EMPLOYEE", List.of(
                    "auth:token_refresh",
                    "leave:create",
                    "leave:view_own",
                    "report:view_team"
            )
    );

    private static final List<String> DEPARTMENTS = List.of(
            "Yönetim",
            "Ürün Geliştirme",
            "Satış ve Pazarlama",
            "İnsan Kaynakları",
            "Finans"
    );

    @Override
    public void run(String... args) {
        seedPermissions();
        seedRoles();
        seedDepartments();
        seedLeaveTypes();
    }

    private void seedPermissions() {
        for (String permissionName : PERMISSIONS) {
            permissionRepository.findByPermissionName(permissionName)
                    .orElseGet(() -> {
                        Permission permission = new Permission();
                        permission.setPermissionName(permissionName);
                        permission.setIsActive(true);
                        permissionRepository.save(permission);
                        log.info("Permission oluşturuldu: {}", permissionName);
                        return permission;
                    });
        }
    }

    private void seedRoles() {
        ROLE_PERMISSION_MAP.forEach((roleName, permissionNames) -> {
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setRoleName(roleName);
                        newRole.setIsActive(true);
                        roleRepository.save(newRole);
                        log.info("Role oluşturuldu: {}", roleName);
                        return newRole;
                    });

            Set<Permission> permissions = permissionNames.stream()
                    .map(this::getPermissionByName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                    
            role.setPermissions(permissions);
            roleRepository.save(role);
            log.info("Role-permission atandı/güncellendi: {}", roleName);
        });
    }

    private void seedDepartments() {
        for (String deptName : DEPARTMENTS) {
            departmentRepository.findByName(deptName)
                    .orElseGet(() -> {
                        Department dept = new Department();
                        dept.setName(deptName);
                        dept.setIsActive(true);
                        departmentRepository.save(dept);
                        log.info("Departman oluşturuldu: {}", deptName);
                        return dept;
                    });
        }
    }

    private void seedLeaveTypes() {
        createOrUpdateLeaveType(
                "Yıllık İzin",
                true,
                true,
                "HR,MANAGER,CEO",
                com.cozumtr.leave_management_system.enums.RequestUnit.DAY
        );

        createOrUpdateLeaveType(
                "Mazeret İzni (Saatlik)",
                true,
                false,
                "MANAGER",
                com.cozumtr.leave_management_system.enums.RequestUnit.HOUR
        );

        createOrUpdateLeaveType(
                "Hastalık İzni (Raporlu)",
                false,
                false,
                "HR,MANAGER",
                com.cozumtr.leave_management_system.enums.RequestUnit.DAY
        );

        createOrUpdateLeaveType(
                "Ücretsiz İzin",
                false,
                false,
                "HR,MANAGER,CEO",
                com.cozumtr.leave_management_system.enums.RequestUnit.DAY
        );
    }

    private void createOrUpdateLeaveType(
            String name,
            boolean isPaid,
            boolean deductsFromAnnual,
            String workflowDefinition,
            com.cozumtr.leave_management_system.enums.RequestUnit requestUnit
    ) {
        LeaveType type = leaveTypeRepository.findAll().stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    LeaveType lt = new LeaveType();
                    lt.setName(name);
                    return lt;
                });

        type.setPaid(isPaid);
        type.setDeductsFromAnnual(deductsFromAnnual);
        type.setWorkflowDefinition(workflowDefinition);
        type.setRequestUnit(requestUnit);
        type.setIsActive(true);

        leaveTypeRepository.save(type);
        log.info("LeaveType seed/güncelleme: {}", name);
    }

    private Permission getPermissionByName(String permissionName) {
        return permissionRepository.findByPermissionName(permissionName)
                .orElseThrow(() -> new IllegalStateException("Permission bulunamadı: " + permissionName));
    }
}

