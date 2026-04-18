package com.aipostman.repository;

import com.aipostman.domain.SourceCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SourceCandidateRepository extends JpaRepository<SourceCandidate, Long> {

    List<SourceCandidate> findByStatus(String status);

    List<SourceCandidate> findByTopic(String topic);

    List<SourceCandidate> findByQueryContainingOrTopicContaining(String query, String topic);

    @Query("SELECT sc FROM SourceCandidate sc WHERE sc.url = :url")
    SourceCandidate findByUrl(@Param("url") String url);

    @Query("SELECT sc FROM SourceCandidate sc WHERE sc.confidence >= :minConfidence ORDER BY sc.confidence DESC")
    List<SourceCandidate> findByConfidenceGreaterThanEqualOrderByConfidenceDesc(@Param("minConfidence") BigDecimal minConfidence);
}