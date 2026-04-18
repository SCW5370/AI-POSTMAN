package com.aipostman.service;

import com.aipostman.domain.User;
import com.aipostman.domain.UserPreference;
import com.aipostman.domain.UserProfile;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeliveryScheduleService {

    @Value("${app.scheduler.prebuild-lead-hours:8}")
    private int prebuildLeadHours;

    private final UserProfileService userProfileService;
    private final FeedbackService feedbackService;

    public DeliveryScheduleService(UserProfileService userProfileService, FeedbackService feedbackService) {
        this.userProfileService = userProfileService;
        this.feedbackService = feedbackService;
    }

    public BuildPlan getBuildPlan(User user, UserPreference preference, ZonedDateTime nowUtc) {
        ZoneId zoneId = parseZoneId(user.getTimezone());
        LocalTime deliveryTime = parseDeliveryTime(preference.getDeliveryTime());
        ZonedDateTime localNow = nowUtc.withZoneSameInstant(zoneId);

        // 计算下一次发送日期，考虑用户的发送频率偏好
        LocalDate nextDeliveryDate = calculateNextDeliveryDate(user, preference, localNow.toLocalDate());
        ZonedDateTime targetDelivery = nextDeliveryDate.atTime(deliveryTime).atZone(zoneId);

        long hoursUntilDelivery = Duration.between(localNow, targetDelivery).toHours();
        boolean inPrebuildWindow = !localNow.isAfter(targetDelivery)
                && Duration.between(localNow, targetDelivery).toHours() <= prebuildLeadHours;

        return new BuildPlan(targetDelivery.toLocalDate(), inPrebuildWindow, hoursUntilDelivery);
    }

    public boolean shouldSendNow(User user, UserPreference preference, LocalDate digestDate, ZonedDateTime nowUtc) {
        ZoneId zoneId = parseZoneId(user.getTimezone());
        LocalTime deliveryTime = parseDeliveryTime(preference.getDeliveryTime());
        ZonedDateTime localNow = nowUtc.withZoneSameInstant(zoneId);
        
        // 检查是否是计划的发送日期
        if (!digestDate.equals(calculateNextDeliveryDate(user, preference, localNow.toLocalDate()))) {
            return false;
        }
        
        // 检查是否是今天
        if (!digestDate.equals(localNow.toLocalDate())) {
            return false;
        }
        
        // 检查是否到了发送时间
        return !localNow.toLocalTime().isBefore(deliveryTime);
    }

    public boolean shouldFinalizeNow(User user, UserPreference preference, LocalDate digestDate, ZonedDateTime nowUtc) {
        BuildPlan plan = getBuildPlan(user, preference, nowUtc);
        if (digestDate.equals(plan.targetDigestDate())) {
            return true;
        }
        return shouldSendNow(user, preference, digestDate, nowUtc);
    }

    public boolean shouldSendDigest(User user, int contentQualityScore) {
        // 基于内容质量和用户活跃度决定是否发送
        int minQualityScore = 50; // 最低内容质量分数
        
        // 获取用户画像
        UserProfile profile = userProfileService.getProfile(user.getId());
        
        // 基于用户活跃度调整最低质量要求
        if (isUserActive(user)) {
            minQualityScore = 40; // 活跃用户对内容质量要求稍低
        }
        
        return contentQualityScore >= minQualityScore;
    }

    private LocalDate calculateNextDeliveryDate(User user, UserPreference preference, LocalDate currentDate) {
        // 获取用户的发送频率偏好，默认每周
        String frequency = "weekly"; // 默认每周发送一次
        if (preference != null && preference.getDeliveryFrequency() != null) {
            frequency = preference.getDeliveryFrequency();
        }
        
        // 基于频率计算下一次发送日期
        switch (frequency) {
            case "daily":
                return currentDate;
            case "weekly":
                // 每周一发送
                int daysUntilMonday = (8 - currentDate.getDayOfWeek().getValue()) % 7;
                return currentDate.plusDays(daysUntilMonday);
            case "biweekly":
                // 每两周发送一次
                int daysUntilBiweekly = (15 - currentDate.getDayOfMonth()) % 14;
                return currentDate.plusDays(daysUntilBiweekly);
            case "monthly":
                // 每月1号发送
                if (currentDate.getDayOfMonth() == 1) {
                    return currentDate;
                } else {
                    return currentDate.plusMonths(1).withDayOfMonth(1);
                }
            default:
                // 默认每周发送一次
                int daysUntilDefault = (8 - currentDate.getDayOfWeek().getValue()) % 7;
                return currentDate.plusDays(daysUntilDefault);
        }
    }

    private boolean isUserActive(User user) {
        // 基于用户反馈频率判断用户活跃度
        long feedbackCount = feedbackService.getFeedbackCountByUserId(user.getId(), 30); // 最近30天的反馈数
        return feedbackCount >= 3; // 至少3次反馈视为活跃用户
    }

    private ZoneId parseZoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ignored) {
            return ZoneId.of("Asia/Shanghai");
        }
    }

    private LocalTime parseDeliveryTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (Exception ignored) {
            return LocalTime.of(8, 0);
        }
    }

    public record BuildPlan(LocalDate targetDigestDate, boolean shouldBuildNow, long hoursUntilDelivery) {}
}
