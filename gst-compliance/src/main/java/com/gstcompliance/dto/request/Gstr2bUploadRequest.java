package com.gstcompliance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2bUploadRequest {

    @NotEmpty(message = "Invoice list cannot be empty")
    @Valid
    private List<Gstr2bInvoiceDto> invoices;
}