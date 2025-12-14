package com.cozumtr.leave_management_system.controller;

import com.cozumtr.leave_management_system.dto.request.ChangePasswordRequestDto;
import com.cozumtr.leave_management_system.dto.response.MessageResponseDto;
import com.cozumtr.leave_management_system.dto.response.UserResponse;
import com.cozumtr.leave_management_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PreAuthorize("hasRole('HR')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(search, pageable);
        return ResponseEntity.ok(users);
    }
}

