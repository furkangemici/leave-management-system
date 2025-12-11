package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.ChangePasswordRequestDto;
import com.cozumtr.leave_management_system.dto.response.MessageResponseDto;
import com.cozumtr.leave_management_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponseDto> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
        userService.changePassword(request);

        MessageResponseDto response = MessageResponseDto.builder()
                .message("Şifre başarıyla güncellendi.")
                .build();

        return ResponseEntity.ok(response);
    }
}

