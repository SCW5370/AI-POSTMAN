package com.aipostman.dto.response;

import com.aipostman.common.enums.DeliveryMode;
import java.math.BigDecimal;
import java.util.List;

public record PreferenceResponse(
        Long userId,
        String goals,
        List<String> preferredTopics,
        List<String> blockedTopics,
        List<String> preferredSources,
        DeliveryMode deliveryMode,
        String deliveryTime,
        String deliveryFrequency,
        Integer maxItemsPerDigest,
        BigDecimal explorationRatio
) {
}
