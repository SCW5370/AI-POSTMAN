package com.aipostman.dto.response;

public record BuildTaskResponse(
        String taskId,
        String status,
        String message,
        DigestResponse digest
) {
}
