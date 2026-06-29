package com.gstcompliance.controller;

import com.gstcompliance.dto.response.ApiResponse;
import com.gstcompliance.dto.response.InvoiceResponse;
import com.gstcompliance.model.Invoice;
import com.gstcompliance.model.User;
import com.gstcompliance.pipeline.AgentPipelineService;
import com.gstcompliance.pipeline.PipelineState;
import com.gstcompliance.repository.InvoiceRepository;
import com.gstcompliance.repository.UserRepository;
import com.gstcompliance.service.FileStorageService;
import com.gstcompliance.service.InvoiceService; // Injected missing service import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page; // Added missing Page import
import org.springframework.data.domain.Pageable; // Added missing Pageable import
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final AgentPipelineService pipelineService;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final InvoiceService invoiceService; // Injecting dependency used by trailing endpoints

    // In-memory job tracker for asynchronous pipeline futures
    private final Map<String, CompletableFuture<PipelineState>> jobStatusMap = new HashMap<>();

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String email = authentication.getName();
        String jobId = UUID.randomUUID().toString();

        try {
            // 1. Get user from database
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));

            // 2. Upload file to MinIO/S3
            String fileKey = fileStorageService.uploadFile(file);
            log.info("✅ File uploaded to: {}", fileKey);

            // Generate unique PENDING values to avoid unique constraint violations
            String pendingId = UUID.randomUUID().toString().substring(0, 8);
            String pendingInvoiceNumber = "PENDING_" + pendingId;
            String pendingVendorGstin = "PENDING_" + pendingId;

            // 3. Create and save invoice placeholder to database
            Invoice invoice = new Invoice();
            invoice.setUser(user);
            invoice.setVendorName("PENDING");
            invoice.setVendorGstin(pendingVendorGstin);
            invoice.setInvoiceNumber(pendingInvoiceNumber);
            invoice.setInvoiceDate(LocalDate.now());
            invoice.setTotalAmount(BigDecimal.ZERO);
            invoice.setTotalGst(BigDecimal.ZERO);
            invoice.setFileKey(fileKey);
            invoice.setParseStatus(Invoice.ParseStatus.PENDING.name());
            invoice.setConfidenceScore(BigDecimal.ZERO);

            Invoice savedInvoice = invoiceRepository.save(invoice);
            log.info("✅ Invoice saved to database with ID: {}", savedInvoice.getId());

            // 4. Run multi-agent intelligence pipeline asynchronously
            CompletableFuture<PipelineState> future = pipelineService.runPipeline(
                    savedInvoice.getUser().getId(),
                    savedInvoice.getUser().getEmail(),
                    String.format("%02d-%04d",
                            LocalDate.now().getMonthValue(),
                            LocalDate.now().getYear()),
                    Collections.singletonList(fileKey),
                    savedInvoice.getId()
            );
            jobStatusMap.put(jobId, future);

            Map<String, String> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("invoiceId", savedInvoice.getId().toString());
            response.put("status", "QUEUED");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        CompletableFuture<PipelineState> future = jobStatusMap.get(jobId);

        if (future == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", "NOT_FOUND");
            response.put("message", "Job ID not found. It may have expired.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);

        if (future.isDone()) {
            try {
                PipelineState state = future.get();
                response.put("status", state.getStatus().name());
                response.put("completed", true);
                response.put("allAgentsSuccessful", state.isAllAgentsSuccessful());
                response.put("errors", state.getErrors());
                response.put("result", state);
            } catch (Exception e) {
                response.put("status", "FAILED");
                response.put("error", e.getMessage());
            }
        } else {
            response.put("status", "PROCESSING");
            response.put("completed", false);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoices(
            Authentication authentication,
            Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Invoices retrieved successfully",
                        invoiceService.getInvoiceResponses(
                                authentication.getName(),
                                pageable
                        )
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable UUID id,
            Authentication authentication) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Invoice retrieved successfully",
                        invoiceService.getInvoiceResponse(
                                id,
                                authentication.getName()
                        )
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @PathVariable UUID id,
            Authentication authentication) {

        invoiceService.deleteInvoice(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Invoice deleted successfully",
                        null
                )
        );
    }
}