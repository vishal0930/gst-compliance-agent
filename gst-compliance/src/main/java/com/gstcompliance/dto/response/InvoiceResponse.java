package com.gstcompliance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private UUID id;

    private String vendorName;
    private BigDecimal taxableValue;
    private String vendorGstin;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    private BigDecimal totalAmount;

    private BigDecimal totalGst;

    private String parseStatus;

    private BigDecimal confidenceScore;

    private LocalDateTime createdAt;
}