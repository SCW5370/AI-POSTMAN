package com.aipostman.domain;

import com.aipostman.common.enums.ItemStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "normalized_items")
public class NormalizedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "raw_item_id", nullable = false, unique = true)
    private RawItem rawItem;

    @Column(name = "canonical_url")
    private String canonicalUrl;

    @Column(name = "title_clean", nullable = false)
    private String titleClean;

    @Column(name = "summary_clean", columnDefinition = "TEXT")
    private String summaryClean;

    @Column(name = "content_clean", columnDefinition = "TEXT")
    private String contentClean;

    private String language;

    @Type(JsonBinaryType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> tags = new ArrayList<>();

    @Column(name = "source_quality_score", nullable = false)
    private BigDecimal sourceQualityScore = BigDecimal.ZERO;

    @Column(name = "freshness_score", nullable = false)
    private BigDecimal freshnessScore = BigDecimal.ZERO;

    @Column(name = "dedup_group_key")
    private String dedupGroupKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.READY;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RawItem getRawItem() { return rawItem; }
    public void setRawItem(RawItem rawItem) { this.rawItem = rawItem; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }
    public String getTitleClean() { return titleClean; }
    public void setTitleClean(String titleClean) { this.titleClean = titleClean; }
    public String getSummaryClean() { return summaryClean; }
    public void setSummaryClean(String summaryClean) { this.summaryClean = summaryClean; }
    public String getContentClean() { return contentClean; }
    public void setContentClean(String contentClean) { this.contentClean = contentClean; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public BigDecimal getSourceQualityScore() { return sourceQualityScore; }
    public void setSourceQualityScore(BigDecimal sourceQualityScore) { this.sourceQualityScore = sourceQualityScore; }
    public BigDecimal getFreshnessScore() { return freshnessScore; }
    public void setFreshnessScore(BigDecimal freshnessScore) { this.freshnessScore = freshnessScore; }
    public String getDedupGroupKey() { return dedupGroupKey; }
    public void setDedupGroupKey(String dedupGroupKey) { this.dedupGroupKey = dedupGroupKey; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
}
