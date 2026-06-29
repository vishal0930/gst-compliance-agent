package com.gstcompliance.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileRequest {
    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull
    @Min(2020)
    @Max(2030)
    private Integer year;

    @NotNull
    private String userEmail;  // ✅ Add this field
}