package com.archery.range.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.archery.range.domain.LostFoundClaim;

public interface LostFoundClaimRepository extends JpaRepository<LostFoundClaim, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LostFoundClaim c where c.id = :id")
    Optional<LostFoundClaim> findByIdForUpdate(@Param("id") Long id);

    List<LostFoundClaim> findAllByOrderByIdDesc();

    List<LostFoundClaim> findByItemIdOrderByIdDesc(Long itemId);

    List<LostFoundClaim> findByStatusOrderByIdDesc(String status);

    List<LostFoundClaim> findByItemIdAndStatus(Long itemId, String status);

    boolean existsByClaimNo(String claimNo);

}
