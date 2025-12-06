package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.dto.response.RoleResponse;
import com.cozumtr.leave_management_system.entities.Role;
import com.cozumtr.leave_management_system.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * Aktif tüm rolleri listeler (EMPLOYEE hariç).
     * Frontend'de dropdown, radio button, card vb. için kullanılır.
     * İK yeni eleman eklerken rol seçimi için kullanılır.
     * 
     * NOT: EMPLOYEE rolü listede gösterilmez çünkü her kullanıcıya otomatik olarak eklenir.
     * İK sadece ekstra roller (HR, MANAGER, CEO, ACCOUNTING vb.) seçebilir.
     */
    public List<RoleResponse> getAllActiveRoles() {
        return roleRepository.findAll().stream()
                .filter(Role::getIsActive)
                .filter(role -> !"EMPLOYEE".equals(role.getRoleName()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RoleResponse mapToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .build();
    }
}
