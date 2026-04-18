package com.aipostman.service;

import com.aipostman.domain.User;
import com.aipostman.domain.UserPreference;
import com.aipostman.dto.request.SavePreferenceRequest;
import com.aipostman.dto.response.PreferenceResponse;
import com.aipostman.repository.UserPreferenceRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {

    private final UserService userService;
    private final UserPreferenceRepository preferenceRepository;

    public PreferenceService(UserService userService, UserPreferenceRepository preferenceRepository) {
        this.userService = userService;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional
    public PreferenceResponse save(Long userId, SavePreferenceRequest request) {
        User user = userService.getUser(userId);
        UserPreference preference = preferenceRepository.findByUserId(userId).orElseGet(UserPreference::new);
        preference.setUser(user);
        if (preference.getPreferredTopics() == null) {
            preference.setPreferredTopics(new ArrayList<>());
        }
        if (preference.getBlockedTopics() == null) {
            preference.setBlockedTopics(new ArrayList<>());
        }
        if (preference.getPreferredSources() == null) {
            preference.setPreferredSources(new ArrayList<>());
        }
        if (request.goals() != null) {
            preference.setGoals(request.goals());
        }
        if (request.preferredTopics() != null) {
            preference.setPreferredTopics(request.preferredTopics());
        }
        if (request.blockedTopics() != null) {
            preference.setBlockedTopics(request.blockedTopics());
        }
        if (request.preferredSources() != null) {
            preference.setPreferredSources(request.preferredSources());
        }
        if (request.deliveryMode() != null) {
            preference.setDeliveryMode(request.deliveryMode());
        }
        if (request.deliveryTime() != null && !request.deliveryTime().isBlank()) {
            preference.setDeliveryTime(request.deliveryTime());
        }
        if (request.deliveryFrequency() != null && !request.deliveryFrequency().isBlank()) {
            preference.setDeliveryFrequency(request.deliveryFrequency());
        }
        if (request.maxItemsPerDigest() != null) {
            preference.setMaxItemsPerDigest(request.maxItemsPerDigest());
        }
        if (request.explorationRatio() != null) {
            preference.setExplorationRatio(request.explorationRatio());
        } else if (preference.getExplorationRatio() == null) {
            preference.setExplorationRatio(BigDecimal.valueOf(0.10));
        }
        UserPreference saved = preferenceRepository.save(preference);
        return toResponse(saved);
    }

    public PreferenceResponse get(Long userId) {
        return toResponse(getPreference(userId));
    }

    public UserPreference getPreference(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
    }

    @Transactional
    public UserPreference createDefaultPreference(Long userId) {
        User user = userService.getUser(userId);
        UserPreference preference = new UserPreference();
        preference.setUser(user);
        preference.setPreferredTopics(new ArrayList<>());
        preference.setBlockedTopics(new ArrayList<>());
        preference.setPreferredSources(new ArrayList<>());
        if (preference.getExplorationRatio() == null) {
            preference.setExplorationRatio(BigDecimal.valueOf(0.10));
        }
        return preferenceRepository.save(preference);
    }

    private PreferenceResponse toResponse(UserPreference preference) {
        return new PreferenceResponse(
                preference.getUser().getId(),
                preference.getGoals(),
                preference.getPreferredTopics(),
                preference.getBlockedTopics(),
                preference.getPreferredSources(),
                preference.getDeliveryMode(),
                preference.getDeliveryTime(),
                preference.getDeliveryFrequency(),
                preference.getMaxItemsPerDigest(),
                preference.getExplorationRatio()
        );
    }
}
