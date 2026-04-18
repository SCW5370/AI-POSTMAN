package com.aipostman.repository;

import com.aipostman.domain.DailyDigestItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyDigestItemRepository extends JpaRepository<DailyDigestItem, Long> {
    List<DailyDigestItem> findByDigestIdOrderBySectionAscItemOrderAsc(Long digestId);

    @Query("""
        select distinct ddi.normalizedItem.id
        from DailyDigestItem ddi
        join ddi.digest d
        where d.user.id = :userId
          and d.status = com.aipostman.common.enums.DigestStatus.SENT
          and d.sentAt >= :since
        """)
    Set<Long> findRecentSentNormalizedItemIds(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
