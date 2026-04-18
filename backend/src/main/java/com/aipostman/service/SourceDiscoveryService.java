package com.aipostman.service;

import com.aipostman.client.WorkerClient;
import com.aipostman.common.enums.DeliveryMode;
import com.aipostman.domain.SourceCandidate;
import com.aipostman.domain.UserPreference;
import com.aipostman.common.enums.SourceType;
import com.aipostman.repository.SourceRepository;
import com.aipostman.repository.SourceCandidateRepository;
import com.aipostman.repository.UserPreferenceRepository;
import com.aipostman.repository.UserProfileRepository;
import com.aipostman.service.CacheService;
import com.aipostman.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class SourceDiscoveryService {

    @Autowired
    private WorkerClient workerClient;

    @Autowired
    private SourceCandidateRepository sourceCandidateRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private SourceCandidateService sourceCandidateService;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private MonitoringService monitoringService;

    @Value("${SOURCE_DISCOVERY_BATCH_SIZE:10}")
    private int batchSize;

    @Value("${SOURCE_DISCOVERY_TIMEOUT_SECONDS:30}")
    private int timeoutSeconds;

    @Value("${SOURCE_DISCOVERY_CACHE_TTL_HOURS:24}")
    private int cacheTtlHours;

    @Value("${app.discovery.auto-enabled:true}")
    private boolean autoDiscoveryEnabled;

    @Value("${app.discovery.auto-conservative-max-topics:2}")
    private int conservativeMaxTopics;

    @Value("${app.discovery.auto-conservative-max-approvals-per-run:2}")
    private int conservativeMaxApprovalsPerRun;

    @Value("${app.discovery.auto-conservative-min-confidence:80}")
    private double conservativeMinConfidence;

    @Value("${app.discovery.auto-aggressive-max-topics:5}")
    private int aggressiveMaxTopics;

    @Value("${app.discovery.auto-aggressive-max-approvals-per-run:8}")
    private int aggressiveMaxApprovalsPerRun;

    @Value("${app.discovery.auto-aggressive-min-confidence:60}")
    private double aggressiveMinConfidence;

    @Value("${app.discovery.auto-conservative-cooldown-minutes:720}")
    private int conservativeCooldownMinutes;

    @Value("${app.discovery.auto-aggressive-cooldown-minutes:180}")
    private int aggressiveCooldownMinutes;

    public CompletableFuture<List<SourceCandidate>> discoverSources(String userId, String topic) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 尝试从缓存获取
                String cacheKey = cacheService.generateSourceDiscoveryKey(topic);
                List<SourceCandidate> cachedResult = cacheService.get(cacheKey, List.class)
                        .orElse(null);
                if (cachedResult != null) {
                    return cachedResult;
                }

                // 调用 worker 进行源发现并保存
                List<WorkerClient.WorkerSourcePayload> discoveredSources = workerClient.discoverSources(topic, batchSize, timeoutSeconds);
                List<SourceCandidate> result = saveCandidates(topic, discoveredSources, "pipeline");
                
                // 缓存结果
                cacheService.set(cacheKey, result, Duration.ofHours(cacheTtlHours));
                
                return result;
            } catch (Exception e) {
                throw new RuntimeException("Failed to discover sources", e);
            }
        });
    }

    public CompletableFuture<List<SourceCandidate>> discoverSourcesWithAgent(String userId, String topic) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                monitoringService.recordAgentDiscovery();
                List<WorkerClient.WorkerSourcePayload> discoveredSources =
                        workerClient.discoverWithAgent(topic, batchSize, timeoutSeconds);
                return saveCandidates(topic, discoveredSources, "agent");
            } catch (Exception e) {
                monitoringService.recordError();
                // Fallback to standard discover pipeline so auto-discovery remains available.
                try {
                    List<WorkerClient.WorkerSourcePayload> discoveredSources =
                            workerClient.discoverSources(topic, batchSize, timeoutSeconds);
                    return saveCandidates(topic, discoveredSources, "agent_fallback");
                } catch (Exception ex) {
                    String detail = ex.getMessage();
                    throw new RuntimeException(
                            (detail == null || detail.isBlank())
                                    ? "Failed to discover sources with agent"
                                    : "Failed to discover sources with agent: " + detail,
                            ex
                    );
                }
            }
        });
    }

    public List<SourceCandidate> getDiscoveredSources(String topic) {
        return sourceCandidateRepository.findByTopic(topic);
    }

    public AutoDiscoveryResult autoDiscoverAndApproveFromUserProfile(Long userId) {
        return autoDiscoverAndApproveFromUserProfile(userId, null);
    }

    public AutoDiscoveryResult autoDiscoverAndApproveFromUserProfile(Long userId, String strategyOverride) {
        if (!autoDiscoveryEnabled) {
            return new AutoDiscoveryResult("disabled", List.of(), 0, 0, 0, List.of(), List.of());
        }
        DiscoveryStrategy strategy = resolveStrategy(userId, strategyOverride);
        StrategyPolicy policy = policyFor(strategy);
        List<String> topics = collectTopics(userId, policy.maxTopics());
        if (topics.isEmpty()) {
            return new AutoDiscoveryResult(strategy.name().toLowerCase(), List.of(), 0, 0, 0, List.of(), List.of());
        }

        int discoveredCandidates = 0;
        int approvedCandidates = 0;
        int skippedByCooldown = 0;
        List<Long> approvedCandidateIds = new ArrayList<>();
        List<Long> approvedSourceIds = new ArrayList<>();

        for (String topic : topics) {
            if (approvedCandidates >= policy.maxApprovalsPerRun()) {
                break;
            }
            if (!allowTopicDiscovery(userId, strategy.name().toLowerCase(), topic, policy.cooldownMinutes())) {
                skippedByCooldown++;
                continue;
            }
            List<SourceCandidate> candidates;
            try {
                candidates = discoverSourcesWithAgent(String.valueOf(userId), topic).join();
            } catch (Exception ex) {
                monitoringService.recordError();
                // Keep auto-discovery best-effort: one topic failure must not abort the full run.
                continue;
            }
            discoveredCandidates += candidates.size();
            List<SourceCandidate> pending = candidates.stream()
                    .filter(candidate -> "pending".equalsIgnoreCase(candidate.getStatus()))
                    .filter(candidate -> candidate.getConfidence() != null
                            && candidate.getConfidence().doubleValue() >= policy.minConfidence())
                    .sorted((a, b) -> b.getConfidence().compareTo(a.getConfidence()))
                    .collect(Collectors.toList());
            for (SourceCandidate candidate : pending) {
                if (approvedCandidates >= policy.maxApprovalsPerRun()) {
                    break;
                }
                try {
                    SourceCandidate approved = sourceCandidateService.approveCandidate(candidate.getId());
                    if (!"approved".equalsIgnoreCase(approved.getStatus())) {
                        continue;
                    }
                    approvedCandidates++;
                    approvedCandidateIds.add(approved.getId());
                    sourceRepository.findByUrl(approved.getUrl())
                            .map(source -> source.getId())
                            .ifPresent(approvedSourceIds::add);
                } catch (Exception ignored) {
                    // Best-effort auto discovery should not block digest build.
                }
            }
        }
        List<Long> uniqueSourceIds = approvedSourceIds.stream().distinct().toList();
        return new AutoDiscoveryResult(
                strategy.name().toLowerCase(),
                topics,
                discoveredCandidates,
                approvedCandidates,
                skippedByCooldown,
                approvedCandidateIds,
                uniqueSourceIds
        );
    }

    private List<SourceCandidate> saveCandidates(
            String topic,
            List<WorkerClient.WorkerSourcePayload> discoveredSources,
            String methodPrefix
    ) {
        for (WorkerClient.WorkerSourcePayload sourcePayload : discoveredSources) {
            if (sourcePayload == null || sourcePayload.url() == null || sourcePayload.url().isBlank()) {
                continue;
            }
            if (sourceCandidateRepository.findByUrl(sourcePayload.url()) != null) {
                continue;
            }
            SourceCandidate candidate = new SourceCandidate();
            candidate.setQuery(topic);
            candidate.setTopic(topic);
            candidate.setUrl(sourcePayload.url());
            candidate.setSourceType(resolveSourceType(sourcePayload.sourceType()));
            candidate.setConfidence(BigDecimal.valueOf(sourcePayload.confidence() == null ? 60.0 : sourcePayload.confidence()));
            String method = sourcePayload.discoveryMethod() == null || sourcePayload.discoveryMethod().isBlank()
                    ? methodPrefix
                    : methodPrefix + ":" + sourcePayload.discoveryMethod();
            candidate.setDiscoveryMethod(method);
            candidate.setStatus("pending");
            sourceCandidateRepository.save(candidate);
        }
        return sourceCandidateRepository.findByTopic(topic);
    }

    private SourceType resolveSourceType(String raw) {
        if (raw == null || raw.isBlank()) {
            return SourceType.RSS;
        }
        try {
            return SourceType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            String normalized = raw.trim().toUpperCase();
            if (normalized.startsWith("GITHUB")) {
                return SourceType.GITHUB_FEED;
            }
            if (normalized.contains("ATOM")) {
                return SourceType.ATOM;
            }
            if (normalized.contains("JSON")) {
                return SourceType.JSON_FEED;
            }
            return SourceType.RSS;
        }
    }

    private List<String> collectTopics(Long userId, int maxTopics) {
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        userPreferenceRepository.findByUserId(userId)
                .map(UserPreference::getPreferredTopics)
                .ifPresent(values -> values.forEach(value -> addTopic(topics, value)));
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            addTopicsFromMap(topics, profile.getPreferredTopics());
            addTopicsFromMap(topics, profile.getInterests());
            addTopic(topics, profile.getOccupation());
            addTopic(topics, profile.getIndustryFocus() == null ? null : profile.getIndustryFocus().keySet().stream().findFirst().orElse(null));
        });
        return topics.stream()
                .filter(topic -> topic.length() >= 2)
                .limit(Math.max(1, maxTopics))
                .toList();
    }

    private void addTopicsFromMap(LinkedHashSet<String> topics, Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String key : values.keySet()) {
            addTopic(topics, key);
        }
    }

    private void addTopic(LinkedHashSet<String> topics, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return;
        }
        String compact = normalized.replaceAll("\\s+", " ");
        topics.add(compact);
    }

    private DiscoveryStrategy resolveStrategy(Long userId, String strategyOverride) {
        if (strategyOverride != null && !strategyOverride.isBlank()) {
            try {
                return DiscoveryStrategy.valueOf(strategyOverride.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fallback to user preference mapping
            }
        }
        return userPreferenceRepository.findByUserId(userId)
                .map(UserPreference::getDeliveryMode)
                .map(mode -> mode == DeliveryMode.HUNTER ? DiscoveryStrategy.AGGRESSIVE : DiscoveryStrategy.CONSERVATIVE)
                .orElse(DiscoveryStrategy.CONSERVATIVE);
    }

    private StrategyPolicy policyFor(DiscoveryStrategy strategy) {
        if (strategy == DiscoveryStrategy.AGGRESSIVE) {
            return new StrategyPolicy(
                    Math.max(1, aggressiveMaxTopics),
                    Math.max(1, aggressiveMaxApprovalsPerRun),
                    Math.max(0, aggressiveMinConfidence),
                    Math.max(1, aggressiveCooldownMinutes)
            );
        }
        return new StrategyPolicy(
                Math.max(1, conservativeMaxTopics),
                Math.max(1, conservativeMaxApprovalsPerRun),
                Math.max(0, conservativeMinConfidence),
                Math.max(1, conservativeCooldownMinutes)
        );
    }

    private enum DiscoveryStrategy {
        CONSERVATIVE,
        AGGRESSIVE
    }

    private record StrategyPolicy(int maxTopics, int maxApprovalsPerRun, double minConfidence, int cooldownMinutes) {
    }

    private boolean allowTopicDiscovery(Long userId, String strategy, String topic, int cooldownMinutes) {
        String key = "source:auto-discovery:cooldown:" + userId + ":" + strategy + ":" + topic.toLowerCase();
        boolean inCooldown = cacheService.get(key, Boolean.class).orElse(false);
        if (inCooldown) {
            return false;
        }
        cacheService.set(key, Boolean.TRUE, Duration.ofMinutes(cooldownMinutes));
        return true;
    }

    public record AutoDiscoveryResult(
            String strategy,
            List<String> topics,
            int discoveredCandidates,
            int approvedCandidates,
            int skippedByCooldown,
            List<Long> approvedCandidateIds,
            List<Long> approvedSourceIds
    ) {
    }
}
