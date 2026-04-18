package com.aipostman.dto.response;

public record FetchTaskResponse(
        String taskId,
        String status,
        String message,
        Integer savedCount
) {
}
