package com.cozumtr.leave_management_system.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayTemplateResponse {
    private Long id;
    private String name;
    private String code;
    private Integer durationDays;
    private Boolean isHalfDayBefore;
    private Boolean isMovable;
    private String fixedDate;
}
