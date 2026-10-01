package com.archery.range.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.archery.range.domain.LostFoundItem;

public interface LostFoundItemRepository extends JpaRepository<LostFoundItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from LostFoundItem i where i.id = :id")
    Optional<LostFoundItem> findByIdForUpdate(@Param("id") Long id);

    List<LostFoundItem> findAllByOrderByIdDesc();

    List<LostFoundItem> findByStatusOrderByIdDesc(String status);

    boolean existsByItemNo(String itemNo);

    @Query("select i from LostFoundItem i where i.status = 'CUSTODY' and i.retentionUntil < current date order by i.id desc")
    List<LostFoundItem> findOverdueForDisposal();

}
