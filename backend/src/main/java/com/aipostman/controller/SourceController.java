package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.dto.request.CreateSourceRequest;
import com.aipostman.dto.request.ToggleSourceRequest;
import com.aipostman.dto.response.SourceResponse;
import com.aipostman.service.SourceService;
import com.aipostman.service.SourceDiscoveryService;
import com.aipostman.service.SourceCandidateService;
import com.aipostman.domain.SourceCandidate;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final SourceService sourceService;
    private final SourceDiscoveryService sourceDiscoveryService;
    private final SourceCandidateService sourceCandidateService;

    public SourceController(SourceService sourceService, SourceDiscoveryService sourceDiscoveryService, SourceCandidateService sourceCandidateService) {
        this.sourceService = sourceService;
        this.sourceDiscoveryService = sourceDiscoveryService;
        this.sourceCandidateService = sourceCandidateService;
    }

    @PostMapping
    public ApiResponse<SourceResponse> create(@Valid @RequestBody CreateSourceRequest request) {
        return ApiResponse.ok(sourceService.create(request));
    }

    @GetMapping
    public ApiResponse<List<SourceResponse>> list() {
        return ApiResponse.ok(sourceService.list());
    }

    @PatchMapping("/{id}/enable")
    public ApiResponse<SourceResponse> toggle(@PathVariable Long id, @RequestBody ToggleSourceRequest request) {
        return ApiResponse.ok(sourceService.toggle(id, request.enabled()));
    }

    @PostMapping("/discover")
    public ApiResponse<List<SourceCandidate>> discover(@RequestParam String userId, @RequestParam String topic) {
        List<SourceCandidate> candidates = sourceDiscoveryService.discoverSources(userId, topic).join();
        return ApiResponse.ok(candidates);
    }

    @PostMapping("/discover-agent")
    public ApiResponse<List<SourceCandidate>> discoverWithAgent(@RequestParam String userId, @RequestParam String topic) {
        List<SourceCandidate> candidates = sourceDiscoveryService.discoverSourcesWithAgent(userId, topic).join();
        return ApiResponse.ok(candidates);
    }

    @PostMapping("/discover-agent/bootstrap")
    public ApiResponse<SourceDiscoveryService.AutoDiscoveryResult> discoverAndAutoApprove(
            @RequestParam Long userId,
            @RequestParam(required = false) String strategy
    ) {
        return ApiResponse.ok(sourceDiscoveryService.autoDiscoverAndApproveFromUserProfile(userId, strategy));
    }

    @GetMapping("/candidates")
    public ApiResponse<List<SourceCandidate>> getCandidates(@RequestParam(required = false) String status) {
        List<SourceCandidate> candidates = status != null ? 
                sourceCandidateService.getCandidatesByStatus(status) : 
                sourceCandidateService.getAllCandidates();
        return ApiResponse.ok(candidates);
    }

    @PostMapping("/candidates/{id}/approve")
    public ApiResponse<SourceCandidate> approveCandidate(@PathVariable Long id) {
        SourceCandidate candidate = sourceCandidateService.approveCandidate(id);
        return ApiResponse.ok(candidate);
    }

    @PostMapping("/candidates/{id}/reject")
    public ApiResponse<SourceCandidate> rejectCandidate(@PathVariable Long id, @RequestParam String reason) {
        SourceCandidate candidate = sourceCandidateService.rejectCandidate(id, reason);
        return ApiResponse.ok(candidate);
    }

    @PostMapping("/candidates/{id}/auto-approve")
    public ApiResponse<SourceCandidate> autoApproveWithLLM(@PathVariable Long id) {
        SourceCandidate candidate = sourceCandidateService.autoApproveWithLLM(id);
        return ApiResponse.ok(candidate);
    }
}
