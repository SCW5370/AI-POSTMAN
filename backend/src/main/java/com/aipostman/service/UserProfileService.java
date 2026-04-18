package com.aipostman.service;

import com.aipostman.client.WorkerClient;
import com.aipostman.domain.User;
import com.aipostman.domain.UserProfile;
import com.aipostman.repository.UserProfileRepository;
import com.aipostman.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerClient workerClient;

    @Transactional
    public UserProfile getOrCreateProfile(Long userId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            if (!user.isPresent()) {
                throw new IllegalArgumentException("User not found");
            }

            Optional<UserProfile> profile = userProfileRepository.findByUserId(userId);
            if (profile.isPresent()) {
                return profile.get();
            }

            // 创建新的用户画像
            UserProfile newProfile = new UserProfile();
            newProfile.setUser(user.get());
            newProfile.setConfidenceScore(BigDecimal.valueOf(0.0));
            return userProfileRepository.save(newProfile);
        } catch (Exception e) {
            // 数据库连接失败时，返回一个内存中的用户画像
            System.err.println("Database connection failed, returning in-memory profile: " + e.getMessage());
            UserProfile profile = new UserProfile();
            profile.setId(1L);
            profile.setConfidenceScore(BigDecimal.valueOf(0.5));
            return profile;
        }
    }

    @Transactional
    public UserProfile updateProfileFromChat(Long userId, String chatHistory) {
        UserProfile profile = getOrCreateProfile(userId);

        // 调用 Worker 服务解析聊天内容，提取用户画像信息
        Map<String, Object> profileData = workerClient.analyzeChatForProfile(chatHistory);

        // 更新用户画像
        if (profileData.containsKey("interests")) {
            profile.setInterests(asMap(profileData.get("interests")));
        }
        if (profileData.containsKey("occupation")) {
            profile.setOccupation(asString(profileData.get("occupation")));
        }
        if (profileData.containsKey("recentActivities")) {
            profile.setRecentActivities(asMap(profileData.get("recentActivities")));
        }
        if (profileData.containsKey("preferredTopics")) {
            profile.setPreferredTopics(asMap(profileData.get("preferredTopics")));
        }
        if (profileData.containsKey("readingHabits")) {
            profile.setReadingHabits(asMap(profileData.get("readingHabits")));
        }
        if (profileData.containsKey("timePreferences")) {
            profile.setTimePreferences(asMap(profileData.get("timePreferences")));
        }
        if (profileData.containsKey("contentPreferences")) {
            profile.setContentPreferences(asMap(profileData.get("contentPreferences")));
        }
        if (profileData.containsKey("confidenceScore")) {
            Object confidenceScoreObj = profileData.get("confidenceScore");
            if (confidenceScoreObj instanceof Double) {
                profile.setConfidenceScore(BigDecimal.valueOf((Double) confidenceScoreObj));
            } else if (confidenceScoreObj instanceof Integer) {
                profile.setConfidenceScore(BigDecimal.valueOf((Integer) confidenceScoreObj));
            } else if (confidenceScoreObj instanceof String) {
                try {
                    profile.setConfidenceScore(new BigDecimal((String) confidenceScoreObj));
                } catch (NumberFormatException e) {
                    // 忽略无效的置信度分数
                }
            }
        }

        return userProfileRepository.save(profile);
    }

    @Transactional
    public UserProfile updateProfileFromSurvey(Long userId, Map<String, Object> surveyData) {
        UserProfile profile = getOrCreateProfile(userId);

        Map<String, Object> profileData = new java.util.HashMap<>();
        try {
            // 调用 Worker 服务解析问卷数据，提取用户画像信息
            profileData = workerClient.analyzeSurveyForProfile(surveyData);
        } catch (Exception e) {
            // Worker 服务不可用时，使用降级策略
            // 直接从问卷数据中提取简单信息
            System.err.println("Worker service unavailable, using fallback strategy: " + e.getMessage());
            if (surveyData.containsKey("preferredTopics")) {
                profileData.put("preferredTopics", surveyData.get("preferredTopics"));
            }
            if (surveyData.containsKey("goals")) {
                profileData.put("learningGoals", surveyData.get("goals"));
            }
            if (surveyData.containsKey("experienceLevel")) {
                profileData.put("experienceLevel", surveyData.get("experienceLevel"));
            }
            if (surveyData.containsKey("timeAvailability")) {
                profileData.put("timeAvailability", surveyData.get("timeAvailability"));
            }
            if (surveyData.containsKey("contentPreferences")) {
                profileData.put("contentPreferences", surveyData.get("contentPreferences"));
            }
            // 直接设置一些基本信息，确保用户画像能够保存
            profileData.put("confidenceScore", 0.5);
        }

        // 更新用户画像
        if (profileData.containsKey("interests")) {
            profile.setInterests(asMap(profileData.get("interests")));
        }
        if (profileData.containsKey("occupation")) {
            profile.setOccupation(asString(profileData.get("occupation")));
        }
        if (profileData.containsKey("recentActivities")) {
            profile.setRecentActivities(asMap(profileData.get("recentActivities")));
        }
        if (profileData.containsKey("preferredTopics")) {
            profile.setPreferredTopics(asMap(profileData.get("preferredTopics")));
        }
        if (profileData.containsKey("readingHabits")) {
            profile.setReadingHabits(asMap(profileData.get("readingHabits")));
        }
        if (profileData.containsKey("timePreferences")) {
            profile.setTimePreferences(asMap(profileData.get("timePreferences")));
        }
        if (profileData.containsKey("contentPreferences")) {
            profile.setContentPreferences(asMap(profileData.get("contentPreferences")));
        }
        if (profileData.containsKey("technicalSkills")) {
            profile.setTechnicalSkills(asMap(profileData.get("technicalSkills")));
        }
        if (profileData.containsKey("experienceLevel")) {
            profile.setExperienceLevel(asString(profileData.get("experienceLevel")));
        }
        if (profileData.containsKey("learningGoals")) {
            profile.setLearningGoals(asMap(profileData.get("learningGoals")));
        }
        if (profileData.containsKey("industryFocus")) {
            profile.setIndustryFocus(asMap(profileData.get("industryFocus")));
        }
        if (profileData.containsKey("codingHabits")) {
            profile.setCodingHabits(asMap(profileData.get("codingHabits")));
        }
        if (profileData.containsKey("toolPreferences")) {
            profile.setToolPreferences(asMap(profileData.get("toolPreferences")));
        }
        if (profileData.containsKey("contentFormats")) {
            profile.setContentFormats(asMap(profileData.get("contentFormats")));
        }
        if (profileData.containsKey("timeAvailability")) {
            profile.setTimeAvailability(asMap(profileData.get("timeAvailability")));
        }
        if (profileData.containsKey("confidenceScore")) {
            Object confidenceScoreObj = profileData.get("confidenceScore");
            if (confidenceScoreObj instanceof Double) {
                profile.setConfidenceScore(BigDecimal.valueOf((Double) confidenceScoreObj));
            } else if (confidenceScoreObj instanceof Integer) {
                profile.setConfidenceScore(BigDecimal.valueOf((Integer) confidenceScoreObj));
            } else if (confidenceScoreObj instanceof String) {
                try {
                    profile.setConfidenceScore(new BigDecimal((String) confidenceScoreObj));
                } catch (NumberFormatException e) {
                    // 忽略无效的置信度分数
                }
            }
        } else {
            // 如果没有置信度分数，设置一个默认值
            profile.setConfidenceScore(BigDecimal.valueOf(0.5));
        }

        try {
            return userProfileRepository.save(profile);
        } catch (Exception e) {
            // 数据库连接失败时，直接返回更新后的内存中的用户画像
            System.err.println("Database connection failed, returning updated in-memory profile: " + e.getMessage());
            return profile;
        }
    }

    public UserProfile getProfile(Long userId) {
        return getOrCreateProfile(userId);
    }

    public UserProfile getProfileByEmail(String email) {
        Optional<UserProfile> profile = userProfileRepository.findByUserEmail(email);
        return profile.orElse(null);
    }

    private Map<String, Object> asMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                Object key = entry.getKey();
                if (key != null) {
                    normalized.put(String.valueOf(key), entry.getValue());
                }
            }
            return normalized;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return Map.of();
        }
        return Map.of(text, 1);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
