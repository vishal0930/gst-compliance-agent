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
public class ReconciliationResponse {
    private String period;
    private Integer totalInvoices;
    private Integer matchedCount;
    private Integer mismatchCount;
    private BigDecimal itcAtRisk;
    private List<MismatchDTO> mismatches;
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MismatchDTO {

        private String invoiceNumber;
        private String supplierGstin;
        private BigDecimal bookAmount;
        private BigDecimal portalAmount;
        private BigDecimal diffAmount;
        private BigDecimal diffPercent;
        private String description;
        private String recommendation;
        private String status;

        private String hsnCode;

        private BigDecimal expectedGst;

        private BigDecimal actualGst;

        private String bookInvoiceDate;

        private String portalInvoiceDate;

        private Integer lineItemCount;

        private BigDecimal riskAmount;
    }
}