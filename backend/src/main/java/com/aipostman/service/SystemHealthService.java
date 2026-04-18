package com.aipostman.service;

import com.aipostman.repository.SourceRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthService {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final SourceRepository sourceRepository;
    private final HttpClient httpClient;

    @Value("${app.worker.base-url:http://127.0.0.1:8000}")
    private String workerBaseUrl;

    @Value("${LLM_API_KEY:}")
    private String llmApiKey;

    @Value("${LLM_MODEL:}")
    private String llmModel;

    @Value("${LLM_BASE_URL:${LLM_API_BASE:}}")
    private String llmBaseUrl;

    @Value("${SMTP_HOST:}")
    private String smtpHost;

    @Value("${SMTP_PORT:}")
    private String smtpPort;

    @Value("${SMTP_USERNAME:}")
    private String smtpUsername;

    @Value("${SMTP_PASSWORD:}")
    private String smtpPassword;

    public SystemHealthService(
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory,
            SourceRepository sourceRepository
    ) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.sourceRepository = sourceRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Map<String, Object> readiness() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("database", component(checkDatabase(), "database ok", "database unavailable"));
        data.put("redis", component(checkRedis(), "redis ok", "redis unavailable"));
        data.put("worker", component(checkWorker(), "worker ok", "worker unavailable"));
        data.put("llm", component(isLlmConfigured(), "llm configured", "llm not configured"));
        data.put("smtp", component(isSmtpConfigured(), "smtp configured", "smtp not configured"));
        data.put("sources", Map.of(
                "ok", sourceRepository.count() > 0,
                "count", sourceRepository.count(),
                "message", sourceRepository.count() > 0 ? "sources available" : "no sources seeded"
        ));

        boolean ready = checkDatabase() && checkRedis() && checkWorker() && isLlmConfigured() && isSmtpConfigured();
        data.put("ready", ready);
        data.put("message", ready
                ? "system is ready for first-email acceptance"
                : "system is not ready for first-email acceptance");
        return data;
    }

    private Map<String, Object> component(boolean ok, String healthyMessage, String unhealthyMessage) {
        return Map.of(
                "ok", ok,
                "message", ok ? healthyMessage : unhealthyMessage
        );
    }

    private boolean checkDatabase() {
        try (Connection ignored = dataSource.getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            return "PONG".equalsIgnoreCase(redisConnectionFactory.getConnection().ping());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkWorker() {
        String base = workerBaseUrl == null ? "" : workerBaseUrl.trim();
        if (base.isEmpty()) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300 && response.body().contains("ok");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLlmConfigured() {
        return isRealValue(llmApiKey) && isRealValue(llmModel) && isRealValue(llmBaseUrl);
    }

    private boolean isSmtpConfigured() {
        return isRealValue(smtpHost) && isRealValue(smtpPort) && isRealValue(smtpUsername) && isRealValue(smtpPassword);
    }

    private boolean isRealValue(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return !normalized.contains("YOUR_")
                && !normalized.contains("your@example.com")
                && !normalized.contains("your@qq.com")
                && !normalized.contains("example.com");
    }
}
