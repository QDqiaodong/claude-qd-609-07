package com.archery.range.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.archery.range.domain.LostFoundException;

public interface LostFoundExceptionRepository extends JpaRepository<LostFoundException, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from LostFoundException e where e.id = :id")
    Optional<LostFoundException> findByIdForUpdate(@Param("id") Long id);

    List<LostFoundException> findByClaimIdOrderByIdDesc(Long claimId);

    List<LostFoundException> findByItemIdAndStatusOrderByIdDesc(Long itemId, String status);

    List<LostFoundException> findByStatusOrderByIdDesc(String status);

    boolean existsByExceptionNo(String exceptionNo);

}
