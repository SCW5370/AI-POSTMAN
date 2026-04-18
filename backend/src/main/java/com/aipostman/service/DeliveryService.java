package com.aipostman.service;

import com.aipostman.client.SmtpEmailClient;
import com.aipostman.common.enums.DigestStatus;
import com.aipostman.domain.DailyDigest;
import com.aipostman.dto.response.DigestResponse;
import com.aipostman.repository.DailyDigestRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    private final DailyDigestRepository dailyDigestRepository;
    private final DigestService digestService;
    private final SmtpEmailClient smtpEmailClient;
    private final DigestRenderService digestRenderService;

    @Value("${app.digest.ensure-llm-before-send:true}")
    private boolean ensureLlmBeforeSend;

    public DeliveryService(
            DailyDigestRepository dailyDigestRepository,
            DigestService digestService,
            SmtpEmailClient smtpEmailClient,
            DigestRenderService digestRenderService
    ) {
        this.dailyDigestRepository = dailyDigestRepository;
        this.digestService = digestService;
        this.smtpEmailClient = smtpEmailClient;
        this.digestRenderService = digestRenderService;
    }

    @Transactional
    public DigestResponse send(Long digestId) {
        return send(digestId, false);
    }

    @Transactional
    public DigestResponse send(Long digestId, boolean forceResend) {
        DailyDigest digest = dailyDigestRepository.findById(digestId)
                .orElseThrow(() -> new IllegalArgumentException("Digest not found: " + digestId));
        if (digest.getTotalItems() <= 0) {
            throw new IllegalStateException("Digest has no items to send.");
        }
        if (ensureLlmBeforeSend) {
            digestService.finalizeDigest(digestId, true);
        }
        DigestResponse response = digestService.getDigest(digestId);
        String html = digestRenderService.renderHtml(digest, response);
        
        // 使用 SMTP 邮件服务
        logger.info("使用 SMTP 邮件服务发送邮件到: {}", digest.getUser().getEmail());
        
        boolean sent = smtpEmailClient.sendEmail(digest.getUser().getEmail(), digest.getSubject(), html);
        if (!sent) {
            throw new IllegalStateException("Digest send failed: SMTP 邮件发送失败");
        }
        
        digest.setHtmlContent(html);
        // 只有当 forceResend 为 false 时，才将状态设置为 SENT，这样可以避免覆盖已发送的记录
        // 当 forceResend 为 true 时，保持原来的状态，以便可以再次发送
        if (!forceResend) {
            digest.setStatus(DigestStatus.SENT);
            digest.setSentAt(LocalDateTime.now());
            dailyDigestRepository.save(digest);
        }
        return digestService.getDigest(digestId);
    }
}
