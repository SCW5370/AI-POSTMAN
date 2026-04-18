package com.aipostman.repository;

import com.aipostman.domain.ItemEnrichment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemEnrichmentRepository extends JpaRepository<ItemEnrichment, Long> {
    Optional<ItemEnrichment> findByNormalizedItemId(Long normalizedItemId);
    List<ItemEnrichment> findByNormalizedItemIdIn(List<Long> normalizedItemIds);
}
