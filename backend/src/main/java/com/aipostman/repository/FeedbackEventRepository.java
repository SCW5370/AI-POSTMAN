package com.aipostman.repository;

import com.aipostman.common.enums.FeedbackType;
import com.aipostman.domain.FeedbackEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedbackEventRepository extends JpaRepository<FeedbackEvent, Long> {

    @Query("""
        select fe from FeedbackEvent fe
        where fe.user.id = :userId
          and fe.digestItem.normalizedItem.id = :normalizedItemId
        """)
    List<FeedbackEvent> findByUserIdAndNormalizedItemId(Long userId, Long normalizedItemId);

    long countByUserIdAndFeedbackType(Long userId, FeedbackType feedbackType);

    List<FeedbackEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(fe) FROM FeedbackEvent fe WHERE fe.user.id = :userId AND fe.createdAt > :after")
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
}
