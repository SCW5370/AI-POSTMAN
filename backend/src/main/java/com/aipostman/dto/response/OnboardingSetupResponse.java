package com.aipostman.dto.response;

public record OnboardingSetupResponse(
        UserResponse user,
        PreferenceResponse preference,
        int seededSources
) {
}
