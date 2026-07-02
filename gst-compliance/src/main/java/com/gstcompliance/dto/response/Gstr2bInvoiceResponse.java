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

/**
 * Replaces Map&lt;String, Object&gt; in all GSTR-2B API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bInvoiceResponse {

    private UUID   id;
    private String taxPeriod;
    private String supplierName;
    private String supplierGstin;
    private String buyerGstin;
    private String invoiceNumber;

    private LocalDate   invoiceDate;
    private BigDecimal  taxableValue;
    private BigDecimal  cgst;
    private BigDecimal  sgst;
    private BigDecimal  igst;
    private BigDecimal  cess;
    private BigDecimal  grandTotal;

    private String importStatus;
    private String matchStatus;

    private LocalDateTime uploadedAt;

    private List<LineItemResponse> lineItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemResponse {
        private UUID       id;
        private String     description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String     hsnCode;
        private BigDecimal gstRate;
        private BigDecimal taxableValue;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal cess;
        private Boolean    itcEligible;
        private Boolean    reverseCharge;
    }
}
