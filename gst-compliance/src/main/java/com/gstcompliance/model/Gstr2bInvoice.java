package com.gstcompliance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "gstr2b_invoices",
    uniqueConstraints = @UniqueConstraint(
        name  = "gstr2b_invoices_unique_per_period",
        columnNames = {"user_id", "supplier_gstin", "invoice_number", "tax_period"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── Tax period (MM-YYYY) — the government filing period, NOT the invoice date ──
    @Column(name = "tax_period", length = 7, nullable = false)
    private String taxPeriod;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "supplier_gstin", length = 15, nullable = false)
    private String supplierGstin;

    @Column(name = "buyer_gstin", length = 15, nullable = false)
    private String buyerGstin;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "taxable_value", precision = 15, scale = 2)
    private BigDecimal taxableValue;

    @Column(name = "cgst", precision = 15, scale = 2)
    private BigDecimal cgst;

    @Column(name = "sgst", precision = 15, scale = 2)
    private BigDecimal sgst;

    @Column(name = "igst", precision = 15, scale = 2)
    private BigDecimal igst;

    @Column(name = "cess", precision = 15, scale = 2)
    private BigDecimal cess;

    @Column(name = "grand_total", precision = 15, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "source")
    @Builder.Default
    private String source = "GSTR2B";

    /**
     * PENDING → IMPORTED → MATCHED / MISMATCH / NOT_FOUND / DUPLICATE
     */
    @Column(name = "import_status", length = 20, nullable = false)
    @Builder.Default
    private String importStatus = ImportStatus.IMPORTED.name();

    @Column(name = "match_status", length = 20, nullable = false)
    @Builder.Default
    private String matchStatus = MatchStatus.NOT_CHECKED.name();

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "gstr2bInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Gstr2bLineItem> lineItems;

    public enum ImportStatus {
        PENDING, IMPORTED, MATCHED, MISMATCH, NOT_FOUND, DUPLICATE, RECONCILED
    }

    public enum MatchStatus {
        NOT_CHECKED, MATCHED, MISMATCH, NOT_FOUND, DUPLICATE
    }
}
