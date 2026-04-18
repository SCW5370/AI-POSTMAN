package com.aipostman.repository;

import com.aipostman.domain.DailyDigest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyDigestRepository extends JpaRepository<DailyDigest, Long> {
    Optional<DailyDigest> findByUserIdAndDigestDateAndDigestType(Long userId, LocalDate digestDate, String digestType);
    List<DailyDigest> findByUserIdOrderByDigestDateDesc(Long userId);
    List<DailyDigest> findByStatus(com.aipostman.common.enums.DigestStatus status);
}
