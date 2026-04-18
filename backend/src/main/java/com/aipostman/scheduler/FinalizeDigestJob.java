package com.aipostman.scheduler;

import com.aipostman.service.DigestFinalizationService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FinalizeDigestJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(FinalizeDigestJob.class);

    @Autowired
    private DigestFinalizationService digestFinalizationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int finalized = digestFinalizationService.finalizePendingDigests();
        if (finalized > 0) {
            log.info("finalize job completed finalized={}", finalized);
        }
    }
}

