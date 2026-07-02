package com.gstcompliance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records every GSTR-2B bulk import event.
 * Enables the Import History view on the frontend.
 */
@Entity
@Table(name = "gstr2b_import_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bImportSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Format MM-YYYY e.g. "07-2025" */
    @Column(name = "tax_period", length = 7, nullable = false)
    private String taxPeriod;

    @Column(name = "total_invoices", nullable = false)
    @Builder.Default
    private Integer totalInvoices = 0;

    @Column(name = "successful", nullable = false)
    @Builder.Default
    private Integer successful = 0;

    @Column(name = "failed", nullable = false)
    @Builder.Default
    private Integer failed = 0;

    @Column(name = "total_taxable", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxable = BigDecimal.ZERO;

    @Column(name = "total_cgst", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalCgst = BigDecimal.ZERO;

    @Column(name = "total_sgst", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalSgst = BigDecimal.ZERO;

    @Column(name = "total_igst", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalIgst = BigDecimal.ZERO;

    @Column(name = "total_itc", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalItc = BigDecimal.ZERO;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
