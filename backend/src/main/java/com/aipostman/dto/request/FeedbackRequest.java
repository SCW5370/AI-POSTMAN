package com.aipostman.dto.request;

import com.aipostman.common.enums.FeedbackType;

public record FeedbackRequest(Long userId, Long digestId, Long digestItemId, FeedbackType feedbackType) {
}
