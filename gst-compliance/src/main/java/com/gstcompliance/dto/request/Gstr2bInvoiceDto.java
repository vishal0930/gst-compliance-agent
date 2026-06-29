package com.gstcompliance.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bInvoiceDto {

    @NotBlank(message = "Supplier name is required")
    @Size(max = 255, message = "Supplier name cannot exceed 255 characters")
    private String supplierName;

    @NotBlank(message = "Supplier GSTIN is required")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GSTIN format")
    private String supplierGstin;

    @NotBlank(message = "Buyer GSTIN is required")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GSTIN format")
    private String buyerGstin;

    @NotBlank(message = "Invoice number is required")
    @Size(max = 50, message = "Invoice number cannot exceed 50 characters")
    private String invoiceNumber;

    @NotNull(message = "Invoice date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;

    @NotNull(message = "Taxable value is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Taxable value must be positive")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal taxableValue;

    @NotNull(message = "CGST is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "CGST cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal cgst;

    @NotNull(message = "SGST is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "SGST cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal sgst;

    @NotNull(message = "IGST is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "IGST cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal igst;

    @NotNull(message = "Grand total is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Grand total must be positive")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal grandTotal;

    @NotEmpty(message = "Line items cannot be empty")
    @Valid
    private List<Gstr2bLineItemDto> lineItems;
}