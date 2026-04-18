package com.aipostman.service;

import com.aipostman.dto.request.CreateUserRequest;
import com.aipostman.dto.request.OnboardingSetupRequest;
import com.aipostman.dto.request.SavePreferenceRequest;
import com.aipostman.dto.response.OnboardingSetupResponse;
import com.aipostman.dto.response.PreferenceResponse;
import com.aipostman.dto.response.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

    private final UserService userService;
    private final PreferenceService preferenceService;
    private final SourceService sourceService;

    public OnboardingService(UserService userService, PreferenceService preferenceService, SourceService sourceService) {
        this.userService = userService;
        this.preferenceService = preferenceService;
        this.sourceService = sourceService;
    }

    @Transactional
    public OnboardingSetupResponse setup(OnboardingSetupRequest request) {
        UserResponse user = userService.create(new CreateUserRequest(
                request.email(),
                request.displayName(),
                request.timezone()
        ));
        PreferenceResponse preference = preferenceService.save(user.id(), new SavePreferenceRequest(
                request.goals(),
                request.preferredTopics(),
                request.blockedTopics(),
                null,
                request.deliveryMode(),
                request.deliveryTime(),
                "daily",
                request.maxItemsPerDigest(),
                request.explorationRatio()
        ));
        int seeded = Boolean.TRUE.equals(request.seedDefaultSources()) ? sourceService.seedDefaultSources() : 0;
        return new OnboardingSetupResponse(user, preference, seeded);
    }
}
