package com.aipostman.service;

import com.aipostman.common.enums.FeedbackType;
import com.aipostman.domain.FeedbackEvent;
import com.aipostman.domain.NormalizedItem;
import com.aipostman.dto.request.FeedbackRequest;
import com.aipostman.dto.response.FeedbackBucketResponse;
import com.aipostman.dto.response.FeedbackSummaryResponse;
import com.aipostman.repository.DailyDigestItemRepository;
import com.aipostman.repository.DailyDigestRepository;
import com.aipostman.repository.FeedbackEventRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final DailyDigestRepository digestRepository;
    private final DailyDigestItemRepository digestItemRepository;
    private final FeedbackEventRepository feedbackEventRepository;

    public FeedbackService(
            UserService userService,
            UserProfileService userProfileService,
            DailyDigestRepository digestRepository,
            DailyDigestItemRepository digestItemRepository,
            FeedbackEventRepository feedbackEventRepository
    ) {
        this.userService = userService;
        this.userProfileService = userProfileService;
        this.digestRepository = digestRepository;
        this.digestItemRepository = digestItemRepository;
        this.feedbackEventRepository = feedbackEventRepository;
    }

    @Transactional
    public void record(FeedbackRequest request) {
        FeedbackEvent event = new FeedbackEvent();
        event.setUser(userService.getUser(request.userId()));
        event.setDigest(digestRepository.findById(request.digestId())
                .orElseThrow(() -> new IllegalArgumentException("Digest not found: " + request.digestId())));
        event.setDigestItem(digestItemRepository.findById(request.digestItemId())
                .orElseThrow(() -> new IllegalArgumentException("Digest item not found: " + request.digestItemId())));
        event.setFeedbackType(request.feedbackType());
        feedbackEventRepository.save(event);

        // 更新用户画像
        updateUserProfileBasedOnFeedback(request.userId(), event);
    }

    private void updateUserProfileBasedOnFeedback(Long userId, FeedbackEvent event) {
        // 基于反馈内容更新用户画像
        Map<String, Object> surveyData = new HashMap<>();
        
        // 分析用户对内容的反馈，提取相关的兴趣和偏好
        if (event.getFeedbackType() == FeedbackType.USEFUL || event.getFeedbackType() == FeedbackType.FOLLOW) {
            // 提取内容的标签作为用户的兴趣
            List<String> tags = event.getDigestItem().getNormalizedItem().getTags();
            if (!tags.isEmpty()) {
                surveyData.put("preferredTopics", tags);
            }
            
            // 提取内容的来源作为用户的兴趣
            String sourceName = event.getDigestItem().getNormalizedItem().getRawItem().getSource().getName();
            if (sourceName != null && !sourceName.isBlank()) {
                surveyData.put("interests", List.of(sourceName));
            }
        }
        
        // 如果有有效的数据，更新用户画像
        if (!surveyData.isEmpty()) {
            userProfileService.updateProfileFromSurvey(userId, surveyData);
        }
    }

    public double getFeedbackAdjustment(Long userId, Long normalizedItemId) {
        List<FeedbackEvent> events = feedbackEventRepository.findByUserIdAndNormalizedItemId(userId, normalizedItemId);
        return events.stream()
                .map(FeedbackEvent::getFeedbackType)
                .mapToDouble(this::feedbackWeight)
                .sum();
    }

    public double getFeedbackAdjustment(Long userId, NormalizedItem item) {
        double direct = getFeedbackAdjustment(userId, item.getId());
        double sourceAffinity = getSourceAffinityAdjustment(userId, item);
        double topicAffinity = getTopicAffinityAdjustment(userId, item);
        return direct + sourceAffinity + topicAffinity;
    }

    public FeedbackSummaryResponse summary(Long userId) {
        List<FeedbackEvent> events = feedbackEventRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Double> sourceScores = new HashMap<>();
        Map<String, Double> topicScores = new HashMap<>();
        for (FeedbackEvent event : events) {
            double weight = feedbackWeight(event.getFeedbackType());
            String sourceName = event.getDigestItem().getNormalizedItem().getRawItem().getSource().getName();
            sourceScores.merge(sourceName, weight, Double::sum);
            event.getDigestItem().getNormalizedItem().getTags().stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.toLowerCase(Locale.ROOT))
                    .forEach(tag -> topicScores.merge(tag, weight, Double::sum));
        }
        return new FeedbackSummaryResponse(
                feedbackEventRepository.countByUserIdAndFeedbackType(userId, FeedbackType.USEFUL),
                feedbackEventRepository.countByUserIdAndFeedbackType(userId, FeedbackType.NORMAL),
                feedbackEventRepository.countByUserIdAndFeedbackType(userId, FeedbackType.FOLLOW),
                topBuckets(sourceScores),
                topBuckets(topicScores)
        );
    }

    private double getSourceAffinityAdjustment(Long userId, NormalizedItem item) {
        String sourceName = item.getRawItem().getSource().getName();
        return feedbackEventRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(event -> sourceName.equalsIgnoreCase(event.getDigestItem().getNormalizedItem().getRawItem().getSource().getName()))
                .limit(10)
                .map(FeedbackEvent::getFeedbackType)
                .mapToDouble(type -> feedbackWeight(type) * 0.25)
                .sum();
    }

    private double getTopicAffinityAdjustment(Long userId, NormalizedItem item) {
        List<String> tags = item.getTags().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .toList();
        if (tags.isEmpty()) {
            return 0.0;
        }
        return feedbackEventRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(event -> event.getDigestItem().getNormalizedItem().getTags().stream()
                        .map(tag -> tag.toLowerCase(Locale.ROOT))
                        .anyMatch(tags::contains))
                .limit(15)
                .map(FeedbackEvent::getFeedbackType)
                .mapToDouble(type -> feedbackWeight(type) * 0.15)
                .sum();
    }

    private List<FeedbackBucketResponse> topBuckets(Map<String, Double> buckets) {
        return buckets.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> new FeedbackBucketResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private double feedbackWeight(FeedbackType type) {
        return switch (type) {
            case USEFUL -> 8.0;
            case FOLLOW -> 12.0;
            case NORMAL -> 2.0;
        };
    }

    public long getFeedbackCountByUserId(Long userId, int days) {
        return feedbackEventRepository.countByUserIdAndCreatedAtAfter(userId, LocalDateTime.now().minusDays(days));
    }
}
