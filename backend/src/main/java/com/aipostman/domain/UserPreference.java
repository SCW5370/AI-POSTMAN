package com.aipostman.domain;

import com.aipostman.common.enums.DeliveryMode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Type(JsonBinaryType.class)
    @Column(name = "preferred_topics", nullable = false, columnDefinition = "jsonb")
    private List<String> preferredTopics = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(name = "blocked_topics", nullable = false, columnDefinition = "jsonb")
    private List<String> blockedTopics = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(name = "preferred_sources", nullable = false, columnDefinition = "jsonb")
    private List<String> preferredSources = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false)
    private DeliveryMode deliveryMode = DeliveryMode.BALANCED;

    @Column(name = "delivery_time", nullable = false)
    private String deliveryTime = "08:00";

    @Column(name = "max_items_per_digest", nullable = false)
    private Integer maxItemsPerDigest = 5;

    @Column(name = "exploration_ratio", nullable = false)
    private BigDecimal explorationRatio = BigDecimal.valueOf(0.10);

    @Column(name = "delivery_frequency", nullable = false)
    private String deliveryFrequency = "weekly";

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
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }
    public List<String> getPreferredTopics() { return preferredTopics; }
    public void setPreferredTopics(List<String> preferredTopics) { this.preferredTopics = preferredTopics; }
    public List<String> getBlockedTopics() { return blockedTopics; }
    public void setBlockedTopics(List<String> blockedTopics) { this.blockedTopics = blockedTopics; }
    public List<String> getPreferredSources() { return preferredSources; }
    public void setPreferredSources(List<String> preferredSources) { this.preferredSources = preferredSources; }
    public DeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(DeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }
    public String getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(String deliveryTime) { this.deliveryTime = deliveryTime; }
    public Integer getMaxItemsPerDigest() { return maxItemsPerDigest; }
    public void setMaxItemsPerDigest(Integer maxItemsPerDigest) { this.maxItemsPerDigest = maxItemsPerDigest; }
    public BigDecimal getExplorationRatio() { return explorationRatio; }
    public void setExplorationRatio(BigDecimal explorationRatio) { this.explorationRatio = explorationRatio; }
    public String getDeliveryFrequency() { return deliveryFrequency; }
    public void setDeliveryFrequency(String deliveryFrequency) { this.deliveryFrequency = deliveryFrequency; }
}
