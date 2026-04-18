package com.aipostman.service;

import com.aipostman.domain.User;
import com.aipostman.domain.UserPreference;
import com.aipostman.repository.DailyDigestRepository;
import com.aipostman.repository.UserPreferenceRepository;
import com.aipostman.repository.UserRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DigestPreparationService {

    private static final Logger log = LoggerFactory.getLogger(DigestPreparationService.class);

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final DailyDigestRepository dailyDigestRepository;
    private final DeliveryScheduleService deliveryScheduleService;
    private final DigestService digestService;
    private final SourceDiscoveryService sourceDiscoveryService;

    public DigestPreparationService(
            UserRepository userRepository,
            UserPreferenceRepository userPreferenceRepository,
            DailyDigestRepository dailyDigestRepository,
            DeliveryScheduleService deliveryScheduleService,
            DigestService digestService,
            SourceDiscoveryService sourceDiscoveryService
    ) {
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.dailyDigestRepository = dailyDigestRepository;
        this.deliveryScheduleService = deliveryScheduleService;
        this.digestService = digestService;
        this.sourceDiscoveryService = sourceDiscoveryService;
    }

    public int prebuildDueDigests() {
        return prebuildDueDigests(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public int prebuildDueDigests(ZonedDateTime nowUtc) {
        int built = 0;
        for (User user : userRepository.findAll()) {
            if (!user.isDeliveryEnabled()) {
                continue;
            }
            UserPreference preference = userPreferenceRepository.findByUserId(user.getId()).orElse(null);
            if (preference == null) {
                continue;
            }
            DeliveryScheduleService.BuildPlan plan = deliveryScheduleService.getBuildPlan(user, preference, nowUtc);
            if (!plan.shouldBuildNow()) {
                continue;
            }
            boolean hasReadyDigest = dailyDigestRepository
                    .findByUserIdAndDigestDateAndDigestType(user.getId(), plan.targetDigestDate(), "daily")
                    .map(existing -> existing.getStatus().name().equals("DRAFT") || existing.getStatus().name().equals("SENT"))
                    .orElse(false);
            if (hasReadyDigest) {
                continue;
            }
            try {
                SourceDiscoveryService.AutoDiscoveryResult discovery =
                        sourceDiscoveryService.autoDiscoverAndApproveFromUserProfile(user.getId());
                if (!discovery.approvedSourceIds().isEmpty()) {
                    digestService.fetchAndStore(discovery.approvedSourceIds());
                    log.info("prebuild auto-discovery approvedSources={} userId={} digestDate={}",
                            discovery.approvedSourceIds().size(), user.getId(), plan.targetDigestDate());
                }
                digestService.buildDigest(user.getId(), plan.targetDigestDate());
                built++;
                log.info("prebuilt digest userId={} digestDate={} hoursUntilDelivery={}",
                        user.getId(), plan.targetDigestDate(), plan.hoursUntilDelivery());
            } catch (Exception ex) {
                log.warn("prebuild digest failed userId={} digestDate={} reason={}",
                        user.getId(), plan.targetDigestDate(), ex.getMessage());
            }
        }
        return built;
    }
}
