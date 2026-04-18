package com.aipostman.repository;

import com.aipostman.domain.Source;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {
    List<Source> findAllByEnabledTrueOrderByPriorityDesc();
    Optional<Source> findByUrl(String url);
}
