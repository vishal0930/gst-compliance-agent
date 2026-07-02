package com.gstcompliance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gstr2b_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gstr2b_invoice_id", nullable = false)
    private Gstr2bInvoice gstr2bInvoice;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "hsn_code", length = 8)
    private String hsnCode;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "taxable_value", precision = 15, scale = 2)
    private BigDecimal taxableValue;

    @Column(name = "cgst_amount", precision = 15, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_amount", precision = 15, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_amount", precision = 15, scale = 2)
    private BigDecimal igstAmount;

    // ── Future-proof fields ──────────────────────────────────────────────
    @Column(name = "cess", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cess = BigDecimal.ZERO;

    /** Whether this line item's GST is eligible for ITC credit */
    @Column(name = "itc_eligible", nullable = false)
    @Builder.Default
    private Boolean itcEligible = true;

    /** Section 9(3) / 9(4) reverse charge flag */
    @Column(name = "reverse_charge", nullable = false)
    @Builder.Default
    private Boolean reverseCharge = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
