package com.gstcompliance.controller;

import com.gstcompliance.agent.Gstr2bReconcilerAgent;
import com.gstcompliance.agent.base.AgentResult;
import com.gstcompliance.dto.request.ReconcileRequest;
import com.gstcompliance.dto.response.ApiResponse;
import com.gstcompliance.dto.response.ReconciliationResponse;
import com.gstcompliance.model.ReconciliationRecord;
import com.gstcompliance.service.InvoiceService;
import com.gstcompliance.service.GstrPortalService;
import com.gstcompliance.util.ExcelExporter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final InvoiceService invoiceService;

    private final ExcelExporter excelExporter;
    private final Gstr2bReconcilerAgent reconcilerAgent;

    // ✅ ONE METHOD ONLY - Removed duplicate
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<ReconciliationResponse>> runReconciliation(
            @Valid @RequestBody ReconcileRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Running reconciliation for user: {}, period: {}-{}",
                email, request.getMonth(), request.getYear());

        try {
            // Set user email for the request
            request.setUserEmail(email);

            // Execute reconciliation agent
            AgentResult<ReconciliationResponse> result = reconcilerAgent.execute(request);

            if (!result.isSuccess()) {
                throw new RuntimeException(result.getErrorMessage());
            }

            ReconciliationResponse response = result.getData();

            return ResponseEntity.ok(ApiResponse.success("Reconciliation completed successfully", response));

        } catch (Exception e) {
            log.error("Reconciliation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Reconciliation failed: " + e.getMessage(), "RECONCILIATION_ERROR"));
        }
    }
    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<ReconciliationResponse>> reconcile(
            @Valid @RequestBody ReconcileRequest request,
            Authentication authentication) {

        return runReconciliation(request, authentication);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReconciliationRecord>>> getReconciliations(
            Authentication authentication,
            Pageable pageable) {

        String email = authentication.getName();
        Page<ReconciliationRecord> records = invoiceService.getReconciliationRecords(email, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reconciliations retrieved", records));
    }
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<ReconciliationRecord>>> getHistory(
            Authentication authentication,
            Pageable pageable) {

        return getReconciliations(authentication, pageable);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReconciliationResponse>> getReconciliation(
            @PathVariable UUID id,
            Authentication authentication) {

        String email = authentication.getName();
        ReconciliationRecord record = invoiceService.getReconciliationRecords(email, Pageable.unpaged())
                .stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Reconciliation record not found"));

        ReconciliationResponse response =
                invoiceService.getReconciliation(email, record.getTaxPeriod());
        return ResponseEntity.ok(ApiResponse.success("Reconciliation retrieved", response));
    }

    @GetMapping("/{id}/mismatches")
    public ResponseEntity<ApiResponse<Object>> getMismatches(
            @PathVariable UUID id,
            @RequestParam(required = false) String type,
            Authentication authentication) {

        String email = authentication.getName();
        ReconciliationRecord record = invoiceService.getReconciliationRecords(email, Pageable.unpaged())
                .stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Reconciliation record not found"));

        var mismatches =
                invoiceService.getMismatches(email, record.getTaxPeriod(), type);
        return ResponseEntity.ok(ApiResponse.success("Mismatches retrieved", mismatches));
    }

    @PostMapping("/{id}/resolve/{mismatchId}")
    public ResponseEntity<ApiResponse<Object>> resolveMismatch(
            @PathVariable UUID id,
            @PathVariable String mismatchId,
            @RequestBody String note,
            Authentication authentication) {

        String email = authentication.getName();
        ReconciliationRecord record = invoiceService.getReconciliationRecords(email, Pageable.unpaged())
                .stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Reconciliation record not found"));

        var resolved =
                invoiceService.resolveMismatch(record.getTaxPeriod(), mismatchId, note, email);
        return ResponseEntity.ok(ApiResponse.success("Mismatch resolved", resolved));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportReconciliation(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "xlsx") String format,
            Authentication authentication) {

        String email = authentication.getName();
        ReconciliationRecord record = invoiceService.getReconciliationRecords(email, Pageable.unpaged())
                .stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Reconciliation record not found"));

        ReconciliationResponse response =
                invoiceService.getReconciliation(email, record.getTaxPeriod());

        ByteArrayInputStream excelStream = excelExporter.exportReconciliation(response);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reconciliation-" + id + ".xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelStream.readAllBytes());
    }
}