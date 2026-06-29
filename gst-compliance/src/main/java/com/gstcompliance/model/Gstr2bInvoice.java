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
                columnNames = {"user_id", "supplier_gstin", "invoice_number"}
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

    @Column(name = "grand_total", precision = 15, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "source")
    @Builder.Default
    private String source = "GSTR2B";


    @CreationTimestamp
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(
            mappedBy = "gstr2bInvoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )

    private List<Gstr2bLineItem> lineItems;
}