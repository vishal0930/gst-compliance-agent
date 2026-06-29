package com.gstcompliance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceParseResponse {

    private String vendorName;
    private String vendorGstin;
    private String invoiceNumber;
    private String invoiceDate;

    // NEW
    private BigDecimal taxableValue;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;

    private BigDecimal totalAmount;
    private BigDecimal totalGst;

    private List<LineItemDTO> lineItems;

    private BigDecimal confidenceScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemDTO {

        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;

        private String hsnCode;
        private BigDecimal gstRate;

        private BigDecimal taxableValue;

        // NEW
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
    }
}