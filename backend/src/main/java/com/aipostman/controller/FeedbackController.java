package com.aipostman.controller;

import com.aipostman.common.ApiResponse;
import com.aipostman.common.enums.FeedbackType;
import com.aipostman.dto.request.FeedbackRequest;
import com.aipostman.dto.response.FeedbackSummaryResponse;
import com.aipostman.service.FeedbackService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<String> record(@RequestBody FeedbackRequest request) {
        feedbackService.record(request);
        return ApiResponse.ok("recorded");
    }

    @GetMapping(value = "/click", produces = MediaType.TEXT_HTML_VALUE)
    public String click(
            @RequestParam Long userId,
            @RequestParam Long digestId,
            @RequestParam Long digestItemId,
            @RequestParam FeedbackType type
    ) {
        feedbackService.record(new FeedbackRequest(userId, digestId, digestItemId, type));
        return "<html><body><h3>已记录：" + type.name().toLowerCase() + "</h3><p>你之后会看到更相关的内容。</p></body></html>";
    }

    @GetMapping("/summary/{userId}")
    public ApiResponse<FeedbackSummaryResponse> summary(@PathVariable Long userId) {
        return ApiResponse.ok(feedbackService.summary(userId));
    }
}
