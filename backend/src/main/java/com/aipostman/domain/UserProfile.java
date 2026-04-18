package com.aipostman.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Type(JsonBinaryType.class)
    @Column(name = "interests", columnDefinition = "JSONB")
    private Map<String, Object> interests;

    @Column(name = "occupation")
    private String occupation;

    @Type(JsonBinaryType.class)
    @Column(name = "recent_activities", columnDefinition = "JSONB")
    private Map<String, Object> recentActivities;

    @Type(JsonBinaryType.class)
    @Column(name = "preferred_topics", columnDefinition = "JSONB")
    private Map<String, Object> preferredTopics;

    @Type(JsonBinaryType.class)
    @Column(name = "reading_habits", columnDefinition = "JSONB")
    private Map<String, Object> readingHabits;

    @Type(JsonBinaryType.class)
    @Column(name = "time_preferences", columnDefinition = "JSONB")
    private Map<String, Object> timePreferences;

    @Type(JsonBinaryType.class)
    @Column(name = "content_preferences", columnDefinition = "JSONB")
    private Map<String, Object> contentPreferences;

    @Type(JsonBinaryType.class)
    @Column(name = "technical_skills", columnDefinition = "JSONB")
    private Map<String, Object> technicalSkills;

    @Column(name = "experience_level")
    private String experienceLevel;

    @Type(JsonBinaryType.class)
    @Column(name = "learning_goals", columnDefinition = "JSONB")
    private Map<String, Object> learningGoals;

    @Type(JsonBinaryType.class)
    @Column(name = "industry_focus", columnDefinition = "JSONB")
    private Map<String, Object> industryFocus;

    @Type(JsonBinaryType.class)
    @Column(name = "coding_habits", columnDefinition = "JSONB")
    private Map<String, Object> codingHabits;

    @Type(JsonBinaryType.class)
    @Column(name = "tool_preferences", columnDefinition = "JSONB")
    private Map<String, Object> toolPreferences;

    @Type(JsonBinaryType.class)
    @Column(name = "content_formats", columnDefinition = "JSONB")
    private Map<String, Object> contentFormats;

    @Type(JsonBinaryType.class)
    @Column(name = "time_availability", columnDefinition = "JSONB")
    private Map<String, Object> timeAvailability;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastUpdatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Map<String, Object> getInterests() { return interests; }
    public void setInterests(Map<String, Object> interests) { this.interests = interests; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public Map<String, Object> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(Map<String, Object> recentActivities) { this.recentActivities = recentActivities; }
    public Map<String, Object> getPreferredTopics() { return preferredTopics; }
    public void setPreferredTopics(Map<String, Object> preferredTopics) { this.preferredTopics = preferredTopics; }
    public Map<String, Object> getReadingHabits() { return readingHabits; }
    public void setReadingHabits(Map<String, Object> readingHabits) { this.readingHabits = readingHabits; }
    public Map<String, Object> getTimePreferences() { return timePreferences; }
    public void setTimePreferences(Map<String, Object> timePreferences) { this.timePreferences = timePreferences; }
    public Map<String, Object> getContentPreferences() { return contentPreferences; }
    public void setContentPreferences(Map<String, Object> contentPreferences) { this.contentPreferences = contentPreferences; }
    public Map<String, Object> getTechnicalSkills() { return technicalSkills; }
    public void setTechnicalSkills(Map<String, Object> technicalSkills) { this.technicalSkills = technicalSkills; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public Map<String, Object> getLearningGoals() { return learningGoals; }
    public void setLearningGoals(Map<String, Object> learningGoals) { this.learningGoals = learningGoals; }
    public Map<String, Object> getIndustryFocus() { return industryFocus; }
    public void setIndustryFocus(Map<String, Object> industryFocus) { this.industryFocus = industryFocus; }
    public Map<String, Object> getCodingHabits() { return codingHabits; }
    public void setCodingHabits(Map<String, Object> codingHabits) { this.codingHabits = codingHabits; }
    public Map<String, Object> getToolPreferences() { return toolPreferences; }
    public void setToolPreferences(Map<String, Object> toolPreferences) { this.toolPreferences = toolPreferences; }
    public Map<String, Object> getContentFormats() { return contentFormats; }
    public void setContentFormats(Map<String, Object> contentFormats) { this.contentFormats = contentFormats; }
    public Map<String, Object> getTimeAvailability() { return timeAvailability; }
    public void setTimeAvailability(Map<String, Object> timeAvailability) { this.timeAvailability = timeAvailability; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}