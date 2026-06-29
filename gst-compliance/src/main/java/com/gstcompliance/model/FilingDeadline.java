package com.gstcompliance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "filing_deadlines",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "form_type", "due_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilingDeadline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "form_type", length = 20, nullable = false)
    private String formType;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "penalty_per_day", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal penaltyPerDay = new BigDecimal("50.00");

    @Column(name = "severity", length = 10)
    @Builder.Default
    private String severity = "MEDIUM";

    @Column(name = "notified")
    @Builder.Default
    private Boolean notified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum FormType {
        GSTR_1, GSTR_3B, GSTR_9
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}