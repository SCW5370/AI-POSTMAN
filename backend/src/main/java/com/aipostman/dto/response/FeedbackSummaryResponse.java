package com.aipostman.dto.response;

import java.util.List;

public record FeedbackSummaryResponse(
        long usefulCount,
        long normalCount,
        long followCount,
        List<FeedbackBucketResponse> topSources,
        List<FeedbackBucketResponse> topTopics
) {
}
