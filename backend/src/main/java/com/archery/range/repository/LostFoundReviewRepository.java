package com.archery.range.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.archery.range.domain.LostFoundReview;

public interface LostFoundReviewRepository extends JpaRepository<LostFoundReview, Long> {
    List<LostFoundReview> findByClaimIdOrderByIdAsc(Long claimId);

    int countByClaimId(Long claimId);

}
