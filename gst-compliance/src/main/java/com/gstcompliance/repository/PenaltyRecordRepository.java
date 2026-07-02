package com.gstcompliance.repository;

import com.gstcompliance.model.PenaltyRecord;
import com.gstcompliance.model.PenaltyRecord.PenaltyStatus;
import com.gstcompliance.model.PenaltyRecord.ReturnType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PenaltyRecordRepository extends JpaRepository<PenaltyRecord, UUID> {

    List<PenaltyRecord> findByUserIdOrderByCalculatedAtDesc(UUID userId);

    Page<PenaltyRecord> findByUserIdOrderByCalculatedAtDesc(UUID userId, Pageable pageable);

    List<PenaltyRecord> findByUserIdAndStatus(UUID userId, PenaltyStatus status);

    PenaltyRecord findByUserIdAndTaxPeriodAndReturnType(
            UUID userId,
            String taxPeriod,
            ReturnType returnType
    );

    @Query("SELECT p FROM PenaltyRecord p WHERE p.user.id = :userId AND p.taxPeriod = :period")
    List<PenaltyRecord> findByUserIdAndTaxPeriod(@Param("userId") UUID userId, @Param("period") String period);

    @Query("SELECT p FROM PenaltyRecord p WHERE p.user.id = :userId AND p.status = :status ORDER BY p.calculatedAt DESC")
    List<PenaltyRecord> findByUserIdAndStatusOrderByCalculatedAtDesc(
            @Param("userId") UUID userId,
            @Param("status") PenaltyStatus status
    );

    @Query("SELECT p FROM PenaltyRecord p WHERE p.dueDate < :date AND p.status = 'PENDING'")
    List<PenaltyRecord> findOverduePendingPenalties(@Param("date") LocalDate date);

    @Query("SELECT SUM(p.totalPenalty) FROM PenaltyRecord p WHERE p.user.id = :userId AND p.status = 'PENDING'")
    java.math.BigDecimal sumPendingPenaltiesByUserId(@Param("userId") UUID userId);
}
