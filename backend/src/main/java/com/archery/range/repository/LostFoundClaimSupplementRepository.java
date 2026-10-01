package com.archery.range.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.archery.range.domain.LostFoundClaimSupplement;

public interface LostFoundClaimSupplementRepository extends JpaRepository<LostFoundClaimSupplement, Long> {
    List<LostFoundClaimSupplement> findByClaimIdOrderByIdAsc(Long claimId);
}
