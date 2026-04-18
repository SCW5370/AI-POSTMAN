package com.aipostman.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_items")
public class RawItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    private String author;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "summary_raw", columnDefinition = "TEXT")
    private String summaryRaw;

    @Column(name = "content_raw", columnDefinition = "TEXT")
    private String contentRaw;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "raw_hash")
    private String rawHash;

    @PrePersist
    public void onCreate() {
        this.fetchedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getSummaryRaw() { return summaryRaw; }
    public void setSummaryRaw(String summaryRaw) { this.summaryRaw = summaryRaw; }
    public String getContentRaw() { return contentRaw; }
    public void setContentRaw(String contentRaw) { this.contentRaw = contentRaw; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public String getRawHash() { return rawHash; }
    public void setRawHash(String rawHash) { this.rawHash = rawHash; }
}
