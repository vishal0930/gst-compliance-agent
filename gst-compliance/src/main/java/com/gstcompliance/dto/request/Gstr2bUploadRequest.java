package com.gstcompliance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bUploadRequest {

    /** Tax period month (1–12). Government GSTR-2B is issued per calendar month. */
    @NotNull(message = "Month is required")
    @Min(value = 1,  message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    /** Tax period year e.g. 2025 */
    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be 2020 or later")
    @Max(value = 2030, message = "Year must be 2030 or earlier")
    private Integer year;

    /**
     * Whether to replace an existing import for this period.
     * If false and the period already exists the upload is rejected.
     */
    @Builder.Default
    private boolean replace = false;

    @NotEmpty(message = "Invoice list cannot be empty")
    @Valid
    private List<Gstr2bInvoiceDto> invoices;

    /** Derived helper — format "MM-YYYY" */
    public String getTaxPeriod() {
        return String.format("%02d-%04d", month, year);
    }
}
