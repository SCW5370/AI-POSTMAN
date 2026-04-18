package com.aipostman.service;

import com.aipostman.dto.request.ConfigRequest;
import com.aipostman.dto.response.ConfigResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    @Value("${LLM_API_KEY:}")
    private String llmApiKey;

    @Value("${LLM_API_BASE:${LLM_BASE_URL:}}")
    private String llmApiBase;

    @Value("${LLM_MODEL:}")
    private String llmModel;

    @Value("${SMTP_HOST:}")
    private String smtpHost;

    @Value("${SMTP_PORT:}")
    private String smtpPort;

    @Value("${SMTP_USERNAME:}")
    private String smtpUsername;

    @Value("${SMTP_PASSWORD:}")
    private String smtpPassword;

    @Value("${DB_URL:}")
    private String dbUrl;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${WORKER_BASE_URL:}")
    private String workerBaseUrl;

    @Value("${SERVER_PORT:}")
    private String serverPort;

    @Value("${APP_PUBLIC_BASE_URL:}")
    private String publicBaseUrl;

    public ConfigResponse getConfig() {
        ConfigResponse response = new ConfigResponse();
        response.setLlmApiKey(readEnvValue("LLM_API_KEY", llmApiKey));
        response.setLlmApiBase(readEnvValue("LLM_API_BASE", readEnvValue("LLM_BASE_URL", llmApiBase)));
        response.setLlmModel(readEnvValue("LLM_MODEL", llmModel));
        response.setSmtpUsername(readEnvValue("SMTP_USERNAME", smtpUsername));
        response.setSmtpPassword(readEnvValue("SMTP_PASSWORD", smtpPassword));
        return response;
    }

    public void saveConfig(ConfigRequest request) {
        try {
            Path envPath = resolveProjectRoot().resolve(".env");
            Map<String, String> updates = new LinkedHashMap<>();

            putIfPresent(updates, "LLM_API_KEY", request.getLlmApiKey());
            putIfPresent(updates, "LLM_BASE_URL", request.getLlmApiBase());
            putIfPresent(updates, "LLM_API_BASE", request.getLlmApiBase());
            putIfPresent(updates, "LLM_MODEL", request.getLlmModel());
            putIfPresent(updates, "SMTP_USERNAME", request.getSmtpUsername());
            putIfPresent(updates, "SMTP_PASSWORD", request.getSmtpPassword());

            ensureDefault(updates, "DB_URL", valueOrDefault(dbUrl, "jdbc:postgresql://127.0.0.1:5432/aipostman"));
            ensureDefault(updates, "DB_USERNAME", valueOrDefault(dbUsername, "aipostman"));
            ensureDefault(updates, "DB_PASSWORD", valueOrDefault(dbPassword, "aipostman"));
            ensureDefault(updates, "SMTP_HOST", valueOrDefault(smtpHost, "smtp.qq.com"));
            ensureDefault(updates, "SMTP_PORT", valueOrDefault(smtpPort, "587"));
            ensureDefault(updates, "WORKER_BASE_URL", valueOrDefault(workerBaseUrl, "http://127.0.0.1:8000"));
            ensureDefault(updates, "SERVER_PORT", valueOrDefault(serverPort, "8080"));
            ensureDefault(updates, "APP_PUBLIC_BASE_URL", valueOrDefault(publicBaseUrl, "http://127.0.0.1:8080"));

            updateEnvFile(envPath, updates);
            logger.info("配置保存成功");
        } catch (Exception e) {
            logger.error("保存配置失败", e);
            throw new RuntimeException("保存配置失败", e);
        }
    }

    private Path resolveProjectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; i < 5 && current != null; i++) {
            boolean hasProjectLayout = Files.isDirectory(current.resolve("backend"))
                    && Files.isDirectory(current.resolve("frontend"))
                    && Files.isDirectory(current.resolve("worker"));
            if (hasProjectLayout) {
                return current;
            }
            current = current.getParent();
        }
        current = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; i < 5 && current != null; i++) {
            if (Files.exists(current.resolve(".env")) || Files.exists(current.resolve(".env.example"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位项目根目录，请确认服务从项目目录启动");
    }

    private void updateEnvFile(Path envPath, Map<String, String> updates) throws IOException {
        List<String> existingLines = Files.exists(envPath)
                ? Files.readAllLines(envPath, StandardCharsets.UTF_8)
                : List.of();
        List<String> rewritten = new ArrayList<>();
        Map<String, Boolean> touched = new LinkedHashMap<>();
        for (String key : updates.keySet()) {
            touched.put(key, false);
        }

        for (String line : existingLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                rewritten.add(line);
                continue;
            }

            int idx = trimmed.indexOf('=');
            String key = trimmed.substring(0, idx).trim();
            if (!updates.containsKey(key)) {
                rewritten.add(line);
                continue;
            }

            rewritten.add(key + "=" + updates.get(key));
            touched.put(key, true);
        }

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            if (Boolean.TRUE.equals(touched.get(entry.getKey()))) {
                continue;
            }
            rewritten.add(entry.getKey() + "=" + entry.getValue());
        }

        Files.write(envPath, rewritten, StandardCharsets.UTF_8);
    }

    private void putIfPresent(Map<String, String> updates, String key, String value) {
        if (value != null && !value.isBlank()) {
            updates.put(key, value.trim());
        }
    }

    private String readEnvValue(String key, String fallback) {
        Path envPath = resolveProjectRoot().resolve(".env");
        if (!Files.exists(envPath)) {
            return fallback;
        }
        try {
            for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                String lineKey = trimmed.substring(0, idx).trim();
                if (!key.equals(lineKey)) {
                    continue;
                }
                return trimmed.substring(idx + 1).trim();
            }
        } catch (IOException ignored) {
            // fall back to injected value when reading .env fails
        }
        return fallback;
    }

    private void ensureDefault(Map<String, String> updates, String key, String value) {
        if (!updates.containsKey(key) && value != null && !value.isBlank()) {
            updates.put(key, value.trim());
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

}
