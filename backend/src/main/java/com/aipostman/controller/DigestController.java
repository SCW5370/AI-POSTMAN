package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.dto.response.DigestResponse;
import com.aipostman.service.DigestService;
import com.aipostman.repository.DailyDigestRepository;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/digests")
public class DigestController {

    private final DigestService digestService;
    private final DailyDigestRepository dailyDigestRepository;

    public DigestController(DigestService digestService, DailyDigestRepository dailyDigestRepository) {
        this.digestService = digestService;
        this.dailyDigestRepository = dailyDigestRepository;
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<DigestResponse>> list(@PathVariable Long userId) {
        return ApiResponse.ok(digestService.listDigests(userId));
    }

    @GetMapping("/detail/{digestId}")
    public ApiResponse<DigestResponse> detail(@PathVariable Long digestId) {
        return ApiResponse.ok(digestService.getDigest(digestId));
    }

    @GetMapping(value = "/detail/{digestId}/html", produces = MediaType.TEXT_HTML_VALUE)
    public String detailHtml(@PathVariable Long digestId) {
        return dailyDigestRepository.findById(digestId)
                .orElseThrow(() -> new IllegalArgumentException("Digest not found: " + digestId))
                .getHtmlContent();
    }
}
