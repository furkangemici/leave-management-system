package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.*;
import com.cozumtr.leave_management_system.enums.RequestStatus;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class) // Mockito'yu etkinleştirir
public class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository; // Depoyu taklit ediyoruz (Mock)

    @InjectMocks
    private LeaveRequestService leaveRequestService; // Test edeceğimiz asıl servis

    // SENARYO 1: Kullanıcı İK (HR) ise
    @Test
    public void whenUserIsHR_shouldFindAllPendingRequests() {
        // 1. HAZIRLIK (Arrange)
        User hrUser = new User();

        // Rol oluşturuyoruz: ROLE_HR
        Role hrRole = new Role();
        hrRole.setRoleName("ROLE_HR");

        Set<Role> roles = new HashSet<>();
        roles.add(hrRole);
        hrUser.setRoles(roles);

        // 2. İŞLEM (Act)
        // Servis metodunu çağırıyoruz
        leaveRequestService.getRequestsForApproval(hrUser);

        // 3. DOĞRULAMA (Assert)
        // İK olduğu için "findByRequestStatus" metodunu çağırmalıydı.
        // verify() metodu, mock nesnesinin bu metodunun çağırılıp çağırılmadığını kontrol eder.
        verify(leaveRequestRepository, times(1)).findByRequestStatus(RequestStatus.PENDING_APPROVAL);
    }

    // SENARYO 2: Kullanıcı Yönetici (Manager) ise
    @Test
    public void whenUserIsManager_shouldFindOnlyDepartmentRequests() {
        // 1. HAZIRLIK (Arrange)
        User managerUser = new User();

        // Rol oluşturuyoruz: ROLE_MANAGER (İK DEĞİL)
        Role managerRole = new Role();
        managerRole.setRoleName("ROLE_MANAGER");

        Set<Role> roles = new HashSet<>();
        roles.add(managerRole);
        managerUser.setRoles(roles);

        // Yöneticiye bir departman atamamız lazım (Çünkü kodumuz getDepartment().getId() yapıyor)
        // Zincirleme veri oluşturuyoruz: User -> Employee -> Department
        Employee employee = new Employee();
        Department department = new Department();
        department.setId(55L); // Örnek Departman ID'si: 55

        employee.setDepartment(department);
        managerUser.setEmployee(employee);

        // 2. İŞLEM (Act)
        leaveRequestService.getRequestsForApproval(managerUser);

        // 3. DOĞRULAMA (Assert)
        // Yönetici olduğu için SADECE kendi departmanını (ID: 55) sorgulamalıydı.
        verify(leaveRequestRepository, times(1))
                .findByEmployeeDepartmentIdAndRequestStatus(55L, RequestStatus.PENDING_APPROVAL);
    }
}