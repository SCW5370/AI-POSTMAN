package com.aipostman.client;

import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class WorkerClient {

    private final WebClient webClient;
    private final Duration fetchTimeout;
    private final Duration editorialTimeout;
    private final Duration finalizeTimeout;

    public WorkerClient(
            @Value("${app.worker.base-url}") String workerBaseUrl,
            @Value("${app.worker.fetch-timeout-seconds:25}") long fetchTimeoutSeconds,
            @Value("${app.worker.editorial-timeout-seconds:8}") long editorialTimeoutSeconds,
            @Value("${app.worker.finalize-timeout-seconds:70}") long finalizeTimeoutSeconds
    ) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
        this.webClient = WebClient.builder()
                .baseUrl(workerBaseUrl)
                .exchangeStrategies(strategies)
                .build();
        this.fetchTimeout = Duration.ofSeconds(fetchTimeoutSeconds);
        this.editorialTimeout = Duration.ofSeconds(editorialTimeoutSeconds);
        this.finalizeTimeout = Duration.ofSeconds(finalizeTimeoutSeconds);
    }

    public FetchResponse fetch(List<WorkerSourcePayload> sources) {
        try {
            return webClient.post()
                    .uri("/api/fetch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new FetchRequest(sources))
                    .retrieve()
                    .bodyToMono(FetchResponse.class)
                    .block(fetchTimeout);
        } catch (WebClientResponseException e) {
            System.err.println("Worker fetch error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch sources: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch sources", e);
        }
    }

    public EditorialDecisionResponse decide(EditorialDecisionRequest request) {
        try {
            return webClient.post()
                    .uri("/api/editorial/decide")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EditorialDecisionResponse.class)
                    .block(editorialTimeout);
        } catch (WebClientResponseException e) {
            System.err.println("Worker decide error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to make editorial decision: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to make editorial decision", e);
        }
    }

    public FinalizeDigestResponse finalizeDigest(FinalizeDigestRequest request) {
        try {
            return webClient.post()
                    .uri("/api/editorial/finalize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FinalizeDigestResponse.class)
                    .block(finalizeTimeout);
        } catch (WebClientResponseException e) {
            System.err.println("Worker finalize error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to finalize digest: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to finalize digest", e);
        }
    }

    public List<WorkerSourcePayload> discoverSources(String topic, int batchSize, int timeoutSeconds) {
        try {
            return webClient.post()
                    .uri("/api/discover/sources")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new DiscoverSourcesRequest(topic, batchSize))
                    .retrieve()
                    .bodyToFlux(WorkerSourcePayload.class)
                    .collectList()
                    .block(Duration.ofSeconds(timeoutSeconds));
        } catch (WebClientResponseException e) {
            System.err.println("Worker discover sources error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to discover sources: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to discover sources", e);
        }
    }

    public EvaluateCandidateResponse evaluateSourceCandidate(String url, String topic, String sourceType, double confidence) {
        try {
            return webClient.post()
                    .uri("/api/discover/evaluate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new EvaluateCandidateRequest(url, topic, sourceType, confidence))
                    .retrieve()
                    .bodyToMono(EvaluateCandidateResponse.class)
                    .block(Duration.ofSeconds(10));
        } catch (WebClientResponseException e) {
            System.err.println("Worker evaluate error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to evaluate source candidate: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate source candidate", e);
        }
    }

    public Map<String, Object> analyzeChatForProfile(String chatHistory) {
        try {
            return webClient.post()
                    .uri("/api/profile/analyze-chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new AnalyzeChatRequest(chatHistory))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(15));
        } catch (WebClientResponseException e) {
            System.err.println("Worker analyze chat error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to analyze chat for profile: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze chat for profile", e);
        }
    }

    public Map<String, Object> analyzeSurveyForProfile(Map<String, Object> surveyData) {
        try {
            return webClient.post()
                    .uri("/api/profile/analyze-survey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new AnalyzeSurveyRequest(surveyData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));
        } catch (WebClientResponseException e) {
            System.err.println("Worker analyze survey error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to analyze survey for profile: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze survey for profile", e);
        }
    }

    public List<WorkerSourcePayload> discoverWithAgent(String topic, int batchSize, int timeoutSeconds) {
        try {
            return webClient.post()
                    .uri("/api/discover/agent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new DiscoverAgentRequest(topic, batchSize))
                    .retrieve()
                    .bodyToFlux(WorkerSourcePayload.class)
                    .collectList()
                    .block(Duration.ofSeconds(timeoutSeconds));
        } catch (WebClientResponseException e) {
            System.err.println("Worker agent discovery error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to discover with agent: " + e.getMessage(), e);
        } catch (Exception e) {
            String detail = e.getMessage();
            throw new RuntimeException(
                    (detail == null || detail.isBlank())
                            ? "Failed to discover with agent"
                            : "Failed to discover with agent: " + detail,
                    e
            );
        }
    }

    public record WorkerSourcePayload(Long id, String name, String url, String sourceType, Integer priority, String language, Double confidence, String discoveryMethod) {}
    public record FetchRequest(List<WorkerSourcePayload> sources) {}
    public record FetchResponse(List<WorkerItemPayload> items) {}
    public record EditorialPreferencePayload(
            String goals,
            List<String> preferredTopics,
            List<String> blockedTopics,
            String deliveryMode,
            Double explorationRatio,
            Integer maxItemsPerDigest
    ) {}
    public record EditorialCandidatePayload(
            Long normalizedItemId,
            String title,
            String summary,
            String sourceName,
            List<String> tags,
            Double ruleScore
    ) {}
    public record EditorialDecisionRequest(
            EditorialPreferencePayload preference,
            List<EditorialCandidatePayload> candidates
    ) {}
    public record EditorialDecisionResponse(
            List<EditorialDecisionItemPayload> decisions,
            Boolean llmUsed,
            String strategy,
            String fallbackReason
    ) {}
    public record EditorialDecisionItemPayload(
            Long normalizedItemId,
            String section,
            Double scoreAdjustment,
            String reason
    ) {}
    public record FinalizeDigestItemPayload(
            Long normalizedItemId,
            String title,
            String summary,
            String content,
            String sourceName,
            String section
    ) {}
    public record FinalizeDigestRequest(
            EditorialPreferencePayload preference,
            List<FinalizeDigestItemPayload> items,
            Map<String, Object> userProfile
    ) {}
    public record FinalizedDigestItemPayload(
            Long normalizedItemId,
            String shortSummary,
            String relevanceReason,
            String actionHint
    ) {}
    public record FinalizeDigestResponse(
            List<FinalizedDigestItemPayload> items,
            Boolean llmUsed,
            String strategy,
            String fallbackReason
    ) {}
    public record WorkerItemPayload(
            Long sourceId,
            String externalId,
            String title,
            String url,
            String author,
            String publishedAt,
            String summaryRaw,
            String contentRaw,
            String canonicalUrl,
            String titleClean,
            String summaryClean,
            String contentClean,
            List<String> tags,
            Double sourceQualityScore,
            Double freshnessScore,
            String dedupGroupKey,
            String shortSummary,
            String relevanceReason,
            String actionHint
    ) {}

    public record DiscoverSourcesRequest(String topic, int batchSize) {}

    public record EvaluateCandidateRequest(String url, String topic, String sourceType, double confidence) {}
    public record EvaluateCandidateResponse(boolean approve, String reason, double score) {}

    public record AnalyzeChatRequest(String chatHistory) {}
    public record AnalyzeSurveyRequest(Map<String, Object> surveyData) {}
    public record DiscoverAgentRequest(String topic, int batchSize) {}
}
