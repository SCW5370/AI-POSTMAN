package com.aipostman.repository;

import com.aipostman.domain.FetchTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FetchTaskRepository extends JpaRepository<FetchTask, Long> {
    Optional<FetchTask> findByTaskId(String taskId);
    List<FetchTask> findByStatusIn(List<String> statuses);
    boolean existsByStatusIn(List<String> statuses);
}
