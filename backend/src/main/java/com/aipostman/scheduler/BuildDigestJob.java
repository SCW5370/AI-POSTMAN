package com.aipostman.scheduler;

import com.aipostman.service.DigestPreparationService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class BuildDigestJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(BuildDigestJob.class);

    @Autowired
    private DigestPreparationService digestPreparationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int built = digestPreparationService.prebuildDueDigests();
        if (built > 0) {
            log.info("prebuild job completed built={}", built);
        }
    }
}
