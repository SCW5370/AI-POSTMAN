package com.aipostman.domain;

import com.aipostman.common.enums.DigestStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_digests")
public class DailyDigest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "digest_date", nullable = false)
    private LocalDate digestDate;

    @Column(name = "digest_type", nullable = false)
    private String digestType = "daily";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DigestStatus status = DigestStatus.DRAFT;

    private String subject;

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "llm_used")
    private Boolean llmUsed = false;

    @Column(name = "editorial_strategy")
    private String editorialStrategy = "heuristic";

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
    public LocalDate getDigestDate() { return digestDate; }
    public void setDigestDate(LocalDate digestDate) { this.digestDate = digestDate; }
    public String getDigestType() { return digestType; }
    public void setDigestType(String digestType) { this.digestType = digestType; }
    public DigestStatus getStatus() { return status; }
    public void setStatus(DigestStatus status) { this.status = status; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public Boolean getLlmUsed() { return llmUsed; }
    public void setLlmUsed(Boolean llmUsed) { this.llmUsed = llmUsed; }
    public String getEditorialStrategy() { return editorialStrategy; }
    public void setEditorialStrategy(String editorialStrategy) { this.editorialStrategy = editorialStrategy; }
}
