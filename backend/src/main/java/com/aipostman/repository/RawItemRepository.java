package com.aipostman.repository;

import com.aipostman.domain.RawItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawItemRepository extends JpaRepository<RawItem, Long> {
    Optional<RawItem> findBySourceIdAndUrl(Long sourceId, String url);
}
