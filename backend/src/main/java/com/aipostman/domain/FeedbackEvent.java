package com.aipostman.domain;

import com.aipostman.common.enums.FeedbackType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_events")
public class FeedbackEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "digest_id", nullable = false)
    private DailyDigest digest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "digest_item_id", nullable = false)
    private DailyDigestItem digestItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private FeedbackType feedbackType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public DailyDigest getDigest() { return digest; }
    public void setDigest(DailyDigest digest) { this.digest = digest; }
    public DailyDigestItem getDigestItem() { return digestItem; }
    public void setDigestItem(DailyDigestItem digestItem) { this.digestItem = digestItem; }
    public FeedbackType getFeedbackType() { return feedbackType; }
    public void setFeedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; }
}
