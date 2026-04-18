package com.aipostman.dto.response;

import java.math.BigDecimal;

public record CandidateScoreResponse(
        Long normalizedItemId,
        String title,
        String sourceName,
        String publishedAt,
        BigDecimal finalScore
) {
}
