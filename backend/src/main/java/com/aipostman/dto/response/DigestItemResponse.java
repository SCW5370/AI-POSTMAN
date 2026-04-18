package com.aipostman.dto.response;

import java.math.BigDecimal;

public record DigestItemResponse(
        Long id,
        String section,
        Integer itemOrder,
        BigDecimal finalScore,
        String enrichmentStatus,
        String title,
        String summary,
        String relevanceReason,
        String sourceName,
        String url,
        String usefulFeedbackUrl,
        String normalFeedbackUrl,
        String followFeedbackUrl
) {
}
