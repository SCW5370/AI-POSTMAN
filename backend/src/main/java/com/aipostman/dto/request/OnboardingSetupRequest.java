package com.aipostman.dto.request;

import com.aipostman.common.enums.DeliveryMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public record OnboardingSetupRequest(
        @Email @NotBlank String email,
        String displayName,
        String timezone,
        String goals,
        List<String> preferredTopics,
        List<String> blockedTopics,
        DeliveryMode deliveryMode,
        String deliveryTime,
        Integer maxItemsPerDigest,
        BigDecimal explorationRatio,
        Boolean seedDefaultSources
) {
}
