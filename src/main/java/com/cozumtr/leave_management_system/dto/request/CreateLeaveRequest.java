package com.cozumtr.leave_management_system.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateLeaveRequest {

    @NotNull(message = "İzin türü seçilmelidir.")
    private Long leaveTypeId; // Örn: Yıllık İzin (ID: 1), Mazeret (ID: 2)

    @NotNull(message = "Başlangıç tarihi boş olamaz.")
    @FutureOrPresent(message = "Geçmişe dönük izin talep edilemez.")
    private LocalDateTime startDate;

    @NotNull(message = "Bitiş tarihi boş olamaz.")
    private LocalDateTime endDate;

    private String reason; // İzin sebebi (Opsiyonel olabilir)
}

