package com.gstcompliance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hsn_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsnCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hsn_code", length = 8, unique = true, nullable = false)
    private String hsnCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "igst_rate", precision = 5, scale = 2)
    private BigDecimal igstRate;

    @Column(name = "cgst_rate", precision = 5, scale = 2)
    private BigDecimal cgstRate;

    @Column(name = "sgst_rate", precision = 5, scale = 2)
    private BigDecimal sgstRate;

    @Column(name = "cess_rate", precision = 5, scale = 2)
    private BigDecimal cessRate;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "rate_source", length = 50)
    private String rateSource;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "notification_ref", length = 100)
    private String notificationRef;

    @Column(name = "chapter", length = 2)
    private String chapter;

    @Column(name = "heading", length = 4)
    private String heading;

    @Column(name = "sub_heading", length = 6)
    private String subHeading;

    @Column(name = "embedding", columnDefinition = "vector(768)")
    private String embedding;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BigDecimal computeTotalGstRate() {
        if (igstRate != null && igstRate.compareTo(BigDecimal.ZERO) > 0) {
            return igstRate;
        }
        BigDecimal total = BigDecimal.ZERO;
        if (cgstRate != null) total = total.add(cgstRate);
        if (sgstRate != null) total = total.add(sgstRate);
        if (cessRate != null) total = total.add(cessRate);
        return total;
    }
}
