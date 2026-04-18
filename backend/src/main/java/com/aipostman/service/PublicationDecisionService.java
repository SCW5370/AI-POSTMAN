package com.aipostman.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PublicationDecisionService {

    @Value("${app.digest.high-score-threshold}")
    private int highScoreThreshold;

    @Value("${app.digest.medium-score-threshold}")
    private int mediumScoreThreshold;

    @Value("${app.digest.medium-score-min-count}")
    private int mediumScoreMinCount;

    public boolean shouldPublish(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) {
            return false;
        }
        boolean hasHigh = scores.stream().anyMatch(score -> score.doubleValue() >= highScoreThreshold);
        long mediumCount = scores.stream().filter(score -> score.doubleValue() >= mediumScoreThreshold).count();
        if (hasHigh || mediumCount >= mediumScoreMinCount) {
            return true;
        }

        List<BigDecimal> topScores = scores.stream()
                .sorted(java.util.Comparator.reverseOrder())
                .limit(5)
                .toList();
        boolean hasMvpLead = topScores.stream().anyMatch(score -> score.doubleValue() >= 60.0);
        long mvpSupportCount = topScores.stream().filter(score -> score.doubleValue() >= 50.0).count();
        if (hasMvpLead || mvpSupportCount >= 2) {
            return true;
        }

        boolean hasUsableLead = topScores.stream().anyMatch(score -> score.doubleValue() >= 45.0);
        if (hasUsableLead && topScores.size() >= 3) {
            return true;
        }

        // 为新用户或没有偏好的用户降低发布阈值
        boolean hasLowLead = topScores.stream().anyMatch(score -> score.doubleValue() >= 40.0);
        return hasLowLead && topScores.size() >= 2;
    }
}
