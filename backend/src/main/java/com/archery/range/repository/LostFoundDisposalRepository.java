package com.archery.range.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.archery.range.domain.LostFoundDisposal;

public interface LostFoundDisposalRepository extends JpaRepository<LostFoundDisposal, Long> {
    List<LostFoundDisposal> findByItemIdOrderByIdDesc(Long itemId);

    List<LostFoundDisposal> findAllByOrderByIdDesc();

    boolean existsByDisposalNo(String disposalNo);

}
