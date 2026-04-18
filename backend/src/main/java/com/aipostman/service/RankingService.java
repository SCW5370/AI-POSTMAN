package com.aipostman.service;

import com.aipostman.domain.ItemEnrichment;
import com.aipostman.domain.NormalizedItem;
import com.aipostman.domain.UserPreference;
import com.aipostman.repository.ItemEnrichmentRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

    private final ItemEnrichmentRepository itemEnrichmentRepository;
    private final FeedbackService feedbackService;
    private final UserProfileService userProfileService;

    public RankingService(ItemEnrichmentRepository itemEnrichmentRepository, FeedbackService feedbackService, UserProfileService userProfileService) {
        this.itemEnrichmentRepository = itemEnrichmentRepository;
        this.feedbackService = feedbackService;
        this.userProfileService = userProfileService;
    }

    public BigDecimal score(Long userId, UserPreference preference, NormalizedItem item) {
        double keywordMatchScore = computeKeywordMatch(preference, item);
        double goalMatchScore = computeGoalMatch(preference, item);
        double sourceQualityScore = computeSourceQuality(preference, item);
        double freshnessScore = computeFreshness(item);
        double feedbackAdjustment = feedbackService.getFeedbackAdjustment(userId, item);
        double profileMatchScore = computeProfileMatch(userId, item);
        double genericPenalty = computeGenericPenalty(item);
        double total = keywordMatchScore + goalMatchScore + sourceQualityScore + freshnessScore + feedbackAdjustment + profileMatchScore - genericPenalty;
        total = Math.max(total, 0.0);
        return BigDecimal.valueOf(total).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private double computeProfileMatch(Long userId, NormalizedItem item) {
        // 基于用户画像计算内容匹配分数
        com.aipostman.domain.UserProfile profile = userProfileService.getProfile(userId);
        if (profile == null) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // 匹配用户兴趣
        if (profile.getInterests() != null) {
            String sourceName = normalize(item.getRawItem().getSource().getName());
            for (Map.Entry<String, Object> entry : profile.getInterests().entrySet()) {
                String interest = normalize(entry.getKey());
                if (sourceName.contains(interest)) {
                    score += 10.0;
                }
            }
        }
        
        // 匹配用户偏好话题
        if (profile.getPreferredTopics() != null) {
            String combined = String.join(" ",
                    normalize(item.getTitleClean()),
                    normalize(item.getSummaryClean()),
                    item.getTags().stream().map(this::normalize).reduce("", (a, b) -> a + " " + b));
            
            for (Map.Entry<String, Object> entry : profile.getPreferredTopics().entrySet()) {
                String topic = normalize(entry.getKey());
                if (combined.contains(topic)) {
                    score += 15.0;
                }
            }
        }
        
        return Math.min(score, 30.0); // 上限30分
    }

    private double computeKeywordMatch(UserPreference preference, NormalizedItem item) {
        String combined = String.join(" ",
                normalize(item.getTitleClean()),
                normalize(item.getSummaryClean()),
                item.getTags().stream().map(this::normalize).reduce("", (a, b) -> a + " " + b));

        return preference.getPreferredTopics().stream()
                .map(this::normalize)
                .filter(topic -> !topic.isBlank())
                .mapToDouble(topic -> topicMatchScore(combined, topic))
                .sum();
    }

    private double computeGoalMatch(UserPreference preference, NormalizedItem item) {
        String goal = normalize(preference.getGoals());
        if (goal.isBlank()) {
            return 0.0;
        }
        String combined = normalize(item.getTitleClean()) + " " + normalize(item.getSummaryClean());
        double score = 0;
        for (String part : goal.split("[,，。\\s]+")) {
            if (!part.isBlank() && combined.contains(part)) {
                score += 10.0;
            }
        }
        ItemEnrichment enrichment = itemEnrichmentRepository.findByNormalizedItemId(item.getId()).orElse(null);
        if (enrichment != null && enrichment.getRelevanceReason() != null) {
            String reason = normalize(enrichment.getRelevanceReason());
            for (String part : goal.split("[,，。\\s]+")) {
                if (!part.isBlank() && reason.contains(part)) {
                    score += 5.0;
                }
            }
        }
        return Math.min(score, 26.0);
    }

    private double computeSourceQuality(UserPreference preference, NormalizedItem item) {
        double normalizedPriority = item.getSourceQualityScore().doubleValue() / 4.0;
        String sourceName = normalize(item.getRawItem().getSource().getName());
        boolean preferred = preference.getPreferredSources().stream()
                .map(this::normalize)
                .anyMatch(sourceName::contains);
        return Math.min(normalizedPriority, 24.0) + (preferred ? 8.0 : 0.0);
    }

    private double computeFreshness(NormalizedItem item) {
        LocalDateTime publishedAt = item.getRawItem().getPublishedAt();
        if (publishedAt == null) {
            return item.getFreshnessScore().doubleValue();
        }
        long hours = Math.max(Duration.between(publishedAt, LocalDateTime.now()).toHours(), 0);
        if (hours <= 6) {
            return 25.0;
        }
        if (hours <= 24) {
            return 18.0;
        }
        if (hours <= 72) {
            return 10.0;
        }
        return 4.0;
    }

    private double computeGenericPenalty(NormalizedItem item) {
        String title = normalize(item.getTitleClean());
        String summary = normalize(item.getSummaryClean());
        double penalty = 0.0;
        if (title.startsWith("show hn") || title.startsWith("ask hn")) {
            penalty += 18.0;
        }
        if (title.contains("is hiring") || title.contains("hiring")) {
            penalty += 22.0;
        }
        if (summary.startsWith("article url:") || summary.startsWith("comments url:")) {
            penalty += 16.0;
        }
        if (title.contains("what are you working on")) {
            penalty += 20.0;
        }
        return penalty;
    }

    private boolean containsTopic(String combined, String topic) {
        if (combined.contains(topic)) {
            return true;
        }
        List<String> tokens = tokenize(topic).toList();
        long matched = tokens.stream()
                .filter(token -> token.length() >= 2 && combined.contains(token))
                .count();
        return !tokens.isEmpty() && matched == tokens.size();
    }

    private Stream<String> tokenize(String value) {
        return Arrays.stream(value.split("[,，。/\\-\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isBlank());
    }

    private double topicMatchScore(String combined, String topic) {
        if (combined.contains(topic)) {
            return 24.0;
        }
        List<String> tokens = tokenize(topic).toList();
        if (tokens.isEmpty()) {
            return 0.0;
        }
        long matched = tokens.stream()
                .filter(token -> token.length() >= 2 && combined.contains(token))
                .count();
        if (matched == tokens.size() && tokens.size() > 1) {
            return 18.0;
        }
        if (matched >= 2 && tokens.size() >= 3) {
            return 10.0;
        }
        return 0.0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
