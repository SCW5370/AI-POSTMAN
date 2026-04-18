package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.dto.request.OnboardingSetupRequest;
import com.aipostman.dto.response.OnboardingSetupResponse;
import com.aipostman.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/setup")
    public ApiResponse<OnboardingSetupResponse> setup(@Valid @RequestBody OnboardingSetupRequest request) {
        return ApiResponse.ok(onboardingService.setup(request));
    }
}
