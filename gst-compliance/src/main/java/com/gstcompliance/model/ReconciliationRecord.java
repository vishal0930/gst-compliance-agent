package com.gstcompliance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tax_period", length = 7, nullable = false)
    private String taxPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private Status status = Status.RUNNING;

    @Column(name = "total_invoices")
    @Builder.Default
    private Integer totalInvoices = 0;

    @Column(name = "matched_count")
    @Builder.Default
    private Integer matchedCount = 0;

    @Column(name = "mismatch_count")
    @Builder.Default
    private Integer mismatchCount = 0;

    @Column(name = "itc_at_risk", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal itcAtRisk = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_json", columnDefinition = "jsonb")
    private String reportJson;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        RUNNING, DONE, FAILED
    }
}