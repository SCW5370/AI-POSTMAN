package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.dto.request.CreateUserRequest;
import com.aipostman.dto.request.UpdateUserRequest;
import com.aipostman.dto.response.UserResponse;
import com.aipostman.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> get(@PathVariable Long userId) {
        return ApiResponse.ok(userService.get(userId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> update(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userService.update(userId, request));
    }
}
