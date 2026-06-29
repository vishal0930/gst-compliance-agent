package com.gstcompliance.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gstcompliance.agent.*;
import com.gstcompliance.agent.base.AgentResult;
import com.gstcompliance.dto.request.ReconcileRequest;
import com.gstcompliance.dto.response.ComplianceBriefResponse;
import com.gstcompliance.dto.response.InvoiceParseResponse;
import com.gstcompliance.dto.response.ReconciliationResponse;
import com.gstcompliance.model.Invoice;
import com.gstcompliance.model.LineItem;
import com.gstcompliance.model.ReconciliationRecord;
import com.gstcompliance.repository.InvoiceRepository;
import com.gstcompliance.repository.LineItemRepository;
import com.gstcompliance.repository.ReconciliationRepository;
import com.gstcompliance.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentPipelineService {

    private final InvoiceParserAgent invoiceParserAgent;
    private final HsnClassifierAgent hsnClassifierAgent;
    private final Gstr2bReconcilerAgent reconcilerAgent;
    private final DeadlineTrackerAgent deadlineTrackerAgent;
    private final ReturnDrafterAgent returnDrafterAgent;
    private final InvoiceRepository invoiceRepository;
    private final LineItemRepository lineItemRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ObjectMapper objectMapper;

    public AgentPipelineService(
            InvoiceParserAgent invoiceParserAgent,
            HsnClassifierAgent hsnClassifierAgent,
            @Lazy Gstr2bReconcilerAgent reconcilerAgent,
            DeadlineTrackerAgent deadlineTrackerAgent,
            ReturnDrafterAgent returnDrafterAgent,
            InvoiceRepository invoiceRepository,
            LineItemRepository lineItemRepository,
            ReconciliationRepository reconciliationRepository,
            ObjectMapper objectMapper) {
        this.invoiceParserAgent = invoiceParserAgent;
        this.hsnClassifierAgent = hsnClassifierAgent;
        this.reconcilerAgent = reconcilerAgent;
        this.deadlineTrackerAgent = deadlineTrackerAgent;
        this.returnDrafterAgent = returnDrafterAgent;
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public CompletableFuture<PipelineState> runPipeline(
            UUID userId,
            String userEmail,
            String period,
            List<String> fileKeys,
            UUID invoiceId) {

        String correlationId = UUID.randomUUID().toString();
        log.info("Starting pipeline with correlationId: {}", correlationId);

        PipelineState state = PipelineState.builder()
                .userId(userId)
                .invoiceId(invoiceId)
                .userEmail(userEmail)
                .period(period)
                .correlationId(correlationId)
                .status(PipelineState.PipelineStatus.PENDING)
                .startedAt(LocalDateTime.now())
                .errors(new ArrayList<>())
                .build();

        try {
            // ----------------------------------------------------------------
            // Step 1: Parse invoices
            // ----------------------------------------------------------------
            state.setStatus(PipelineState.PipelineStatus.PARSING);
            List<InvoiceParseResponse> parsedInvoices = parseInvoices(fileKeys);
            state.setParsedInvoices(parsedInvoices);
            log.info("Parsed {} invoices", parsedInvoices.size());

            // Persist parsed header data (vendorName, invoiceNumber, dates, amounts)
            // so the DB row is clean before we write line items next.
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

            if (!parsedInvoices.isEmpty()) {
                InvoiceParseResponse parsed = parsedInvoices.get(0);

                invoice.setVendorName(parsed.getVendorName() != null ? parsed.getVendorName() : "UNKNOWN");
                invoice.setVendorGstin(parsed.getVendorGstin() != null ? parsed.getVendorGstin() : "UNKNOWN");
                invoice.setInvoiceNumber(parsed.getInvoiceNumber() != null ? parsed.getInvoiceNumber() : "UNKNOWN");

                if (parsed.getInvoiceDate() != null && !parsed.getInvoiceDate().isBlank()) {
                    invoice.setInvoiceDate(DateUtils.parseDate(parsed.getInvoiceDate()));

                    // Correct the pipeline period to match the actual invoice date
                    LocalDate parsedDate = invoice.getInvoiceDate();
                    String correctedPeriod = String.format(
                            "%02d-%04d",
                            parsedDate.getMonthValue(),
                            parsedDate.getYear()
                    );
                    state.setPeriod(correctedPeriod);
                    log.info("Pipeline period updated to {}", correctedPeriod);
                }

                invoice.setTotalAmount(parsed.getTotalAmount() != null ? parsed.getTotalAmount() : BigDecimal.ZERO);
                invoice.setTotalGst(parsed.getTotalGst() != null ? parsed.getTotalGst() : BigDecimal.ZERO);
                invoice.setConfidenceScore(parsed.getConfidenceScore() != null ? parsed.getConfidenceScore() : BigDecimal.ZERO);
                invoice.setParseStatus(Invoice.ParseStatus.DONE.name());

                invoiceRepository.save(invoice);
                log.info("Invoice header updated: {}", invoice.getId());
            }

            // ----------------------------------------------------------------
            // Step 2: Classify HSN codes
            // ----------------------------------------------------------------
            state.setStatus(PipelineState.PipelineStatus.CLASSIFYING);
            List<Map<String, Object>> classifiedItems = classifyItems(parsedInvoices);
            state.setClassificationResults(classifiedItems);
            log.info("Classified {} items", classifiedItems.size());

            // ----------------------------------------------------------------
            // Step 2.5: Persist line items BEFORE reconciliation
            //
            // Gstr2bReconcilerAgent reads bookInv.getLineItems() from the DB.
            // If we persist here, the reconciler will see the correct line items.
            // ----------------------------------------------------------------
            persistLineItems(invoice, state);

            // ----------------------------------------------------------------
            // Step 3: Reconcile
            // ----------------------------------------------------------------
            state.setStatus(PipelineState.PipelineStatus.RECONCILING);

            // Re-read invoice so we have the latest invoiceDate (set in Step 1)
            invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

            LocalDate invoiceDate = invoice.getInvoiceDate();

            ReconcileRequest reconcileRequest = new ReconcileRequest();
            reconcileRequest.setMonth(invoiceDate.getMonthValue());
            reconcileRequest.setYear(invoiceDate.getYear());
            reconcileRequest.setUserEmail(state.getUserEmail());

            ReconciliationResponse reconciliationResponse = reconcile(reconcileRequest);

            if (reconciliationResponse != null) {
                ReconciliationRecord record = reconciliationRepository
                        .findByUserIdAndTaxPeriod(userId, state.getPeriod())
                        .orElse(
                                ReconciliationRecord.builder()
                                        .user(invoice.getUser())
                                        .taxPeriod(state.getPeriod())
                                        .startedAt(LocalDateTime.now())
                                        .build()
                        );

                record.setStatus(ReconciliationRecord.Status.DONE);
                record.setCompletedAt(LocalDateTime.now());
                record.setTotalInvoices(reconciliationResponse.getTotalInvoices());
                record.setMatchedCount(reconciliationResponse.getMatchedCount());
                record.setMismatchCount(reconciliationResponse.getMismatchCount());
                record.setItcAtRisk(reconciliationResponse.getItcAtRisk());
                record.setReportJson(objectMapper.writeValueAsString(reconciliationResponse));

                reconciliationRepository.save(record);
            }

            state.setReconciliationResult(reconciliationResponse);
            log.info("Reconciliation complete");

            // ----------------------------------------------------------------
            // Step 4: Track deadlines
            // ----------------------------------------------------------------
            state.setStatus(PipelineState.PipelineStatus.DEADLINE_TRACKING);
            List<Map<String, Object>> deadlines = trackDeadlines(
                    state.getUserEmail(),
                    invoiceDate.getMonthValue(),
                    invoiceDate.getYear()
            );
            state.setDeadlines(deadlines);
            log.info("Found {} deadlines", deadlines.size());

            // ----------------------------------------------------------------
            // Step 5: Draft return
            // ----------------------------------------------------------------
            state.setStatus(PipelineState.PipelineStatus.DRAFTING);
            ComplianceBriefResponse draft = draftReturn(state);
            state.setReturnDraft(draft);
            log.info("Return draft generated");

            state.setStatus(PipelineState.PipelineStatus.COMPLETED);
            state.setCompletedAt(LocalDateTime.now());
            state.setAllAgentsSuccessful(true);

        } catch (Exception e) {
            log.error("Pipeline failed: {}", e.getMessage(), e);
            state.setStatus(PipelineState.PipelineStatus.FAILED);
            state.getErrors().add(e.getMessage());
            state.setAllAgentsSuccessful(false);
        }

        return CompletableFuture.completedFuture(state);
    }

    /**
     * Persists line items for the invoice using parsed + classified data already in PipelineState.
     *
     * Called between Step 2 (classification) and Step 3 (reconciliation) so that
     * Gstr2bReconcilerAgent can read bookInv.getLineItems() from the database.
     *
     * After runPipeline() returns, InvoiceService.processInvoiceAsync() calls its own
     * processLineItems() which will overwrite these rows. That is safe: the second write
     * is identical in data because it uses the same PipelineState. If you want to avoid
     * the duplicate write, remove the processLineItems() call from InvoiceService — but
     * do NOT do that until the end-to-end flow is confirmed working.
     */
    private void persistLineItems(Invoice invoice, PipelineState state) {
        if (state.getParsedInvoices() == null || state.getParsedInvoices().isEmpty()) {
            log.warn("persistLineItems: no parsed invoices in state for invoice {}", invoice.getId());
            return;
        }

        InvoiceParseResponse parsedInvoice = state.getParsedInvoices().get(0);
        List<InvoiceParseResponse.LineItemDTO> parsedLineItems = parsedInvoice.getLineItems();

        if (parsedLineItems == null || parsedLineItems.isEmpty()) {
            log.warn("persistLineItems: no line items in parsed invoice {}", invoice.getId());
            return;
        }

        List<Map<String, Object>> classificationResults = state.getClassificationResults();
        if (classificationResults == null || classificationResults.isEmpty()) {
            log.warn("persistLineItems: no classification results for invoice {}", invoice.getId());
            return;
        }

        // Delete any stale line items from a previous failed attempt
        List<LineItem> existing = lineItemRepository.findByInvoiceId(invoice.getId());
        if (!existing.isEmpty()) {
            lineItemRepository.deleteAll(existing);
            lineItemRepository.flush();
            log.info("persistLineItems: deleted {} stale line items for invoice {}",
                    existing.size(), invoice.getId());
        }

        String supplierGstin = invoice.getVendorGstin();
        String buyerGstin    = invoice.getUser().getGstin();
        boolean transactionTypeUnknown =
                supplierGstin == null || supplierGstin.length() < 2 ||
                        buyerGstin    == null || buyerGstin.length()    < 2;
        boolean isInterState = !transactionTypeUnknown && !isSameState(supplierGstin, buyerGstin);

        int count = Math.min(parsedLineItems.size(), classificationResults.size());
        List<LineItem> lineItems = new ArrayList<>();
        BigDecimal totalGst = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            InvoiceParseResponse.LineItemDTO parsedItem  = parsedLineItems.get(i);
            Map<String, Object>              classified  = classificationResults.get(i);

            LineItem lineItem = new LineItem();
            lineItem.setInvoice(invoice);
            lineItem.setDescription(parsedItem.getDescription());
            lineItem.setQuantity(parsedItem.getQuantity());
            lineItem.setUnitPrice(parsedItem.getUnitPrice());

            // Taxable value
            BigDecimal taxableValue = parsedItem.getTaxableValue();
            if (taxableValue == null && parsedItem.getQuantity() != null && parsedItem.getUnitPrice() != null) {
                taxableValue = parsedItem.getQuantity()
                        .multiply(parsedItem.getUnitPrice())
                        .setScale(2, RoundingMode.HALF_UP);
            }
            if (taxableValue == null) taxableValue = BigDecimal.ZERO;
            lineItem.setTaxableValue(taxableValue);

            // HSN + rates from classification
            lineItem.setHsnCode((String) classified.get("hsnCode"));
            BigDecimal cgstRate = toBigDecimal(classified.get("cgstRate"));
            BigDecimal sgstRate = toBigDecimal(classified.get("sgstRate"));
            BigDecimal igstRate = toBigDecimal(classified.get("igstRate"));

            // Tax amounts
            BigDecimal cgst = BigDecimal.ZERO;
            BigDecimal sgst = BigDecimal.ZERO;
            BigDecimal igst = BigDecimal.ZERO;

            if (isInterState) {
                if (igstRate != null)
                    igst = taxableValue.multiply(igstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                if (cgstRate != null)
                    cgst = taxableValue.multiply(cgstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (sgstRate != null)
                    sgst = taxableValue.multiply(sgstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }

            lineItem.setCgstAmount(cgst);
            lineItem.setSgstAmount(sgst);
            lineItem.setIgstAmount(igst);

            // Confidence + review flag (mirrors InvoiceService logic)
            BigDecimal confidence = toBigDecimal(classified.getOrDefault("confidence", BigDecimal.valueOf(0.90)));
            lineItem.setHsnConfidence(confidence);

            boolean needsReview = classified.get("hsnCode") == null
                    || (confidence != null && confidence.compareTo(BigDecimal.valueOf(0.70)) < 0)
                    || transactionTypeUnknown;
            lineItem.setNeedsReview(needsReview);

            if (needsReview) {
                List<String> reasons = new ArrayList<>();
                if (classified.get("hsnCode") == null) reasons.add("HSN_UNKNOWN");
                if (confidence != null && confidence.compareTo(BigDecimal.valueOf(0.70)) < 0) reasons.add("OCR_LOW_CONFIDENCE");
                if (transactionTypeUnknown) reasons.add("GSTIN_INVALID");
                lineItem.setReviewReason(String.join(",", reasons));
            }

            lineItems.add(lineItem);
            totalGst = totalGst.add(cgst).add(sgst).add(igst);
        }

        lineItemRepository.saveAll(lineItems);
        lineItemRepository.flush();

        // Update invoice totalGst so reconciler reads correct value
        invoice.setTotalGst(totalGst);
        invoiceRepository.save(invoice);
        invoiceRepository.flush();

        log.info("persistLineItems: saved {} line items for invoice {}, totalGst={}",
                lineItems.size(), invoice.getId(), totalGst);
    }

    // ----------------------------------------------------------------
    // Private agent-call helpers (unchanged)
    // ----------------------------------------------------------------

    private List<InvoiceParseResponse> parseInvoices(List<String> fileKeys) {
        List<InvoiceParseResponse> results = new ArrayList<>();
        for (String fileKey : fileKeys) {
            AgentResult<InvoiceParseResponse> result = invoiceParserAgent.execute(fileKey);
            if (result.isSuccess()) {
                results.add(result.getData());
            } else {
                log.error("Failed to parse invoice: {}", result.getErrorMessage());
            }
        }
        return results;
    }

    private List<Map<String, Object>> classifyItems(List<InvoiceParseResponse> invoices) {
        List<InvoiceParseResponse.LineItemDTO> items = new ArrayList<>();
        for (InvoiceParseResponse invoice : invoices) {
            if (invoice.getLineItems() != null) {
                items.addAll(invoice.getLineItems());
            }
        }
        AgentResult<List<Map<String, Object>>> result = hsnClassifierAgent.execute(items);
        return result.isSuccess() ? result.getData() : new ArrayList<>();
    }

    private ReconciliationResponse reconcile(ReconcileRequest request) {
        AgentResult<ReconciliationResponse> result = reconcilerAgent.execute(request);
        return result.isSuccess() ? result.getData() : null;
    }

    private List<Map<String, Object>> trackDeadlines(String userId, int month, int year) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("month", month);
        request.put("year", year);
        AgentResult<List<Map<String, Object>>> result = deadlineTrackerAgent.execute(request);
        return result.isSuccess() ? result.getData() : new ArrayList<>();
    }

    private ComplianceBriefResponse draftReturn(PipelineState state) {
        AgentResult<ComplianceBriefResponse> result = returnDrafterAgent.execute(state);
        return result.isSuccess() ? result.getData() : null;
    }

    // ----------------------------------------------------------------
    // Utility helpers (mirrors InvoiceService — no shared state needed)
    // ----------------------------------------------------------------

    private boolean isSameState(String supplierGstin, String buyerGstin) {
        return supplierGstin.substring(0, 2).equals(buyerGstin.substring(0, 2));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (value instanceof String s) {
            s = s.trim();
            if (s.isBlank()) return null;
            try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}