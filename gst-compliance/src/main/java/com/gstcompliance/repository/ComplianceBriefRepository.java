package com.gstcompliance.repository;

import com.gstcompliance.model.ComplianceBrief;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ComplianceBriefRepository extends JpaRepository<ComplianceBrief, UUID> {
    Page<ComplianceBrief> findByUserId(UUID userId, Pageable pageable);
}