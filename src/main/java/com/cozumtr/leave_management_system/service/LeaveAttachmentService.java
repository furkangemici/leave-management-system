package com.cozumtr.leave_management_system.service;

import com.cozumtr.leave_management_system.entities.LeaveAttachment;
import com.cozumtr.leave_management_system.entities.LeaveRequest;
import com.cozumtr.leave_management_system.entities.User;
import com.cozumtr.leave_management_system.exception.BusinessException;
import com.cozumtr.leave_management_system.repository.LeaveAttachmentRepository;
import com.cozumtr.leave_management_system.repository.LeaveRequestRepository;
import com.cozumtr.leave_management_system.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveAttachmentService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveAttachmentRepository leaveAttachmentRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    @Value("${app.upload.max-file-size:5242880}")
    private long maxFileSizeBytes;
    @Value("${app.upload.allowed-content-types:application/pdf,image/jpeg,image/png}")
    private String allowedContentTypesRaw;

    @Transactional
    public LeaveAttachment uploadAttachment(Long leaveRequestId, MultipartFile file) {
        validateFile(file);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new EntityNotFoundException("İzin talebi bulunamadı ID: " + leaveRequestId));

        authorizeOwnerOrApprover(leaveRequest);

        Path requestDir = Paths.get(uploadDir, "leave-attachments", String.valueOf(leaveRequestId))
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(requestDir);

            String originalFileName = file.getOriginalFilename();
            String cleanOriginalName = StringUtils.hasText(originalFileName)
                    ? StringUtils.cleanPath(originalFileName)
                    : "attachment";
            String storedFileName = UUID.randomUUID() + "_" + cleanOriginalName;

            Path targetPath = requestDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            LeaveAttachment attachment = new LeaveAttachment();
            attachment.setLeaveRequest(leaveRequest);
            attachment.setFileName(cleanOriginalName);
            attachment.setFilePath(targetPath.toString());
            attachment.setFileType(file.getContentType());
            attachment.setUploadDate(LocalDateTime.now());

            LeaveAttachment savedAttachment = leaveAttachmentRepository.save(attachment);
            leaveRequest.getAttachments().add(savedAttachment);

            return savedAttachment;
        } catch (IOException ex) {
            throw new BusinessException("Dosya yüklenirken hata oluştu: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<com.cozumtr.leave_management_system.dto.response.AttachmentResponse> listAttachments(Long leaveRequestId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new EntityNotFoundException("İzin talebi bulunamadı ID: " + leaveRequestId));

        authorizeOwnerOrApprover(leaveRequest);

        return leaveAttachmentRepository.findByLeaveRequestId(leaveRequestId).stream()
                .map(att -> com.cozumtr.leave_management_system.dto.response.AttachmentResponse.builder()
                        .id(att.getId())
                        .fileName(att.getFileName())
                        .fileType(att.getFileType())
                        .uploadDate(att.getUploadDate())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadAttachment(Long attachmentId) {
        LeaveAttachment attachment = leaveAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Belge bulunamadı ID: " + attachmentId));

        LeaveRequest leaveRequest = attachment.getLeaveRequest();
        authorizeOwnerOrApprover(leaveRequest);

        Path path = Paths.get(attachment.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException("Belge dosyası bulunamadı veya silinmiş.");
        }

        Resource resource = new PathResource(path);
        String contentType = attachment.getFileType() != null ? attachment.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(attachment.getFileName())
                                .build()
                                .toString())
                .body(resource);
    }

    private void authorizeOwnerOrApprover(LeaveRequest leaveRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isOwner = leaveRequest.getEmployee().getEmail().equals(email);

        com.cozumtr.leave_management_system.entities.User user = userRepository.findByEmployeeEmail(email)
                .orElse(null);

        boolean isHR = hasRole(user, "HR");
        boolean isCEO = hasRole(user, "CEO");
        boolean isManager = hasRole(user, "MANAGER");

        boolean sameDepartment = user != null
                && user.getEmployee() != null
                && user.getEmployee().getDepartment() != null
                && leaveRequest.getEmployee() != null
                && leaveRequest.getEmployee().getDepartment() != null
                && user.getEmployee().getDepartment().getId().equals(leaveRequest.getEmployee().getDepartment().getId());

        boolean isApprover = isHR || isCEO || (isManager && sameDepartment);

        if (!isOwner && !isApprover) {
            throw new BusinessException("Bu belgeye erişim yetkiniz yok.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Yüklenecek dosya bulunamadı.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessException("Dosya boyutu sınırı aşıldı. Maksimum: " + maxFileSizeBytes + " bayt");
        }

        Set<String> allowed = parseAllowedContentTypes();
        String contentType = file.getContentType();
        if (contentType == null || !allowed.contains(contentType.toLowerCase())) {
            throw new BusinessException("Bu dosya tipi desteklenmiyor. İzin verilen tipler: " + String.join(", ", allowed));
        }
    }

    private Set<String> parseAllowedContentTypes() {
        if (!org.springframework.util.StringUtils.hasText(allowedContentTypesRaw)) {
            return Collections.emptySet();
        }
        return Arrays.stream(allowedContentTypesRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean hasRole(User user, String roleName) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> roleName.equals(r.getRoleName()));
    }
}

