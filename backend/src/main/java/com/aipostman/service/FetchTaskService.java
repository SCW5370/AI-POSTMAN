package com.aipostman.service;

import com.aipostman.domain.FetchTask;
import com.aipostman.dto.response.FetchTaskResponse;
import com.aipostman.repository.FetchTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class FetchTaskService {

    private final DigestService digestService;
    private final FetchTaskRepository fetchTaskRepository;
    private final Executor digestTaskExecutor;
    private final ObjectMapper objectMapper;

    @Value("${app.worker.fetch-max-retries:2}")
    private int fetchMaxRetries;

    @Value("${app.worker.fetch-retry-backoff-ms:1200}")
    private long fetchRetryBackoffMs;

    @Value("${app.worker.fetch-batch-size:4}")
    private int fetchBatchSize;

    public FetchTaskService(
            DigestService digestService,
            FetchTaskRepository fetchTaskRepository,
            @Qualifier("digestTaskExecutor") Executor digestTaskExecutor,
            ObjectMapper objectMapper
    ) {
        this.digestService = digestService;
        this.fetchTaskRepository = fetchTaskRepository;
        this.digestTaskExecutor = digestTaskExecutor;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void onStartup() {
        recoverInterruptedTasks();
    }

    @Transactional
    public FetchTaskResponse submit(List<Long> sourceIds) {
        FetchTask task = new FetchTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setSourceIdsJson(toJson(sourceIds));
        task.setStatus("pending");
        task.setMessage("queued");
        task = fetchTaskRepository.save(task);
        String taskId = task.getTaskId();
        enqueueAfterCommit(() -> runTask(taskId));
        return toResponse(task);
    }

    @Transactional
    public FetchTaskResponse submitIfIdle(List<Long> sourceIds) {
        boolean hasActiveTask = fetchTaskRepository.existsByStatusIn(List.of("pending", "running"));
        if (hasActiveTask) {
            FetchTaskResponse response = new FetchTaskResponse(
                    null,
                    "skipped",
                    "fetch task already pending/running",
                    0
            );
            return response;
        }
        return submit(sourceIds);
    }

    @Transactional(readOnly = true)
    public FetchTaskResponse get(String taskId) {
        FetchTask task = fetchTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Fetch task not found: " + taskId));
        return toResponse(task);
    }

    @Transactional
    public int recoverInterruptedTasks() {
        List<FetchTask> interrupted = fetchTaskRepository.findByStatusIn(List.of("pending", "running"));
        for (FetchTask task : interrupted) {
            task.setStatus("failed");
            task.setMessage("interrupted by service restart");
        }
        fetchTaskRepository.saveAll(interrupted);
        return interrupted.size();
    }

    @Transactional
    protected void runTask(String taskId) {
        FetchTask task = fetchTaskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            return;
        }
        task.setStatus("running");
        task.setMessage("fetching sources");
        fetchTaskRepository.save(task);

        List<Long> sourceIds = fromJson(task.getSourceIdsJson());
        int attempts = 0;
        int maxAttempts = Math.max(1, fetchMaxRetries + 1);
        while (attempts < maxAttempts) {
            attempts++;
            try {
                int saved = digestService.fetchAndStoreBatched(sourceIds, fetchBatchSize);
                task.setStatus("success");
                task.setMessage("fetch completed in attempt " + attempts);
                task.setSavedCount(saved);
                fetchTaskRepository.save(task);
                return;
            } catch (Exception ex) {
                boolean retryable = isRetryable(ex);
                if (!retryable || attempts >= maxAttempts) {
                    task.setStatus("failed");
                    String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    task.setMessage("fetch failed after " + attempts + " attempt(s): " + reason);
                    fetchTaskRepository.save(task);
                    return;
                }
                long waitMs = Math.max(200L, fetchRetryBackoffMs) * (1L << (attempts - 1));
                task.setStatus("running");
                task.setMessage("attempt " + attempts + " timeout, retry in " + waitMs + "ms");
                fetchTaskRepository.save(task);
                sleepQuietly(waitMs);
            }
        }
    }

    private FetchTaskResponse toResponse(FetchTask task) {
        return new FetchTaskResponse(task.getTaskId(), task.getStatus(), task.getMessage(), task.getSavedCount());
    }

    private String toJson(List<Long> sourceIds) {
        try {
            return objectMapper.writeValueAsString(sourceIds == null ? List.of() : sourceIds);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<Long> fromJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    raw,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private boolean isRetryable(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof TimeoutException) {
                return true;
            }
            String message = cursor.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("timeout")
                        || lower.contains("timed out")
                        || lower.contains("connection reset")
                        || lower.contains("connection refused")
                        || lower.contains("worker_call_failed")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private void sleepQuietly(long waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void enqueueAfterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            digestTaskExecutor.execute(runnable);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                digestTaskExecutor.execute(runnable);
            }
        });
    }
}
