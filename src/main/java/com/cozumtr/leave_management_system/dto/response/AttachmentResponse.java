package com.cozumtr.leave_management_system.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class AttachmentResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadDate;
}

