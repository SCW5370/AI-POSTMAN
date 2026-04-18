package com.aipostman.service;

import com.aipostman.common.enums.DigestStatus;
import com.aipostman.domain.DailyDigest;
import com.aipostman.domain.UserPreference;
import com.aipostman.repository.DailyDigestRepository;
import com.aipostman.repository.UserPreferenceRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DigestFinalizationService {

    private static final Logger log = LoggerFactory.getLogger(DigestFinalizationService.class);

    private final DailyDigestRepository dailyDigestRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final DeliveryScheduleService deliveryScheduleService;
    private final DigestService digestService;

    public DigestFinalizationService(
            DailyDigestRepository dailyDigestRepository,
            UserPreferenceRepository userPreferenceRepository,
            DeliveryScheduleService deliveryScheduleService,
            DigestService digestService
    ) {
        this.dailyDigestRepository = dailyDigestRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.deliveryScheduleService = deliveryScheduleService;
        this.digestService = digestService;
    }

    public int finalizePendingDigests() {
        return finalizePendingDigests(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public int finalizePendingDigests(ZonedDateTime nowUtc) {
        int finalized = 0;
        for (DailyDigest digest : dailyDigestRepository.findByStatus(DigestStatus.DRAFT)) {
            if (digest.getTotalItems() <= 0) {
                continue;
            }
            if (!digest.getUser().isDeliveryEnabled()) {
                continue;
            }
            if (digestService.isDigestFullyLlmFinalized(digest.getId())) {
                continue;
            }
            UserPreference preference = userPreferenceRepository.findByUserId(digest.getUser().getId()).orElse(null);
            if (preference == null) {
                continue;
            }
            if (!deliveryScheduleService.shouldFinalizeNow(digest.getUser(), preference, digest.getDigestDate(), nowUtc)) {
                continue;
            }
            try {
                digestService.finalizeDigest(digest.getId(), false);
                finalized++;
                log.info("finalized digest with llm digestId={} userId={} digestDate={}",
                        digest.getId(), digest.getUser().getId(), digest.getDigestDate());
            } catch (Exception ex) {
                log.warn("finalize digest failed digestId={} userId={} reason={}",
                        digest.getId(), digest.getUser().getId(), ex.getMessage());
            }
        }
        return finalized;
    }
}

