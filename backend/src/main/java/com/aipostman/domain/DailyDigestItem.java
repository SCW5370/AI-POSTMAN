package com.aipostman.domain;

import com.aipostman.common.enums.DigestSection;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_digest_items")
public class DailyDigestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "digest_id", nullable = false)
    private DailyDigest digest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "normalized_item_id", nullable = false)
    private NormalizedItem normalizedItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DigestSection section;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(name = "final_score", nullable = false)
    private BigDecimal finalScore = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DailyDigest getDigest() { return digest; }
    public void setDigest(DailyDigest digest) { this.digest = digest; }
    public NormalizedItem getNormalizedItem() { return normalizedItem; }
    public void setNormalizedItem(NormalizedItem normalizedItem) { this.normalizedItem = normalizedItem; }
    public DigestSection getSection() { return section; }
    public void setSection(DigestSection section) { this.section = section; }
    public int getItemOrder() { return itemOrder; }
    public void setItemOrder(int itemOrder) { this.itemOrder = itemOrder; }
    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
}
