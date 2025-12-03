package com.cozumtr.leave_management_system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SprintOverlapDto {

    private String employeeName; // kim izinli
    private String departmentName; // hangi departman
    private String leaveType; // izin türü
    private LocalDateTime startDate; // izin baslangıc
    private LocalDateTime endDate; // izin bitis
    private Long overlapHours; // sprint içinde kaybettigimiz saat(hesaplanacak)

}
