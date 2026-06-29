package com.gstcompliance.repository;

import com.gstcompliance.model.FilingDeadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FilingDeadlineRepository extends JpaRepository<FilingDeadline, UUID> {
    List<FilingDeadline> findByUserId(UUID userId);
    List<FilingDeadline> findByUserIdAndNotifiedFalse(UUID userId);
    List<FilingDeadline> findByDueDateBefore(LocalDate dueDate);
}