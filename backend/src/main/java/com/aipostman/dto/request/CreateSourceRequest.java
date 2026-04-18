package com.aipostman.dto.request;

import com.aipostman.common.enums.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSourceRequest(
        @NotBlank String name,
        @NotBlank String url,
        @NotNull SourceType sourceType,
        String category,
        Integer priority,
        String language
) {
}
