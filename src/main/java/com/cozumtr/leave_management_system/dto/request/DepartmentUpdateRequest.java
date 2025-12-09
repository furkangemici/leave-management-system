package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentUpdateRequest {

    @NotBlank(message = "Departman adı boş olamaz")
    private String name;

    /**
     * Departman yöneticisi ID'si (opsiyonel).
     * Mevcut yöneticiyi güncellemek veya yeni yönetici atamak için kullanılır.
     * null gönderilirse yönetici ataması değiştirilmez.
     */
    private Long managerId;
}

