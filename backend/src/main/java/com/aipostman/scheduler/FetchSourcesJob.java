package com.aipostman.scheduler;

import com.aipostman.dto.response.FetchTaskResponse;
import com.aipostman.service.FetchTaskService;
import java.util.List;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class FetchSourcesJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(FetchSourcesJob.class);

    @Autowired
    private FetchTaskService fetchTaskService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            FetchTaskResponse task = fetchTaskService.submitIfIdle(List.of());
            if ("skipped".equalsIgnoreCase(task.status())) {
                log.info("fetch scheduler skipped: {}", task.message());
                return;
            }
            log.info("fetch scheduler enqueued taskId={}", task.taskId());
        } catch (Exception ex) {
            log.warn("fetch scheduler enqueue failed: {}", ex.getMessage());
        }
    }
}
