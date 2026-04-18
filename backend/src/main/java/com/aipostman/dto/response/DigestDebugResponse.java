package com.aipostman.dto.response;

import java.util.List;

public record DigestDebugResponse(
        boolean publishable,
        int scoredCount,
        int selectedCount,
        List<CandidateScoreResponse> topScored,
        List<CandidateScoreResponse> selected
) {
}
