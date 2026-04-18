package com.aipostman.domain;

import com.aipostman.common.enums.SourceType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    private String category;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int priority = 50;

    private String language = "zh";

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @Column(name = "health_score", nullable = false)
    private BigDecimal healthScore = BigDecimal.valueOf(100);

    @Column(name = "success_rate", nullable = false)
    private BigDecimal successRate = BigDecimal.valueOf(100);

    @Column(name = "avg_delay_ms", nullable = false)
    private int avgDelayMs = 0;

    @Column(name = "deduplication_rate", nullable = false)
    private BigDecimal deduplicationRate = BigDecimal.ZERO;

    @Column(name = "feedback_hit_rate", nullable = false)
    private BigDecimal feedbackHitRate = BigDecimal.ZERO;

    @Column(name = "last_health_check_at")
    private LocalDateTime lastHealthCheckAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(LocalDateTime lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }
    public BigDecimal getHealthScore() { return healthScore; }
    public void setHealthScore(BigDecimal healthScore) { this.healthScore = healthScore; }
    public BigDecimal getSuccessRate() { return successRate; }
    public void setSuccessRate(BigDecimal successRate) { this.successRate = successRate; }
    public int getAvgDelayMs() { return avgDelayMs; }
    public void setAvgDelayMs(int avgDelayMs) { this.avgDelayMs = avgDelayMs; }
    public BigDecimal getDeduplicationRate() { return deduplicationRate; }
    public void setDeduplicationRate(BigDecimal deduplicationRate) { this.deduplicationRate = deduplicationRate; }
    public BigDecimal getFeedbackHitRate() { return feedbackHitRate; }
    public void setFeedbackHitRate(BigDecimal feedbackHitRate) { this.feedbackHitRate = feedbackHitRate; }
    public LocalDateTime getLastHealthCheckAt() { return lastHealthCheckAt; }
    public void setLastHealthCheckAt(LocalDateTime lastHealthCheckAt) { this.lastHealthCheckAt = lastHealthCheckAt; }
}
