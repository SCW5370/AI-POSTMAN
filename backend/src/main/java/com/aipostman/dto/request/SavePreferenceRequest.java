package com.aipostman.dto.request;

import com.aipostman.common.enums.DeliveryMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

public record SavePreferenceRequest(
        String goals,
        List<String> preferredTopics,
        List<String> blockedTopics,
        List<String> preferredSources,
        DeliveryMode deliveryMode,
        String deliveryTime,
        String deliveryFrequency,
        @Min(1) @Max(20) Integer maxItemsPerDigest,
        BigDecimal explorationRatio
) {
}
