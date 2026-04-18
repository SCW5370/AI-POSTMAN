package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.dto.request.SavePreferenceRequest;
import com.aipostman.dto.response.PreferenceResponse;
import com.aipostman.service.PreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @PostMapping("/{userId}")
    public ApiResponse<PreferenceResponse> save(@PathVariable Long userId, @Valid @RequestBody SavePreferenceRequest request) {
        return ApiResponse.ok(preferenceService.save(userId, request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<PreferenceResponse> get(@PathVariable Long userId) {
        return ApiResponse.ok(preferenceService.get(userId));
    }
}
