package com.aipostman.repository;

import com.aipostman.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @Query("SELECT up FROM UserProfile up WHERE up.user.id = :userId")
    Optional<UserProfile> findByUserId(@Param("userId") Long userId);

    @Query("SELECT up FROM UserProfile up WHERE up.user.email = :email")
    Optional<UserProfile> findByUserEmail(@Param("email") String email);

    @Query("SELECT up FROM UserProfile up WHERE up.confidenceScore >= :minConfidence ORDER BY up.confidenceScore DESC")
    java.util.List<UserProfile> findByConfidenceScoreGreaterThanEqualOrderByConfidenceScoreDesc(@Param("minConfidence") double minConfidence);
}