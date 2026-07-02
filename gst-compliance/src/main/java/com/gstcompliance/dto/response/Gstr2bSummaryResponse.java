package com.gstcompliance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Typed summary for GET /gstr2b/summary.
 * Replaces the raw Map&lt;String, Object&gt;.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bSummaryResponse {
    private String     taxPeriod;
    private int        invoiceCount;
    private int        supplierCount;     // distinct supplier GSTINs
    private BigDecimal taxableValue;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal cess;
    private BigDecimal grandTotal;
    private BigDecimal totalItc;          // cgst + sgst + igst
}
