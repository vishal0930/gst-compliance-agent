package com.gstcompliance.controller;

import com.gstcompliance.dto.response.ApiResponse;
import com.gstcompliance.dto.response.ComplianceBriefResponse;
import com.gstcompliance.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final InvoiceService invoiceService;

    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<Map<String, Object>>> draftReturn(
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {

        String email = authentication.getName();
        int month = request.getOrDefault("month", LocalDate.now().getMonthValue());
        int year  = request.getOrDefault("year",  LocalDate.now().getYear());

        log.info("Drafting return for user: {}, period: {}-{}", email, month, year);

        // Kick off the async generation
        invoiceService.generateReturnDraftAsync(email, month, year, null);

        Map<String, Object> response = new HashMap<>();
        response.put("period",  String.format("%02d-%04d", month, year));
        response.put("status",  "GENERATING");
        response.put("message", "Return draft is being generated. Check the drafts list shortly.");

        return ResponseEntity.ok(ApiResponse.success("Return draft generation started", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ComplianceBriefResponse>>> getDrafts(
            Authentication authentication,
            Pageable pageable) {

        String email = authentication.getName();
        Page<ComplianceBriefResponse> drafts = invoiceService.getReturnDraftResponses(email, pageable);
        return ResponseEntity.ok(ApiResponse.success("Drafts retrieved", drafts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplianceBriefResponse>> getDraft(
            @PathVariable UUID id,
            Authentication authentication) {

        String email = authentication.getName();
        ComplianceBriefResponse brief = invoiceService.getReturnDraft(id, email);
        return ResponseEntity.ok(ApiResponse.success("Draft retrieved", brief));
    }

    @GetMapping("/{id}/gstr3b")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGstr3bDraft(
            @PathVariable UUID id,
            Authentication authentication) {

        String email = authentication.getName();
        Map<String, Object> gstr3b = invoiceService.getGstr3bDraft(id, email);
        return ResponseEntity.ok(ApiResponse.success("GSTR-3B draft retrieved", gstr3b));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, String>>> approveDraft(
            @PathVariable UUID id,
            Authentication authentication) {

        String email = authentication.getName();
        invoiceService.approveReturnDraft(id, email);

        Map<String, String> response = new HashMap<>();
        response.put("status", "APPROVED");
        response.put("message", "Return draft approved successfully");

        return ResponseEntity.ok(ApiResponse.success("Draft approved", response));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportDraft(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "pdf") String format,
            Authentication authentication) {

        String email = authentication.getName();
        ComplianceBriefResponse brief = invoiceService.getReturnDraft(id, email);

        byte[] content = brief.toString().getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=compliance-brief-" + id + ".json");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }
}