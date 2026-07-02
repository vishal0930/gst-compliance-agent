package com.gstcompliance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "penalty_records", indexes = {
    @Index(name = "idx_penalty_user", columnList = "user_id"),
    @Index(name = "idx_penalty_period", columnList = "tax_period"),
    @Index(name = "idx_penalty_return_type", columnList = "return_type"),
    @Index(name = "idx_penalty_status", columnList = "status"),
    @Index(name = "idx_penalty_due_date", columnList = "due_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tax_period", nullable = false, length = 7)
    private String taxPeriod; // Format: MM-YYYY

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false)
    private ReturnType returnType;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "delay_days")
    private Integer delayDays;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate; // e.g., 18.00

    @Column(name = "interest_amount", precision = 15, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "late_fee_per_day", precision = 10, scale = 2)
    private BigDecimal lateFeePerDay; // e.g., 200.00

    @Column(name = "late_fee_total", precision = 15, scale = 2)
    private BigDecimal lateFeeTotal;

    @Column(name = "total_penalty", precision = 15, scale = 2)
    private BigDecimal totalPenalty;

    @Column(name = "tax_liability", precision = 15, scale = 2)
    private BigDecimal taxLiability;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PenaltyStatus status;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
        if (status == null) {
            status = PenaltyStatus.PENDING;
        }
    }

    public enum ReturnType {
        GSTR3B,
        GSTR1,
        GSTR9
    }

    public enum PenaltyStatus {
        PENDING,
        CALCULATED,
        PAID,
        WAIVED
    }
}
