package com.gstcompliance.repository;

import com.gstcompliance.model.Gstr2bImportSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Gstr2bImportSessionRepository extends JpaRepository<Gstr2bImportSession, UUID> {

    List<Gstr2bImportSession> findByUserIdOrderByImportedAtDesc(UUID userId);

    Page<Gstr2bImportSession> findByUserIdOrderByImportedAtDesc(UUID userId, Pageable pageable);

    Optional<Gstr2bImportSession> findTopByUserIdAndTaxPeriodOrderByImportedAtDesc(
            UUID userId, String taxPeriod);

    boolean existsByUserIdAndTaxPeriod(UUID userId, String taxPeriod);
}
