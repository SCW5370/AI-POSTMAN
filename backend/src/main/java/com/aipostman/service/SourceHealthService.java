package com.aipostman.service;

import com.aipostman.domain.Source;
import com.aipostman.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SourceHealthService {

    @Autowired
    private SourceRepository sourceRepository;

    // 每天凌晨 2 点执行健康检查
    @Scheduled(cron = "0 0 2 * * ?")
    public void performHealthChecks() {
        List<Source> sources = sourceRepository.findAll();
        for (Source source : sources) {
            updateHealthScore(source);
        }
    }

    public void updateHealthScore(Source source) {
        // 计算综合健康分
        BigDecimal healthScore = calculateHealthScore(source);
        source.setHealthScore(healthScore);
        source.setLastHealthCheckAt(LocalDateTime.now());
        sourceRepository.save(source);

        // 根据健康分调整源状态
        if (healthScore.compareTo(BigDecimal.valueOf(60)) < 0) {
            source.setEnabled(false);
            sourceRepository.save(source);
        }
    }

    private BigDecimal calculateHealthScore(Source source) {
        // 基础分数 100
        BigDecimal score = BigDecimal.valueOf(100);

        // 成功率权重 40%
        score = score.subtract(BigDecimal.valueOf(100).subtract(source.getSuccessRate()).multiply(BigDecimal.valueOf(0.4)));

        // 延迟权重 20%（假设延迟超过 5000ms 开始扣分）
        if (source.getAvgDelayMs() > 5000) {
            int delayExcess = source.getAvgDelayMs() - 5000;
            BigDecimal delayPenalty = BigDecimal.valueOf(Math.min(delayExcess / 100.0, 20));
            score = score.subtract(delayPenalty);
        }

        // 去重率权重 20%（去重率越高越好，假设去重率低于 50% 开始扣分）
        if (source.getDeduplicationRate().compareTo(BigDecimal.valueOf(50)) < 0) {
            score = score.subtract(BigDecimal.valueOf(50).subtract(source.getDeduplicationRate()).multiply(BigDecimal.valueOf(0.4)));
        }

        // 反馈命中率权重 20%（命中率越高越好）
        score = score.add(source.getFeedbackHitRate().multiply(BigDecimal.valueOf(0.2)));

        // 确保分数在 0-100 之间
        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
    }

    public void updateSourceMetrics(Long sourceId, boolean success, long delayMs, boolean deduplicated) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));

        // 更新成功率（简单的移动平均）
        BigDecimal currentSuccessRate = source.getSuccessRate();
        BigDecimal newSuccessRate = currentSuccessRate.multiply(BigDecimal.valueOf(9))
                .add(BigDecimal.valueOf(success ? 100 : 0))
                .divide(BigDecimal.valueOf(10), 2, BigDecimal.ROUND_HALF_UP);
        source.setSuccessRate(newSuccessRate);

        // 更新平均延迟（简单的移动平均）
        int currentAvgDelay = source.getAvgDelayMs();
        int newAvgDelay = (currentAvgDelay * 9 + (int) delayMs) / 10;
        source.setAvgDelayMs(newAvgDelay);

        // 更新去重率（简单的移动平均）
        BigDecimal currentDeduplicationRate = source.getDeduplicationRate();
        BigDecimal newDeduplicationRate = currentDeduplicationRate.multiply(BigDecimal.valueOf(9))
                .add(BigDecimal.valueOf(deduplicated ? 100 : 0))
                .divide(BigDecimal.valueOf(10), 2, BigDecimal.ROUND_HALF_UP);
        source.setDeduplicationRate(newDeduplicationRate);

        sourceRepository.save(source);
    }

    public void updateFeedbackHitRate(Long sourceId, boolean hit) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));

        // 更新反馈命中率（简单的移动平均）
        BigDecimal currentHitRate = source.getFeedbackHitRate();
        BigDecimal newHitRate = currentHitRate.multiply(BigDecimal.valueOf(9))
                .add(BigDecimal.valueOf(hit ? 100 : 0))
                .divide(BigDecimal.valueOf(10), 2, BigDecimal.ROUND_HALF_UP);
        source.setFeedbackHitRate(newHitRate);

        sourceRepository.save(source);
    }
}