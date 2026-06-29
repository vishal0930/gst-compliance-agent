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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "compliance_briefs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceBrief {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tax_period", length = 7, nullable = false)
    private String taxPeriod;

    @Column(name = "brief_text", columnDefinition = "TEXT")
    private String briefText;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_gst", precision = 15, scale = 2)
    private BigDecimal totalGst;

    @Column(name = "total_itc", precision = 15, scale = 2)
    private BigDecimal totalItc;

    @Column(name = "tax_liability", precision = 15, scale = 2)
    private BigDecimal taxLiability;

    @Column(name = "itc_at_risk", precision = 15, scale = 2)
    private BigDecimal itcAtRisk;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_items", columnDefinition = "jsonb")
    private String actionItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gstr3b_draft", columnDefinition = "jsonb")
    private String gstr3bDraft;

    @Column(name = "is_complete")
    @Builder.Default
    private Boolean isComplete = false;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = false;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}