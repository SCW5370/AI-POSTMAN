package com.aipostman.service;

import com.aipostman.client.WorkerClient;
import com.aipostman.common.enums.ActionHint;
import com.aipostman.common.enums.DigestSection;
import com.aipostman.common.enums.DigestStatus;
import com.aipostman.common.enums.ItemStatus;
import com.aipostman.domain.*;
import com.aipostman.dto.response.DigestItemResponse;
import com.aipostman.dto.response.DigestResponse;
import com.aipostman.dto.response.CandidateScoreResponse;
import com.aipostman.dto.response.DigestDebugResponse;
import com.aipostman.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DigestService {

    private final UserService userService;
    private final PreferenceService preferenceService;
    private final SourceService sourceService;
    private final RawItemRepository rawItemRepository;
    private final NormalizedItemRepository normalizedItemRepository;
    private final ItemEnrichmentRepository itemEnrichmentRepository;
    private final DailyDigestRepository dailyDigestRepository;
    private final DailyDigestItemRepository dailyDigestItemRepository;
    private final RankingService rankingService;
    private final PublicationDecisionService publicationDecisionService;
    private final WorkerClient workerClient;
    private final DigestRenderService digestRenderService;
    private final UserProfileService userProfileService;
    private final MonitoringService monitoringService;

    @Value("${app.digest.max-candidate-pool}")
    private int maxCandidatePool;

    @Value("${app.digest.min-item-score}")
    private double minItemScore;

    @Value("${app.digest.exclude-recent-sent-days:3}")
    private int excludeRecentSentDays;

    @Value("${app.digest.editorial-candidate-limit:6}")
    private int editorialCandidateLimit;

    @Value("${app.digest.force-llm-finalize:true}")
    private boolean forceLlmFinalize;

    @Value("${app.worker.fetch-batch-size:4}")
    private int fetchBatchSize;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public DigestService(
            UserService userService,
            PreferenceService preferenceService,
            SourceService sourceService,
            RawItemRepository rawItemRepository,
            NormalizedItemRepository normalizedItemRepository,
            ItemEnrichmentRepository itemEnrichmentRepository,
            DailyDigestRepository dailyDigestRepository,
            DailyDigestItemRepository dailyDigestItemRepository,
            RankingService rankingService,
            PublicationDecisionService publicationDecisionService,
            WorkerClient workerClient,
            DigestRenderService digestRenderService,
            UserProfileService userProfileService,
            MonitoringService monitoringService
    ) {
        this.userService = userService;
        this.preferenceService = preferenceService;
        this.sourceService = sourceService;
        this.rawItemRepository = rawItemRepository;
        this.normalizedItemRepository = normalizedItemRepository;
        this.itemEnrichmentRepository = itemEnrichmentRepository;
        this.dailyDigestRepository = dailyDigestRepository;
        this.dailyDigestItemRepository = dailyDigestItemRepository;
        this.rankingService = rankingService;
        this.publicationDecisionService = publicationDecisionService;
        this.workerClient = workerClient;
        this.digestRenderService = digestRenderService;
        this.userProfileService = userProfileService;
        this.monitoringService = monitoringService;
    }

    @Transactional
    public int fetchAndStore(List<Long> sourceIds) {
        monitoringService.recordFetchRequest();
        return fetchAndStoreBatched(sourceIds, fetchBatchSize);
    }

    @Transactional
    public int fetchAndStoreBatched(List<Long> sourceIds, int batchSize) {
        List<Source> sources = sourceService.getEnabledSources(sourceIds);
        if (sources.isEmpty()) {
            return 0;
        }
        int safeBatchSize = Math.max(1, batchSize);
        int savedCount = 0;
        for (int i = 0; i < sources.size(); i += safeBatchSize) {
            int toIndex = Math.min(sources.size(), i + safeBatchSize);
            savedCount += fetchAndStoreSources(sources.subList(i, toIndex));
        }
        return savedCount;
    }

    private int fetchAndStoreSources(List<Source> sources) {
        WorkerClient.FetchResponse response = workerClient.fetch(sources.stream()
                .map(source -> new WorkerClient.WorkerSourcePayload(
                        source.getId(),
                        source.getName(),
                        source.getUrl(),
                        source.getSourceType().name(),
                        source.getPriority(),
                        source.getLanguage(),
                        null,
                        null))
                .toList());

        int savedCount = 0;
        for (WorkerClient.WorkerItemPayload payload : response.items()) {
            Source source = sourceService.getSource(payload.sourceId());
            RawItem rawItem = rawItemRepository.findBySourceIdAndUrl(source.getId(), payload.url()).orElseGet(RawItem::new);
            boolean created = rawItem.getId() == null;
            rawItem.setSource(source);
            rawItem.setExternalId(payload.externalId());
            rawItem.setTitle(payload.title());
            rawItem.setUrl(payload.url());
            rawItem.setAuthor(payload.author());
            rawItem.setPublishedAt(parseDateTime(payload.publishedAt()));
            rawItem.setSummaryRaw(payload.summaryRaw());
            rawItem.setContentRaw(payload.contentRaw());
            rawItem.setRawHash(payload.dedupGroupKey());
            rawItem = rawItemRepository.save(rawItem);

            NormalizedItem normalized = normalizedItemRepository.findByRawItemId(rawItem.getId()).orElseGet(NormalizedItem::new);
            normalized.setRawItem(rawItem);
            normalized.setCanonicalUrl(payload.canonicalUrl());
            normalized.setTitleClean(payload.titleClean());
            normalized.setSummaryClean(payload.summaryClean());
            normalized.setContentClean(payload.contentClean());
            normalized.setLanguage(source.getLanguage());
            normalized.setTags(payload.tags() == null ? List.of() : payload.tags());
            normalized.setSourceQualityScore(BigDecimal.valueOf(payload.sourceQualityScore() == null ? source.getPriority() : payload.sourceQualityScore()));
            normalized.setFreshnessScore(BigDecimal.valueOf(payload.freshnessScore() == null ? 0.0 : payload.freshnessScore()));
            normalized.setDedupGroupKey(payload.dedupGroupKey());
            normalized.setStatus(ItemStatus.READY);
            normalized = normalizedItemRepository.save(normalized);

            ItemEnrichment enrichment = itemEnrichmentRepository.findByNormalizedItemId(normalized.getId()).orElseGet(ItemEnrichment::new);
            enrichment.setNormalizedItem(normalized);
            enrichment.setShortSummary(payload.shortSummary());
            enrichment.setRelevanceReason(payload.relevanceReason());
            if (payload.actionHint() != null) {
                try {
                    enrichment.setActionHint(ActionHint.valueOf(payload.actionHint().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    // 未知 actionHint 值，保留空
                }
            }
            enrichment.setLlmTags(payload.tags() == null ? List.of() : payload.tags());
            enrichment.setEnrichmentStatus("ready");
            itemEnrichmentRepository.save(enrichment);

            if (created) {
                savedCount++;
            }
            source.setLastFetchedAt(LocalDateTime.now());
        }
        return savedCount;
    }

    @Transactional
    public DigestResponse buildDigest(Long userId, LocalDate digestDate) {
        return buildDigest(userId, digestDate, false);
    }

    @Transactional
    public DigestResponse buildDigest(Long userId, LocalDate digestDate, boolean forceLlm) {
        monitoringService.recordDigestBuild();
        User user = userService.getUser(userId);
        UserPreference preference = preferenceService.getPreference(userId);

        DailyDigest existingDigest = dailyDigestRepository.findByUserIdAndDigestDateAndDigestType(userId, digestDate, "daily")
                .orElse(null);
        // 已 SENT 或有内容的 DRAFT 直接复用，不重新生成（避免覆盖已发送记录）
        // 若需强制重新生成，调用方应先手动将状态置为 PENDING 或传入 forceLlm=true
        if (!forceLlm && existingDigest != null && shouldReuseDigest(existingDigest)) {
            return toResponse(
                    existingDigest,
                    dailyDigestItemRepository.findByDigestIdOrderBySectionAscItemOrderAsc(existingDigest.getId()),
                    Map.of(),
                    new EditorialResult(
                            Map.of(),
                            existingDigest.getLlmUsed() != null ? existingDigest.getLlmUsed() : false,
                            existingDigest.getEditorialStrategy() != null ? existingDigest.getEditorialStrategy() : "heuristic",
                            null
                    )
            );
        }
        // 当 forceLlm 为 true 时，即使摘要已经发送过了，也强制重新生成，以测试发送功能是否正常
        if (forceLlm && existingDigest != null && existingDigest.getStatus() == DigestStatus.SENT) {
            // 重置摘要状态为 DRAFT，以便重新生成
            existingDigest.setStatus(DigestStatus.DRAFT);
            existingDigest.setSentAt(null);
        }

        DailyDigest digest = existingDigest == null ? new DailyDigest() : existingDigest;
        digest.setUser(user);
        digest.setDigestDate(digestDate);
        digest.setDigestType("daily");
        digest.setStatus(DigestStatus.DRAFT);
        // 仅在非重用路径才清空 sentAt，避免覆盖已发送记录
        if (existingDigest == null || existingDigest.getStatus() != DigestStatus.SENT) {
            digest.setSentAt(null);
        }

        Set<Long> recentSentItemIds = findRecentSentItemIds(userId, digestDate);
        List<ScoredItem> scoredItemsBase = normalizedItemRepository.findRecentReadyItems(LocalDateTime.now().minusDays(3)).stream()
                .filter(item -> notBlocked(preference, item))
                .map(item -> new ScoredItem(item, rankingService.score(userId, preference, item)))
                .sorted(Comparator.comparing(ScoredItem::score).reversed())
                .limit(Math.max(preference.getMaxItemsPerDigest(), maxCandidatePool))
                .toList();
        List<ScoredItem> scoredItems = scoredItemsBase.stream()
                .filter(item -> !recentSentItemIds.contains(item.item().getId()))
                .toList();

        EditorialResult editorialResult = fetchEditorialDecisions(preference, scoredItems);
        Map<Long, WorkerClient.EditorialDecisionItemPayload> editorialMap = editorialResult.decisions();
        List<ScoredItem> aiAdjusted = scoredItems.stream()
                .map(item -> applyEditorialDecision(item, editorialMap.get(item.item().getId())))
                .sorted(Comparator.comparing(ScoredItem::score).reversed())
                .toList();

        List<ScoredItem> selected = selectDigestItems(aiAdjusted, preference);
        boolean publishable = publicationDecisionService.shouldPublish(selected.stream().map(ScoredItem::score).toList());
        if (!publishable && !recentSentItemIds.isEmpty()) {
            // If recent-sent exclusion makes today's digest empty, fallback to best available items.
            List<ScoredItem> fallbackAdjusted = scoredItemsBase.stream()
                    .map(item -> applyEditorialDecision(item, editorialMap.get(item.item().getId())))
                    .sorted(Comparator.comparing(ScoredItem::score).reversed())
                    .toList();
            List<ScoredItem> fallbackSelected = selectDigestItems(fallbackAdjusted, preference);
            if (publicationDecisionService.shouldPublish(fallbackSelected.stream().map(ScoredItem::score).toList())) {
                selected = fallbackSelected;
                publishable = true;
            }
        }

        if (!publishable) {
            if (digest.getId() != null) {
                List<DailyDigestItem> oldItems = dailyDigestItemRepository.findByDigestIdOrderBySectionAscItemOrderAsc(digest.getId());
                if (!oldItems.isEmpty()) {
                    dailyDigestItemRepository.deleteAll(oldItems);
                }
            }
            digest.setStatus(DigestStatus.SKIPPED);
            digest.setSentAt(null);
            digest.setSubject("AI 送报员 | 今日无足够高价值内容");
            digest.setHtmlContent("<html><body><p>今天没有达到出刊阈值的内容，因此未发送日报。</p></body></html>");
            digest.setTotalItems(0);
            return toResponse(dailyDigestRepository.save(digest), List.of(), Map.of(), editorialResult);
        }

        FinalizeResult finalizeResult = finalizeSelectedItems(preference, selected);
        if ((forceLlm || forceLlmFinalize) && !finalizeResult.llmUsed()) {
            throw new IllegalStateException("LLM finalization is required but unavailable: " + finalizeResult.fallbackReason());
        }

        DailyDigest savedDigest = dailyDigestRepository.save(digest);
        List<DailyDigestItem> oldItems = dailyDigestItemRepository.findByDigestIdOrderBySectionAscItemOrderAsc(savedDigest.getId());
        if (!oldItems.isEmpty()) {
            dailyDigestItemRepository.deleteAll(oldItems);
        }

        List<DailyDigestItem> digestItems = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            ScoredItem item = selected.get(i);
            DailyDigestItem digestItem = new DailyDigestItem();
            digestItem.setDigest(savedDigest);
            digestItem.setNormalizedItem(item.item());
            digestItem.setItemOrder(i + 1);
            digestItem.setFinalScore(item.score());
            // 为pickSection创建enrichmentMap
            Map<Long, ItemEnrichment> enrichmentMap = new HashMap<>();
            digestItem.setSection(pickSection(item, i, selected.size(), preference, editorialMap.get(item.item().getId()), enrichmentMap));
            digestItems.add(digestItemRepositorySave(digestItem));
        }

        // 当finalizeResult.llmUsed()为true时，使用"llm"策略，而不是editorialResult的strategy
        boolean llmUsed = editorialResult.llmUsed() || finalizeResult.llmUsed();
        String strategy = finalizeResult.llmUsed() ? "llm" : editorialResult.strategy();
        
        // 创建新的EditorialResult，使用计算后的llmUsed和strategy
        EditorialResult updatedEditorialResult = new EditorialResult(
                editorialMap,
                llmUsed,
                strategy,
                editorialResult.fallbackReason()
        );
        
        DigestResponse response = toResponse(
                savedDigest,
                digestItems,
                editorialMap,
                updatedEditorialResult
        );
        savedDigest.setTotalItems(digestItems.size());
        savedDigest.setSubject(buildSubject(digestDate, digestItems.size()));
        savedDigest.setHtmlContent(digestRenderService.renderHtml(savedDigest, response));
        savedDigest.setLlmUsed(llmUsed);
        savedDigest.setEditorialStrategy(strategy);
        savedDigest = dailyDigestRepository.save(savedDigest);
        return toResponse(
                savedDigest,
                digestItems,
                editorialMap,
                updatedEditorialResult
        );
    }

    public List<DigestResponse> listDigests(Long userId) {
        return dailyDigestRepository.findByUserIdOrderByDigestDateDesc(userId).stream()
                .map(digest -> toResponse(
                        digest,
                        dailyDigestItemRepository.findByDigestIdOrderBySectionAscItemOrderAsc(digest.getId()),
                        Map.of(),
                        EditorialResult.none()))
                .toList();
    }

    public DigestResponse getDigest(Long digestId) {
        DailyDigest digest = dailyDigestRepository.findById(digestId)
                .orElseThrow(() -> new IllegalArgumentException("Digest not found: " + digestId));
        return toResponse(
                digest,
                dailyDigestItemRepository.findByDigestIdOrderBySectionAscItemOrderAsc(digestId),
                Map.of(),
                new EditorialResult(
                        Map.of(),
                        digest.getLlmUsed() != null ? digest.getLlmUsed() : false,
                        digest.getEditorialStrategy() != null ? digest.getEditorialStrategy() : "heuristic",
                        null
                ));
    }

    @Transactional
    public DigestResponse finalizeDigest(Long digestId, boolean requireLlm) {
        DailyDigest digest = dailyDigestRepository.findById(digestId)
                .orElseThrow(() -> new IllegalArgumentException("Digest not found: " + digestId));
        List<DailyDigestItem> digestItems = dailyDigestItemRepository.findByDigestIdOrderBySectionAscItemOrderAsc(digestId);
        if (digestItems.isEmpty()) {
            return toResponse(digest, digestItems, Map.of(), EditorialResult.none());
        }
        UserPreference preference = preferenceService.getPreference(digest.getUser().getId());
        List<ScoredItem> selected = digestItems.stream()
                .map(item -> new ScoredItem(item.getNormalizedItem(), item.getFinalScore()))
                .toList();

        FinalizeResult finalizeResult = finalizeSelectedItems(preference, selected);
        boolean mustUseLlm = requireLlm || forceLlmFinalize;
        if (mustUseLlm && !finalizeResult.llmUsed()) {
            throw new IllegalStateException("LLM finalization is required but unavailable: " + finalizeResult.fallbackReason());
        }

        // 当finalizeResult.llmUsed()为true时，使用"llm"策略，而不是"llm_finalize"
        boolean llmUsed = finalizeResult.llmUsed();
        String strategy = llmUsed ? "llm" : "heuristic";
        
        DigestResponse response = toResponse(
                digest,
                digestItems,
                Map.of(),
                new EditorialResult(
                        Map.of(),
                        llmUsed,
                        strategy,
                        finalizeResult.fallbackReason()
                )
        );
        digest.setHtmlContent(digestRenderService.renderHtml(digest, response));
        digest.setLlmUsed(llmUsed);
        digest.setEditorialStrategy(strategy);
        dailyDigestRepository.save(digest);
        return toResponse(
                digest,
                digestItems,
                Map.of(),
                new EditorialResult(
                        Map.of(),
                        llmUsed,
                        strategy,
                        finalizeResult.fallbackReason()
                )
        );
    }

    public boolean isDigestFullyLlmFinalized(Long digestId) {
        List<DailyDigestItem> items = dailyDigestItemRepository.findByDigestIdOrderBySectionAscItemOrderAsc(digestId);
        if (items.isEmpty()) {
            return false;
        }
        return items.stream().allMatch(item -> itemEnrichmentRepository
                .findByNormalizedItemId(item.getNormalizedItem().getId())
                .map(enrichment -> "llm_finalized".equalsIgnoreCase(enrichment.getEnrichmentStatus()))
                .orElse(false));
    }

    public List<CandidateScoreResponse> previewCandidates(Long userId) {
        UserPreference preference = preferenceService.getPreference(userId);
        return normalizedItemRepository.findRecentReadyItems(LocalDateTime.now().minusDays(7)).stream()
                .filter(item -> notBlocked(preference, item))
                .map(item -> new CandidateScoreResponse(
                        item.getId(),
                        item.getTitleClean(),
                        item.getRawItem().getSource().getName(),
                        item.getRawItem().getPublishedAt() == null ? null : item.getRawItem().getPublishedAt().toString(),
                        rankingService.score(userId, preference, item)))
                .sorted(Comparator.comparing(CandidateScoreResponse::finalScore).reversed())
                .limit(20)
                .toList();
    }

    public DigestDebugResponse debugDigest(Long userId) {
        UserPreference preference = preferenceService.getPreference(userId);
        Set<Long> recentSentItemIds = findRecentSentItemIds(userId, LocalDate.now());
        List<ScoredItem> scoredItems = normalizedItemRepository.findRecentReadyItems(LocalDateTime.now().minusDays(3)).stream()
                .filter(item -> notBlocked(preference, item))
                .filter(item -> !recentSentItemIds.contains(item.getId()))
                .map(item -> new ScoredItem(item, rankingService.score(userId, preference, item)))
                .sorted(Comparator.comparing(ScoredItem::score).reversed())
                .limit(Math.max(preference.getMaxItemsPerDigest(), maxCandidatePool))
                .toList();

        List<ScoredItem> selectedItems = selectDigestItems(scoredItems, preference);
        boolean publishable = publicationDecisionService.shouldPublish(selectedItems.stream().map(ScoredItem::score).toList());

        return new DigestDebugResponse(
                publishable,
                scoredItems.size(),
                selectedItems.size(),
                scoredItems.stream().limit(10).map(this::toCandidateScore).toList(),
                selectedItems.stream().map(this::toCandidateScore).toList()
        );
    }

    private DigestSection pickSection(ScoredItem item, int index, int total, UserPreference preference, 
                                     WorkerClient.EditorialDecisionItemPayload editorial, 
                                     Map<Long, ItemEnrichment> enrichmentMap) {
        if (editorial != null && editorial.section() != null) {
            return DigestSection.valueOf(editorial.section());
        }
        if (index == 0 || item.score().doubleValue() >= 90.0) {
            return DigestSection.MUST_READ;
        }
        ItemEnrichment enrichment = enrichmentMap.get(item.item().getId());
        if (enrichment != null && enrichment.getActionHint() == ActionHint.SAVE_FOR_LATER) {
            return DigestSection.WORTH_SAVING;
        }
        if (isExplorationCandidate(item.item(), preference) && index >= Math.max(1, total - 2)) {
            return DigestSection.SURPRISE;
        }
        if (index < Math.max(2, total / 2)) {
            return DigestSection.FOCUS_UPDATES;
        }
        return DigestSection.WORTH_SAVING;
    }

    private boolean notBlocked(UserPreference preference, NormalizedItem item) {
        String haystack = (item.getTitleClean() + " " + item.getSummaryClean()).toLowerCase();
        return preference.getBlockedTopics().stream().noneMatch(blocked -> haystack.contains(blocked.toLowerCase()));
    }

    private DailyDigestItem digestItemRepositorySave(DailyDigestItem digestItem) {
        return dailyDigestItemRepository.save(digestItem);
    }

    private Set<Long> findRecentSentItemIds(Long userId, LocalDate digestDate) {
        int days = Math.max(0, excludeRecentSentDays);
        if (days == 0) {
            return Set.of();
        }
        LocalDateTime since = digestDate.atStartOfDay().minusDays(days);
        return dailyDigestItemRepository.findRecentSentNormalizedItemIds(userId, since);
    }

    private DigestResponse toResponse(
            DailyDigest digest,
            List<DailyDigestItem> digestItems,
            Map<Long, WorkerClient.EditorialDecisionItemPayload> editorialMap,
            EditorialResult editorialResult
    ) {
        List<DigestItemResponse> items = digestItems.stream().map(item -> {
            ItemEnrichment enrichment = itemEnrichmentRepository.findByNormalizedItemId(item.getNormalizedItem().getId()).orElse(null);
            String enrichmentStatus = enrichment == null ? "missing" : enrichment.getEnrichmentStatus();
            return new DigestItemResponse(
                    item.getId(),
                    item.getSection().name().toLowerCase(),
                    item.getItemOrder(),
                    item.getFinalScore(),
                    enrichmentStatus,
                    item.getNormalizedItem().getTitleClean(),
                    enrichment == null ? item.getNormalizedItem().getSummaryClean() : enrichment.getShortSummary(),
                    buildPersonalReason(
                            digest.getUser().getId(),
                            item.getSection(),
                            item.getNormalizedItem(),
                            enrichment,
                            editorialMap.get(item.getNormalizedItem().getId()),
                            editorialResult.llmUsed()
                    ),
                    item.getNormalizedItem().getRawItem().getSource().getName(),
                    item.getNormalizedItem().getRawItem().getUrl(),
                    feedbackUrl(digest.getUser().getId(), digest.getId(), item.getId(), "USEFUL"),
                    feedbackUrl(digest.getUser().getId(), digest.getId(), item.getId(), "NORMAL"),
                    feedbackUrl(digest.getUser().getId(), digest.getId(), item.getId(), "FOLLOW")
            );
        }).toList();
        return new DigestResponse(
                digest.getId(),
                digest.getDigestDate(),
                digest.getStatus().name().toLowerCase(),
                digest.getSubject(),
                digest.getTotalItems(),
                digest.getSentAt(),
                items,
                editorialResult.llmUsed(),
                editorialResult.strategy()
        );
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private record ScoredItem(NormalizedItem item, BigDecimal score) {
    }

    private List<ScoredItem> selectDigestItems(List<ScoredItem> scoredItems, UserPreference preference) {
        int maxItems = preference.getMaxItemsPerDigest();
        int minItems = Math.min(3, maxItems);
        int surpriseSlots = preference.getExplorationRatio().doubleValue() >= 0.10 ? 1 : 0;
        List<ScoredItem> eligibleItems = scoredItems.stream()
                .filter(item -> item.score().doubleValue() >= minItemScore)
                .toList();

        List<ScoredItem> core = eligibleItems.stream()
                .filter(item -> !isExplorationCandidate(item.item(), preference))
                .limit(Math.max(minItems - surpriseSlots, 1))
                .toList();

        List<ScoredItem> exploration = eligibleItems.stream()
                .filter(item -> isExplorationCandidate(item.item(), preference))
                .limit(surpriseSlots)
                .toList();

        List<ScoredItem> selected = new ArrayList<>(core);
        selected.addAll(exploration);
        if (selected.size() < minItems) {
            eligibleItems.stream()
                    .filter(item -> selected.stream().noneMatch(existing -> existing.item().getId().equals(item.item().getId())))
                    .limit(minItems - selected.size())
                    .forEach(selected::add);
        }
        if (selected.size() < maxItems) {
            eligibleItems.stream()
                    .filter(item -> selected.stream().noneMatch(existing -> existing.item().getId().equals(item.item().getId())))
                    .limit(maxItems - selected.size())
                    .forEach(selected::add);
        }
        return selected.stream()
                .sorted(Comparator.comparing(ScoredItem::score).reversed())
                .limit(maxItems)
                .toList();
    }

    private boolean isExplorationCandidate(NormalizedItem item, UserPreference preference) {
        String combined = normalizeCombined(item);
        boolean matchesPreferred = preference.getPreferredTopics().stream()
                .map(topic -> topic.toLowerCase(Locale.ROOT))
                .anyMatch(topic -> containsTopic(combined, topic));
        return !matchesPreferred;
    }

    private String buildPersonalReason(
            Long userId,
            DigestSection section,
            NormalizedItem item,
            ItemEnrichment enrichment,
            WorkerClient.EditorialDecisionItemPayload editorial,
            boolean llmUsed
    ) {
        if (editorial != null && editorial.reason() != null && !editorial.reason().isBlank()) {
            return editorial.reason();
        }
        UserPreference preference = preferenceService.getPreference(userId);
        String combined = normalizeCombined(item);
        String topic = preference.getPreferredTopics().stream()
                .filter(candidate -> containsTopic(combined, candidate.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
        if (topic != null) {
            return switch (section) {
                case MUST_READ -> "你最近在关注" + topic + "，这条内容和这个方向的最新进展直接相关，值得优先看。";
                case FOCUS_UPDATES -> "这条内容延续了你在" + topic + "上的关注，可以帮助你保持对这个方向的跟进。";
                case WORTH_SAVING -> "这条内容和" + topic + "相关，不一定要立刻处理，但值得先留存。";
                case SURPRISE -> "这条内容和" + topic + "相邻，适合作为今天的一点延伸阅读。";
            };
        }

        String goal = preference.getGoals();
        if (goal != null && !goal.isBlank()) {
            String goalHint = extractGoalHint(goal);
            return switch (section) {
                case MUST_READ -> "这条内容和你当前在推进的“" + goalHint + "”直接有关，适合放在今天最前面。";
                case FOCUS_UPDATES -> "如果你最近还在围绕“" + goalHint + "”投入精力，这条内容值得继续跟进。";
                case WORTH_SAVING -> "这条内容和你的当前目标有间接关系，更适合先收藏，后面再看。";
                case SURPRISE -> "这条内容不是你的核心关注，但和“" + goalHint + "”有邻近关联，适合作为扩展视角。";
            };
        }

        if (enrichment != null && enrichment.getRelevanceReason() != null && !enrichment.getRelevanceReason().isBlank()) {
            return enrichment.getRelevanceReason();
        }
        return section == DigestSection.SURPRISE
                ? "这条内容不是核心主线，但值得作为今天的一点意外收获。"
                : "这条内容与当前关注方向接近，适合作为今天的候选。";
    }

    private String feedbackUrl(Long userId, Long digestId, Long digestItemId, String type) {
        return UriComponentsBuilder.fromHttpUrl(publicBaseUrl + "/api/feedback/click")
                .queryParam("userId", userId)
                .queryParam("digestId", digestId)
                .queryParam("digestItemId", digestItemId)
                .queryParam("type", type)
                .toUriString();
    }

    private String buildSubject(LocalDate digestDate, int itemCount) {
        return "AI 送报员 | " + digestDate + " 今日情报简报 · " + itemCount + " 条重点";
    }

    private CandidateScoreResponse toCandidateScore(ScoredItem item) {
        return new CandidateScoreResponse(
                item.item().getId(),
                item.item().getTitleClean(),
                item.item().getRawItem().getSource().getName(),
                item.item().getRawItem().getPublishedAt() == null ? null : item.item().getRawItem().getPublishedAt().toString(),
                item.score()
        );
    }

    private EditorialResult fetchEditorialDecisions(UserPreference preference, List<ScoredItem> scoredItems) {
        List<WorkerClient.EditorialCandidatePayload> candidates = scoredItems.stream()
                .limit(Math.min(scoredItems.size(), editorialCandidateLimit))
                .map(item -> new WorkerClient.EditorialCandidatePayload(
                        item.item().getId(),
                        trimForEditorial(item.item().getTitleClean(), 140),
                        trimForEditorial(item.item().getSummaryClean(), 260),
                        item.item().getRawItem().getSource().getName(),
                        item.item().getTags(),
                        item.score().doubleValue()))
                .toList();
        if (candidates.isEmpty()) {
            return EditorialResult.none();
        }

        WorkerClient.EditorialDecisionResponse response;
        try {
            response = workerClient.decide(
                    new WorkerClient.EditorialDecisionRequest(
                            new WorkerClient.EditorialPreferencePayload(
                                    preference.getGoals(),
                                    preference.getPreferredTopics(),
                                    preference.getBlockedTopics(),
                                    preference.getDeliveryMode().name(),
                                    preference.getExplorationRatio().doubleValue(),
                                    preference.getMaxItemsPerDigest()
                            ),
                            candidates
                    )
            );
        } catch (Exception ex) {
            return new EditorialResult(Map.of(), false, "heuristic", "worker_call_failed");
        }
        if (response == null || response.decisions() == null) {
            return new EditorialResult(Map.of(), false, "heuristic", "empty_response");
        }
        Map<Long, WorkerClient.EditorialDecisionItemPayload> result = new HashMap<>();
        for (WorkerClient.EditorialDecisionItemPayload decision : response.decisions()) {
            result.put(decision.normalizedItemId(), decision);
        }
        boolean llmUsed = Boolean.TRUE.equals(response.llmUsed());
        String strategy = response.strategy() == null || response.strategy().isBlank() ? "unknown" : response.strategy();
        return new EditorialResult(result, llmUsed, strategy, response.fallbackReason());
    }

    private ScoredItem applyEditorialDecision(ScoredItem item, WorkerClient.EditorialDecisionItemPayload editorial) {
        if (editorial == null || editorial.scoreAdjustment() == null) {
            return item;
        }
        return new ScoredItem(
                item.item(),
                item.score().add(BigDecimal.valueOf(editorial.scoreAdjustment())).setScale(2, java.math.RoundingMode.HALF_UP)
        );
    }

    private String normalizeCombined(NormalizedItem item) {
        return (item.getTitleClean() + " " + item.getSummaryClean()).toLowerCase(Locale.ROOT);
    }

    private boolean containsTopic(String combined, String topic) {
        if (combined.contains(topic)) {
            return true;
        }
        int matched = 0;
        int total = 0;
        for (String token : topic.split("[,，。/\\-\\s]+")) {
            String trimmed = token.trim();
            if (trimmed.length() >= 2) {
                total++;
                if (combined.contains(trimmed)) {
                    matched++;
                }
            }
        }
        return total > 0 && matched == total;
    }

    private String extractGoalHint(String goal) {
        String normalized = goal.replace("最近在", "").replace("最近", "").trim();
        return normalized.length() > 18 ? normalized.substring(0, 18) + "..." : normalized;
    }

    private String trimForEditorial(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }

    private record EditorialResult(
            Map<Long, WorkerClient.EditorialDecisionItemPayload> decisions,
            boolean llmUsed,
            String strategy,
            String fallbackReason
    ) {
        private static EditorialResult none() {
            return new EditorialResult(Map.of(), false, "heuristic", "no_candidates");
        }
    }

    private FinalizeResult finalizeSelectedItems(UserPreference preference, List<ScoredItem> selectedItems) {
        if (selectedItems.isEmpty()) {
            return new FinalizeResult(false, "no_selected_items");
        }

        // 获取用户画像信息
        Map<String, Object> userProfile = new HashMap<>();
        if (!selectedItems.isEmpty()) {
            // 从偏好设置中获取用户 ID
            Long userId = preference.getUser().getId();
            com.aipostman.domain.UserProfile profile = userProfileService.getProfile(userId);
            if (profile != null) {
                if (profile.getInterests() != null) {
                    userProfile.put("interests", profile.getInterests());
                }
                if (profile.getOccupation() != null) {
                    userProfile.put("occupation", profile.getOccupation());
                }
                if (profile.getPreferredTopics() != null) {
                    userProfile.put("preferredTopics", profile.getPreferredTopics());
                }
                if (profile.getReadingHabits() != null) {
                    userProfile.put("readingHabits", profile.getReadingHabits());
                }
                if (profile.getContentPreferences() != null) {
                    userProfile.put("contentPreferences", profile.getContentPreferences());
                }
                if (profile.getTechnicalSkills() != null) {
                    userProfile.put("technicalSkills", profile.getTechnicalSkills());
                }
                if (profile.getExperienceLevel() != null) {
                    userProfile.put("experienceLevel", profile.getExperienceLevel());
                }
                if (profile.getLearningGoals() != null) {
                    userProfile.put("learningGoals", profile.getLearningGoals());
                }
                if (profile.getIndustryFocus() != null) {
                    userProfile.put("industryFocus", profile.getIndustryFocus());
                }
                if (profile.getCodingHabits() != null) {
                    userProfile.put("codingHabits", profile.getCodingHabits());
                }
                if (profile.getToolPreferences() != null) {
                    userProfile.put("toolPreferences", profile.getToolPreferences());
                }
                if (profile.getContentFormats() != null) {
                    userProfile.put("contentFormats", profile.getContentFormats());
                }
                if (profile.getTimeAvailability() != null) {
                    userProfile.put("timeAvailability", profile.getTimeAvailability());
                }
            }
        }

        // 批量获取所有需要的 ItemEnrichment，减少 N+1 查询
        List<Long> normalizedItemIds = selectedItems.stream()
                .map(ScoredItem::item)
                .map(NormalizedItem::getId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, ItemEnrichment> enrichmentMap = itemEnrichmentRepository.findByNormalizedItemIdIn(normalizedItemIds)
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getNormalizedItem().getId(),
                        item -> item
                ));
        
        // 为没有找到的项添加 null 值
        for (Long id : normalizedItemIds) {
            enrichmentMap.putIfAbsent(id, null);
        }

        List<WorkerClient.FinalizeDigestItemPayload> items = selectedItems.stream()
                .map(item -> {
                    NormalizedItem normalized = item.item();
                    return new WorkerClient.FinalizeDigestItemPayload(
                            normalized.getId(),
                            trimForEditorial(normalized.getTitleClean(), 160),
                            trimForEditorial(normalized.getSummaryClean(), 220),
                            trimForEditorial(normalized.getContentClean(), 320),
                            normalized.getRawItem().getSource().getName(),
                            pickSection(
                                    item,
                                    selectedItems.indexOf(item),
                                    selectedItems.size(),
                                    preference,
                                    null,
                                    enrichmentMap
                            ).name()
                    );
                })
                .toList();
        WorkerClient.FinalizeDigestResponse response;
        try {
            response = workerClient.finalizeDigest(
                    new WorkerClient.FinalizeDigestRequest(
                            new WorkerClient.EditorialPreferencePayload(
                                    preference.getGoals(),
                                    preference.getPreferredTopics(),
                                    preference.getBlockedTopics(),
                                    preference.getDeliveryMode().name(),
                                    preference.getExplorationRatio().doubleValue(),
                                    preference.getMaxItemsPerDigest()
                            ),
                            items,
                            userProfile
                    )
            );
        } catch (Exception ex) {
            String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new FinalizeResult(false, "worker_call_failed: " + reason);
        }
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return new FinalizeResult(false, response == null ? "empty_response" : response.fallbackReason());
        }

        for (WorkerClient.FinalizedDigestItemPayload finalized : response.items()) {
            NormalizedItem normalized = normalizedItemRepository.findById(finalized.normalizedItemId()).orElse(null);
            if (normalized == null) {
                continue;
            }
            ItemEnrichment enrichment = itemEnrichmentRepository.findByNormalizedItemId(normalized.getId()).orElseGet(ItemEnrichment::new);
            enrichment.setNormalizedItem(normalized);
            enrichment.setShortSummary(finalized.shortSummary());
            enrichment.setRelevanceReason(finalized.relevanceReason());
            if (finalized.actionHint() != null) {
                try {
                    enrichment.setActionHint(ActionHint.valueOf(finalized.actionHint().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    // keep existing hint
                }
            }
            enrichment.setEnrichmentStatus(Boolean.TRUE.equals(response.llmUsed()) ? "llm_finalized" : "heuristic_finalized");
            itemEnrichmentRepository.save(enrichment);
        }
        return new FinalizeResult(Boolean.TRUE.equals(response.llmUsed()), response.fallbackReason());
    }

    private record FinalizeResult(boolean llmUsed, String fallbackReason) {
    }

    private boolean shouldReuseDigest(DailyDigest digest) {
        if (digest.getStatus() == DigestStatus.SENT) {
            return true;
        }
        return digest.getStatus() == DigestStatus.DRAFT && digest.getTotalItems() > 0;
    }
}
