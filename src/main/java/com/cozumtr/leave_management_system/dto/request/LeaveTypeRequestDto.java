package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveTypeRequestDto {

    @NotBlank(message = "İzin türü adı boş olamaz")
    private String name;

    @NotNull
    private Boolean isPaid; //ücretli mi

    @NotNull
    private Boolean deductsFromAnnual; //yıllık izinden düşer mi

    // Örn: "ROLE_HR,ROLE_MANAGER"
    @NotBlank(message = "Onay akışı boş olamaz")
    private String workflowDefinition;

    // "DAY" veya "HOUR"
    @NotBlank
    private String requestUnit;

}
