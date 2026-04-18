package com.aipostman.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DigestResponse(
        Long id,
        LocalDate digestDate,
        String status,
        String subject,
        Integer totalItems,
        LocalDateTime sentAt,
        List<DigestItemResponse> items,
        Boolean llmUsed,
        String editorialStrategy
) {
}
