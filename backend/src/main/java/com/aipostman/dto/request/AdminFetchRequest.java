package com.aipostman.dto.request;

import java.util.List;

public record AdminFetchRequest(List<Long> sourceIds) {
}
