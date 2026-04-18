package com.aipostman.dto.response;

import com.aipostman.common.enums.SourceType;

public record SourceResponse(
        Long id,
        String name,
        String url,
        SourceType sourceType,
        String category,
        boolean enabled,
        int priority,
        String language
) {
}
