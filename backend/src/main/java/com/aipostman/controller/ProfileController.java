package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.domain.UserProfile;
import com.aipostman.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserProfileService userProfileService;

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfile> getMyProfile(@RequestParam Long userId) {
        UserProfile profile = userProfileService.getProfile(userId);
        return ApiResponse.ok(profile);
    }

    @PostMapping("/update/chat")
    public ApiResponse<UserProfile> updateProfileFromChat(@RequestParam Long userId, @RequestBody String chatHistory) {
        UserProfile profile = userProfileService.updateProfileFromChat(userId, chatHistory);
        return ApiResponse.ok(profile);
    }

    @PostMapping("/update/survey")
    public ApiResponse<UserProfile> updateProfileFromSurvey(@RequestParam Long userId, @RequestBody Map<String, Object> surveyData) {
        UserProfile profile = userProfileService.updateProfileFromSurvey(userId, surveyData);
        return ApiResponse.ok(profile);
    }

    @GetMapping("/by-email")
    public ApiResponse<UserProfile> getProfileByEmail(@RequestParam String email) {
        UserProfile profile = userProfileService.getProfileByEmail(email);
        return ApiResponse.ok(profile);
    }
}
