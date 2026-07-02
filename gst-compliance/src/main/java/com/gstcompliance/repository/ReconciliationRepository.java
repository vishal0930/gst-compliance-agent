package com.gstcompliance.repository;

import com.gstcompliance.model.ReconciliationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationRepository extends JpaRepository<ReconciliationRecord, UUID> {

    Page<ReconciliationRecord> findByUserId(UUID userId, Pageable pageable);

    List<ReconciliationRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ReconciliationRecord> findByUserIdAndTaxPeriod(UUID userId, String taxPeriod);

    Page<ReconciliationRecord> findByUserIdAndTaxPeriod(UUID userId, String taxPeriod, Pageable pageable);

    @Query("SELECT r FROM ReconciliationRecord r WHERE r.user.id = :userId AND r.status = :status")
    List<ReconciliationRecord> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") String status);

    @Query("SELECT COUNT(r) FROM ReconciliationRecord r WHERE r.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}