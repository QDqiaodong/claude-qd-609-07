package com.archery.range.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.archery.range.domain.LostFoundChainLog;

public interface LostFoundChainLogRepository extends JpaRepository<LostFoundChainLog, Long> {
    List<LostFoundChainLog> findByItemIdOrderByIdAsc(Long itemId);

    Optional<LostFoundChainLog> findByRequestKey(String requestKey);

}
