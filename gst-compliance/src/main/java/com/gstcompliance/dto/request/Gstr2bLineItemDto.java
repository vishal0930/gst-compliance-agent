package com.gstcompliance.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bLineItemDto {

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Quantity must be positive")
    @Digits(integer = 15, fraction = 3, message = "Invalid decimal format")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Unit price must be positive")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal unitPrice;

    @NotBlank(message = "HSN Code is required")
    @Pattern(regexp = "^[0-9]{4,8}$", message = "Invalid HSN Code format")
    private String hsnCode;

    @NotNull(message = "GST rate is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "GST rate cannot be negative")
    @DecimalMax(value = "100.00", inclusive = true, message = "GST rate cannot exceed 100")
    @Digits(integer = 5, fraction = 2, message = "Invalid decimal format")
    private BigDecimal gstRate;

    @NotNull(message = "Taxable value is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Taxable value must be positive")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal taxableValue;

    @NotNull(message = "CGST amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "CGST cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal cgstAmount;

    @NotNull(message = "SGST amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "SGST cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal sgstAmount;

    @NotNull(message = "IGST amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "IGST cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Invalid decimal format")
    private BigDecimal igstAmount;

    // Optional fields — future-proof, not mandatory
    private BigDecimal cessAmount;

    @Builder.Default
    private Boolean itcEligible = true;

    @Builder.Default
    private Boolean reverseCharge = false;
}