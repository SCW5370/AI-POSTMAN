package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.service.ConfigService;
import com.aipostman.dto.request.ConfigRequest;
import com.aipostman.dto.response.ConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ApiResponse<ConfigResponse> getConfig() {
        ConfigResponse config = configService.getConfig();
        return ApiResponse.ok(config);
    }

    @PostMapping
    public ApiResponse<Void> saveConfig(@RequestBody ConfigRequest request) {
        configService.saveConfig(request);
        return ApiResponse.ok();
    }
}
