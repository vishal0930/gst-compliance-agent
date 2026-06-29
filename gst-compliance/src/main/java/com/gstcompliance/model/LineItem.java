package com.gstcompliance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
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

    @Column(name = "hsn_confidence", precision = 5, scale = 4)
    private BigDecimal hsnConfidence;

    @Column(name = "needs_review")
    @Builder.Default
    private Boolean needsReview = false;

    @Column(name = "review_reason", length = 100)
    private String reviewReason;
}