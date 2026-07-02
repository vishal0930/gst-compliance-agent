package com.gstcompliance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private UUID id;

    private String vendorName;
    private String vendorGstin;

    private String invoiceNumber;
    private LocalDate invoiceDate;

    private BigDecimal taxableValue;   // sum of line-item taxable values
    private BigDecimal totalAmount;
    private BigDecimal totalGst;
    private BigDecimal cgstAmount;     // sum of line-item CGST
    private BigDecimal sgstAmount;     // sum of line-item SGST
    private BigDecimal igstAmount;     // sum of line-item IGST

    private String parseStatus;
    private BigDecimal confidenceScore;
    private LocalDateTime createdAt;

    private List<LineItemResponse> lineItems;

    // ---------------------------------------------------------------
    // Nested DTO — mirrors LineItem entity exactly
    // ---------------------------------------------------------------
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemResponse {
        private UUID id;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String hsnCode;
        private BigDecimal gstRate;
        private BigDecimal taxableValue;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal hsnConfidence;
        private Boolean needsReview;
        private String reviewReason;
    }
}
