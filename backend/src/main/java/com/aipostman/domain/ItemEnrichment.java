package com.aipostman.domain;

import com.aipostman.common.enums.ActionHint;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "item_enrichments")
public class ItemEnrichment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "normalized_item_id", nullable = false, unique = true)
    private NormalizedItem normalizedItem;

    @Column(name = "short_summary", columnDefinition = "TEXT")
    private String shortSummary;

    @Column(name = "relevance_reason", columnDefinition = "TEXT")
    private String relevanceReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_hint")
    private ActionHint actionHint;

    @Type(JsonBinaryType.class)
    @Column(name = "llm_tags", nullable = false, columnDefinition = "jsonb")
    private List<String> llmTags = new ArrayList<>();

    @Column(name = "enrichment_status", nullable = false)
    private String enrichmentStatus = "pending";

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
    public NormalizedItem getNormalizedItem() { return normalizedItem; }
    public void setNormalizedItem(NormalizedItem normalizedItem) { this.normalizedItem = normalizedItem; }
    public String getShortSummary() { return shortSummary; }
    public void setShortSummary(String shortSummary) { this.shortSummary = shortSummary; }
    public String getRelevanceReason() { return relevanceReason; }
    public void setRelevanceReason(String relevanceReason) { this.relevanceReason = relevanceReason; }
    public ActionHint getActionHint() { return actionHint; }
    public void setActionHint(ActionHint actionHint) { this.actionHint = actionHint; }
    public List<String> getLlmTags() { return llmTags; }
    public void setLlmTags(List<String> llmTags) { this.llmTags = llmTags; }
    public String getEnrichmentStatus() { return enrichmentStatus; }
    public void setEnrichmentStatus(String enrichmentStatus) { this.enrichmentStatus = enrichmentStatus; }
}
