package com.aipostman.service;

import com.aipostman.domain.DailyDigest;
import com.aipostman.dto.response.DigestResponse;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class DigestRenderService {

    private final TemplateEngine templateEngine;

    public DigestRenderService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderHtml(DailyDigest digest, DigestResponse response) {
        Context context = new Context();
        context.setVariable("userName", digest.getUser().getDisplayName() == null ? digest.getUser().getEmail() : digest.getUser().getDisplayName());
        context.setVariable("digestDate", digest.getDigestDate());
        context.setVariable("items", response.items());
        context.setVariable("totalItems", response.totalItems());
        return templateEngine.process("digest-template.html", context);
    }
}
