package com.gstcompliance.pipeline;

import com.gstcompliance.dto.response.ComplianceBriefResponse;
import com.gstcompliance.dto.response.InvoiceParseResponse;
import com.gstcompliance.dto.response.ReconciliationResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class PipelineState {

    // User identification - separated concerns
    private UUID userId;
    private UUID invoiceId;
    private String userEmail;          // Email used for service lookups
    private String period;             // Format: MM-YYYY
    private String correlationId;      // For distributed tracing

    // Agent outputs - Strongly typed
    private List<InvoiceParseResponse> parsedInvoices;
    private List<Map<String, Object>> classificationResults;  // Will become HsnClassificationResponse later
    private ReconciliationResponse reconciliationResult;      // ✅ Fixed: Strongly typed
    private List<Map<String, Object>> deadlines;              // Will become FilingDeadlineResponse later
    private ComplianceBriefResponse returnDraft;

    // Status
    private PipelineStatus status;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    @Builder.Default
    private boolean allAgentsSuccessful = true;

    public enum PipelineStatus {
        PENDING,
        PARSING,
        CLASSIFYING,
        RECONCILING,
        DEADLINE_TRACKING,
        DRAFTING,
        COMPLETED,
        FAILED
    }
}