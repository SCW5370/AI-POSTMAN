package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.dto.request.AdminFetchRequest;
import com.aipostman.dto.request.BuildDigestRequest;
import com.aipostman.dto.request.GenerateAndSendRequest;
import com.aipostman.dto.response.BuildTaskResponse;
import com.aipostman.dto.response.CandidateScoreResponse;
import com.aipostman.dto.response.DigestDebugResponse;
import com.aipostman.dto.response.DigestResponse;
import com.aipostman.dto.response.FetchTaskResponse;
import com.aipostman.service.DeliveryService;
import com.aipostman.service.DigestBuildTaskService;
import com.aipostman.service.DigestFinalizationService;
import com.aipostman.service.DigestPreparationService;
import com.aipostman.service.DigestService;
import com.aipostman.service.FetchTaskService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DigestService digestService;
    private final DigestBuildTaskService digestBuildTaskService;
    private final DigestPreparationService digestPreparationService;
    private final DigestFinalizationService digestFinalizationService;
    private final FetchTaskService fetchTaskService;
    private final DeliveryService deliveryService;

    public AdminController(
            DigestService digestService,
            DigestBuildTaskService digestBuildTaskService,
            DigestPreparationService digestPreparationService,
            DigestFinalizationService digestFinalizationService,
            FetchTaskService fetchTaskService,
            DeliveryService deliveryService
    ) {
        this.digestService = digestService;
        this.digestBuildTaskService = digestBuildTaskService;
        this.digestPreparationService = digestPreparationService;
        this.digestFinalizationService = digestFinalizationService;
        this.fetchTaskService = fetchTaskService;
        this.deliveryService = deliveryService;
    }

    @PostMapping("/fetch")
    public ApiResponse<String> fetch(@RequestBody AdminFetchRequest request) {
        int count = digestService.fetchAndStore(request.sourceIds());
        return ApiResponse.ok("saved " + count + " new items");
    }

    @PostMapping("/fetch-async")
    public ApiResponse<FetchTaskResponse> fetchAsync(@RequestBody AdminFetchRequest request) {
        return ApiResponse.ok(fetchTaskService.submit(request.sourceIds()));
    }

    @GetMapping("/fetch-async/{taskId}")
    public ApiResponse<FetchTaskResponse> fetchAsyncStatus(@PathVariable String taskId) {
        return ApiResponse.ok(fetchTaskService.get(taskId));
    }

    @PostMapping("/digests/build")
    public ApiResponse<DigestResponse> build(@RequestBody BuildDigestRequest request) {
        return ApiResponse.ok(
                digestService.buildDigest(
                        request.userId(),
                        request.digestDate(),
                        Boolean.TRUE.equals(request.forceLlm())
                )
        );
    }

    @PostMapping("/digests/build-async")
    public ApiResponse<BuildTaskResponse> buildAsync(@RequestBody BuildDigestRequest request) {
        return ApiResponse.ok(
                digestBuildTaskService.submit(
                        request.userId(),
                        request.digestDate(),
                        Boolean.TRUE.equals(request.forceLlm())
                )
        );
    }

    @GetMapping("/digests/build-async/{taskId}")
    public ApiResponse<BuildTaskResponse> buildAsyncStatus(@PathVariable String taskId) {
        return ApiResponse.ok(digestBuildTaskService.get(taskId));
    }

    @PostMapping("/digests/send/{digestId}")
    public ApiResponse<DigestResponse> send(@PathVariable Long digestId) {
        return ApiResponse.ok(deliveryService.send(digestId));
    }

    @GetMapping("/digests/preview/{userId}")
    public ApiResponse<List<CandidateScoreResponse>> preview(@PathVariable Long userId) {
        return ApiResponse.ok(digestService.previewCandidates(userId));
    }

    @GetMapping("/digests/debug/{userId}")
    public ApiResponse<DigestDebugResponse> debug(@PathVariable Long userId) {
        return ApiResponse.ok(digestService.debugDigest(userId));
    }

    @PostMapping("/digests/prebuild-now")
    public ApiResponse<String> prebuildNow() {
        int built = digestPreparationService.prebuildDueDigests();
        return ApiResponse.ok("prebuilt " + built + " digest(s)");
    }

    @PostMapping("/digests/finalize/{digestId}")
    public ApiResponse<DigestResponse> finalizeDigest(@PathVariable Long digestId) {
        return ApiResponse.ok(digestService.finalizeDigest(digestId, false));
    }

    @PostMapping("/digests/finalize-pending")
    public ApiResponse<String> finalizePending() {
        int finalized = digestFinalizationService.finalizePendingDigests();
        return ApiResponse.ok("finalized " + finalized + " digest(s)");
    }

    @PostMapping("/digests/generate-and-send")
    public ApiResponse<String> generateAndSend(@RequestBody GenerateAndSendRequest request) {
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            com.aipostman.dto.response.DigestResponse digestResponse = digestService.buildDigest(request.userId(), today, true);
            
            deliveryService.send(digestResponse.id(), true);
            
            return ApiResponse.ok("邮件已成功生成并发送");
        } catch (Exception e) {
            return ApiResponse.fail("邮件生成失败: " + e.getMessage());
        }
    }
}
