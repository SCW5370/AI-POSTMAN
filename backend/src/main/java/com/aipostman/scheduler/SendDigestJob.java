package com.aipostman.scheduler;

import com.aipostman.common.enums.DigestStatus;
import com.aipostman.domain.DailyDigest;
import com.aipostman.domain.UserPreference;
import com.aipostman.repository.DailyDigestRepository;
import com.aipostman.repository.UserPreferenceRepository;
import com.aipostman.service.DeliveryScheduleService;
import com.aipostman.service.DeliveryService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SendDigestJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(SendDigestJob.class);

    @Autowired
    private DailyDigestRepository dailyDigestRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private DeliveryScheduleService deliveryScheduleService;

    @Autowired
    private DeliveryService deliveryService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        java.time.ZonedDateTime nowUtc = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        for (DailyDigest digest : dailyDigestRepository.findByStatus(DigestStatus.DRAFT)) {
            if (digest.getTotalItems() <= 0) {
                continue;
            }
            if (!digest.getUser().isDeliveryEnabled()) {
                continue;
            }
            UserPreference preference = userPreferenceRepository.findByUserId(digest.getUser().getId()).orElse(null);
            if (preference == null) {
                continue;
            }
            if (!deliveryScheduleService.shouldSendNow(digest.getUser(), preference, digest.getDigestDate(), nowUtc)) {
                continue;
            }
            try {
                deliveryService.send(digest.getId());
                log.info("sent digest digestId={} userId={} digestDate={}",
                        digest.getId(), digest.getUser().getId(), digest.getDigestDate());
            } catch (Exception ex) {
                log.warn("send digest failed digestId={} userId={} reason={}",
                        digest.getId(), digest.getUser().getId(), ex.getMessage());
            }
        }
    }
}
