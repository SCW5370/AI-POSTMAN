package com.aipostman.service;

import com.aipostman.domain.DigestBuildTask;
import com.aipostman.dto.response.BuildTaskResponse;
import com.aipostman.dto.response.DigestResponse;
import jakarta.annotation.PostConstruct;
import com.aipostman.repository.DigestBuildTaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigestBuildTaskService {

    private final DigestService digestService;
    private final SourceDiscoveryService sourceDiscoveryService;
    private final DigestBuildTaskRepository digestBuildTaskRepository;
    private final Executor digestTaskExecutor;

    public DigestBuildTaskService(
            DigestService digestService,
            SourceDiscoveryService sourceDiscoveryService,
            DigestBuildTaskRepository digestBuildTaskRepository,
            @Qualifier("digestTaskExecutor") Executor digestTaskExecutor
    ) {
        this.digestService = digestService;
        this.sourceDiscoveryService = sourceDiscoveryService;
        this.digestBuildTaskRepository = digestBuildTaskRepository;
        this.digestTaskExecutor = digestTaskExecutor;
    }

    @PostConstruct
    public void onStartup() {
        recoverInterruptedTasks();
    }

    @Transactional
    public BuildTaskResponse submit(Long userId, LocalDate digestDate) {
        return submit(userId, digestDate, false);
    }

    @Transactional
    public BuildTaskResponse submit(Long userId, LocalDate digestDate, boolean forceLlm) {
        DigestBuildTask task = new DigestBuildTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(userId);
        task.setDigestDate(digestDate);
        task.setForceLlm(forceLlm);
        task.setStatus("pending");
        task.setMessage(forceLlm ? "queued (force llm)" : "queued");
        task = digestBuildTaskRepository.save(task);
        String taskId = task.getTaskId();
        enqueueAfterCommit(() -> runTask(taskId));
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public BuildTaskResponse get(String taskId) {
        DigestBuildTask task = digestBuildTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Build task not found: " + taskId));
        return toResponse(task);
    }

    @Transactional
    public int recoverInterruptedTasks() {
        List<DigestBuildTask> interrupted = digestBuildTaskRepository.findByStatusIn(List.of("pending", "running"));
        for (DigestBuildTask task : interrupted) {
            task.setStatus("failed");
            task.setMessage("interrupted by service restart");
        }
        digestBuildTaskRepository.saveAll(interrupted);
        return interrupted.size();
    }

    @Transactional
    protected void runTask(String taskId) {
        DigestBuildTask task = digestBuildTaskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            return;
        }
        task.setStatus("running");
        task.setMessage(task.isForceLlm() ? "building digest (force llm)" : "building digest");
        digestBuildTaskRepository.save(task);

        SourceDiscoveryService.AutoDiscoveryResult discovery = new SourceDiscoveryService.AutoDiscoveryResult(
                "skipped", List.of(), 0, 0, 0, List.of(), List.of()
        );
        String discoveryWarning = null;

        try {
            task.setMessage("discovering sources from profile");
            digestBuildTaskRepository.save(task);
            try {
                discovery = sourceDiscoveryService.autoDiscoverAndApproveFromUserProfile(task.getUserId());
                if (!discovery.approvedSourceIds().isEmpty()) {
                    task.setMessage("fetching newly approved sources");
                    digestBuildTaskRepository.save(task);
                    digestService.fetchAndStore(discovery.approvedSourceIds());
                }
            } catch (Exception discoveryEx) {
                String raw = discoveryEx.getMessage();
                discoveryWarning = (raw == null || raw.isBlank())
                        ? "source discovery skipped"
                        : "source discovery skipped: " + raw;
            }

            task.setMessage(task.isForceLlm() ? "building digest (force llm)" : "building digest");
            digestBuildTaskRepository.save(task);
            DigestResponse digest = digestService.buildDigest(task.getUserId(), task.getDigestDate(), task.isForceLlm());
            task.setStatus("success");
            if (discoveryWarning != null) {
                task.setMessage("build completed (" + discoveryWarning + ")");
            } else if (discovery.approvedCandidates() > 0) {
                task.setMessage("build completed (auto discovered " + discovery.approvedCandidates() + " new sources)");
            } else {
                task.setMessage("build completed");
            }
            task.setDigestId(digest.id());
        } catch (Exception ex) {
            task.setStatus("failed");
            task.setMessage(ex.getMessage() == null ? "build failed" : ex.getMessage());
        }
        digestBuildTaskRepository.save(task);
    }

    private BuildTaskResponse toResponse(DigestBuildTask task) {
        DigestResponse digest = null;
        if (task.getDigestId() != null) {
            try {
                digest = digestService.getDigest(task.getDigestId());
            } catch (Exception ignored) {
                // keep task response available even if digest fetch fails
            }
        }
        return new BuildTaskResponse(task.getTaskId(), task.getStatus(), task.getMessage(), digest);
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
