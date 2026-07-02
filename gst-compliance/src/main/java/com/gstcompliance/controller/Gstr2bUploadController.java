package com.gstcompliance.controller;

import com.gstcompliance.dto.request.Gstr2bUploadRequest;
import com.gstcompliance.dto.response.*;
import com.gstcompliance.model.Gstr2bImportSession;
import com.gstcompliance.service.Gstr2bUploadService;
import com.gstcompliance.service.GstrPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/gstr2b")
@RequiredArgsConstructor
public class Gstr2bUploadController {

    private final Gstr2bUploadService gstr2bUploadService;
    private final GstrPortalService   gstrPortalService;

    // ─────────────────────────────────────────────────────────────────
    // POST /upload  — import an entire month's GSTR-2B statement
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Gstr2bUploadResponse>> uploadGstr2bData(
            @Valid @RequestBody Gstr2bUploadRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("GSTR-2B upload request — user: {}, period: {}, replace: {}",
                email, request.getTaxPeriod(), request.isReplace());

        try {
            Gstr2bUploadResponse response =
                    gstr2bUploadService.uploadGstr2bData(request, email);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("GSTR-2B imported successfully", response));

        } catch (IllegalStateException e) {
            // Period already exists and replace=false → 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage(), "PERIOD_ALREADY_EXISTS"));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /summary?month=&year=  — typed summary with supplier count
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Gstr2bSummaryResponse>> getSummary(
            @RequestParam int month,
            @RequestParam int year,
            Authentication authentication) {

        Gstr2bSummaryResponse summary =
                gstrPortalService.getSummary(authentication.getName(), month, year);

        return ResponseEntity.ok(ApiResponse.success("Summary fetched", summary));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /invoices?month=&year=&page=&size=  — paged + filtered
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<Page<Gstr2bInvoiceResponse>>> getInvoices(
            @RequestParam int month,
            @RequestParam int year,

            // Optional filters
            @RequestParam(required = false) String supplierGstin,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String importStatus,
            @RequestParam(required = false) String matchStatus,

            // Pagination — default 25, max 250
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "25")  int size,
            @RequestParam(defaultValue = "invoiceDate,desc") String sort,

            Authentication authentication) {

        int safeSize = Math.min(size, 250);

        // Parse sort param "field,direction"
        String[] parts     = sort.split(",");
        String   sortField = parts[0];
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(dir, sortField));

        Page<Gstr2bInvoiceResponse> result = gstrPortalService.getInvoices(
                authentication.getName(),
                month, year,
                supplierGstin, supplierName, invoiceNumber,
                importStatus,  matchStatus,
                pageable);

        return ResponseEntity.ok(ApiResponse.success("Invoices fetched", result));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /import-history?page=&size=  — import session log
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/import-history")
    public ResponseEntity<ApiResponse<List<Gstr2bImportSession>>> getImportHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "importedAt"));

        List<Gstr2bImportSession> history =
                gstrPortalService.getImportHistory(authentication.getName(), pageable);

        return ResponseEntity.ok(ApiResponse.success("Import history fetched", history));
    }
}
