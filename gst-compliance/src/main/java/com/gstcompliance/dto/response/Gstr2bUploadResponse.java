
package com.gstcompliance.dto.response;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bUploadResponse {

    private int totalProcessed;
    private int successfulCount;
    private int failedCount;
    private String taxPeriod;
    private java.math.BigDecimal totalItc;
    private List<InvoiceResult> results;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceResult {
        private String invoiceNumber;
        private boolean success;
        private String message;
        private UUID invoiceId;
    }
}