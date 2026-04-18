package com.aipostman.dto.request;

import java.time.LocalDate;

public record BuildDigestRequest(Long userId, LocalDate digestDate, Boolean forceLlm) {
}
