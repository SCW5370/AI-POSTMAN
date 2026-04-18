package com.aipostman.controller;

import com.aipostman.client.SmtpEmailClient;
import com.aipostman.common.ApiResponse;
import com.aipostman.service.SystemHealthService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final SmtpEmailClient smtpEmailClient;
    private final SystemHealthService systemHealthService;

    public HealthController(SmtpEmailClient smtpEmailClient, SystemHealthService systemHealthService) {
        this.smtpEmailClient = smtpEmailClient;
        this.systemHealthService = systemHealthService;
    }

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "ok"));
    }

    @GetMapping("/readiness")
    public ApiResponse<Map<String, Object>> readiness() {
        return ApiResponse.ok(systemHealthService.readiness());
    }

    @PostMapping("/test-email")
    public ApiResponse<Map<String, String>> sendTestEmail(@RequestBody Map<String, String> request) {
        String toEmail = request.get("to");
        if (toEmail == null || toEmail.isEmpty()) {
            return ApiResponse.fail("Missing 'to' email address");
        }

        String subject = "AI 送报员 - 测试邮件";
        String html = "<p>您好！</p><p>这是一封来自 AI 送报员的测试邮件，用于验证邮件发送功能是否正常。</p><p>如果您收到此邮件，说明邮件发送功能工作正常。</p><p>此致，<br>AI 送报员团队</p>";

        boolean sent = smtpEmailClient.sendEmail(toEmail, subject, html);
        if (sent) {
            return ApiResponse.ok(Map.of("status", "success", "message", "测试邮件已成功发送，请检查您的邮箱"));
        } else {
            return ApiResponse.fail("邮件发送失败");
        }
    }
}
