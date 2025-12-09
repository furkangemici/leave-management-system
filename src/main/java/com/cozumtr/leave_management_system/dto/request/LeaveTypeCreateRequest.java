package com.cozumtr.leave_management_system.dto.request;

import com.cozumtr.leave_management_system.enums.RequestUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeCreateRequest {

    @NotBlank(message = "İzin türü adı boş olamaz")
    private String name;

    @NotNull(message = "Ücretli izin durumu belirtilmelidir")
    private Boolean isPaid;

    @NotNull(message = "Yıllık izinden düşme durumu belirtilmelidir")
    private Boolean deductsFromAnnual;

    private String workflowDefinition;

    @NotNull(message = "İstek birimi belirtilmelidir")
    private RequestUnit requestUnit;
}

