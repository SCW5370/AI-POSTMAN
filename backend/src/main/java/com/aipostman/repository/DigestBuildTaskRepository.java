package com.aipostman.repository;

import com.aipostman.domain.DigestBuildTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestBuildTaskRepository extends JpaRepository<DigestBuildTask, Long> {
    Optional<DigestBuildTask> findByTaskId(String taskId);
    List<DigestBuildTask> findByStatusIn(List<String> statuses);
}
