package com.aipostman.repository;

import com.aipostman.domain.NormalizedItem;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NormalizedItemRepository extends JpaRepository<NormalizedItem, Long> {

    java.util.Optional<NormalizedItem> findByRawItemId(Long rawItemId);

    @Query("""
        select ni from NormalizedItem ni
        join fetch ni.rawItem ri
        join fetch ri.source s
        where ni.status = com.aipostman.common.enums.ItemStatus.READY
          and ri.publishedAt >= :since
        order by ri.publishedAt desc
        """)
    List<NormalizedItem> findRecentReadyItems(LocalDateTime since);
}
