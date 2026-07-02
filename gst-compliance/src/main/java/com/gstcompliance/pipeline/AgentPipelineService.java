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

                // Log parsed GST amounts for debugging
                log.info("Parsed invoice GST amounts: taxableValue={}, cgst={}, sgst={}, igst={}, totalGst={}",
                        parsed.getTaxableValue(), parsed.getCgst(), parsed.getSgst(), parsed.getIgst(), parsed.getTotalGst());

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
            
            // Log classification results for debugging
            if (!classifiedItems.isEmpty()) {
                for (int i = 0; i < Math.min(3, classifiedItems.size()); i++) {
                    Map<String, Object> item = classifiedItems.get(i);
                    log.info("Classification result[{}]: hsnCode={}, gstRate={}, cgstRate={}, sgstRate={}, igstRate={}, confidence={}",
                            i, item.get("hsnCode"), item.get("gstRate"), item.get("cgstRate"), 
                            item.get("sgstRate"), item.get("igstRate"), item.get("confidence"));
                }
            } else {
                log.warn("No classification results returned - will use LLM GST values as fallback");
            }

            // ----------------------------------------------------------------
            // Step 2.5: Persist line items
            // ----------------------------------------------------------------
            persistLineItems(invoice, state);

            // Re-read invoice so we have the latest invoiceDate (set in Step 1)
            invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

            LocalDate invoiceDate = invoice.getInvoiceDate();

            // ----------------------------------------------------------------
            // Step 3: Reconcile - DISABLED
            // Reconciliation now only runs when explicitly triggered from reconciliation page
            // ----------------------------------------------------------------
            log.info("Skipping automatic reconciliation - will run when triggered from reconciliation page");

            // ----------------------------------------------------------------
            // Step 4: Track deadlines
            // ----------------------------------------------------------------
            try {
                state.setStatus(PipelineState.PipelineStatus.DEADLINE_TRACKING);
                List<Map<String, Object>> deadlines = trackDeadlines(
                        state.getUserEmail(),
                        invoiceDate.getMonthValue(),
                        invoiceDate.getYear()
                );
                state.setDeadlines(deadlines);
                log.info("Found {} deadlines", deadlines.size());
            } catch (Exception e) {
                log.warn("Deadline tracking skipped for invoice {}: {}", invoiceId, e.getMessage(), e);
                state.getErrors().add("Deadline tracking skipped: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // Step 5: Draft return
            // ----------------------------------------------------------------
            try {
                state.setStatus(PipelineState.PipelineStatus.DRAFTING);
                ComplianceBriefResponse draft = draftReturn(state);
                state.setReturnDraft(draft);
                log.info("Return draft generated");
            } catch (Exception e) {
                log.warn("Return draft skipped for invoice {}: {}", invoiceId, e.getMessage(), e);
                state.getErrors().add("Return draft skipped: " + e.getMessage());
            }

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
        boolean hasClassification = classificationResults != null && !classificationResults.isEmpty();
        
        if (!hasClassification) {
            log.warn("persistLineItems: no classification results for invoice {}, using LLM GST values as fallback", invoice.getId());
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

        // Use parsed line items count, fall back to classification results if available
        int count = hasClassification ? Math.min(parsedLineItems.size(), classificationResults.size()) : parsedLineItems.size();
        List<LineItem> lineItems = new ArrayList<>();
        BigDecimal totalGst = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            InvoiceParseResponse.LineItemDTO parsedItem = parsedLineItems.get(i);
            Map<String, Object> classified = hasClassification ? classificationResults.get(i) : null;

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

            // HSN + rates from classification or LLM fallback
            BigDecimal cgst = BigDecimal.ZERO;
            BigDecimal sgst = BigDecimal.ZERO;
            BigDecimal igst = BigDecimal.ZERO;
            BigDecimal cgstRate = null;
            BigDecimal sgstRate = null;
            BigDecimal igstRate = null;
            BigDecimal gstRate = null;

            boolean hasValidClassification =
                    hasClassification
                            && classified != null
                            && classified.get("hsnCode") != null
                            && (
                            classified.get("gstRate") != null
                                    || classified.get("cgstRate") != null
                                    || classified.get("sgstRate") != null
                                    || classified.get("igstRate") != null
                    );

            if (hasValidClassification) {
                lineItem.setHsnCode((String) classified.get("hsnCode"));
                cgstRate = toBigDecimal(classified.get("cgstRate"));
                sgstRate = toBigDecimal(classified.get("sgstRate"));
                igstRate = toBigDecimal(classified.get("igstRate"));
                gstRate = toBigDecimal(classified.get("gstRate"));

                // Calculate tax amounts from rates using transaction type.
                if (!transactionTypeUnknown && isInterState) {
                    BigDecimal effectiveIgstRate = igstRate != null ? igstRate : gstRate;
                    if (effectiveIgstRate != null) {
                        igst = taxableValue
                                .multiply(effectiveIgstRate)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    }
                } else {
                    BigDecimal effectiveCgstRate = cgstRate;
                    BigDecimal effectiveSgstRate = sgstRate;

                    if ((effectiveCgstRate == null || effectiveSgstRate == null) && gstRate != null) {
                        BigDecimal halfRate = gstRate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                        effectiveCgstRate = effectiveCgstRate != null ? effectiveCgstRate : halfRate;
                        effectiveSgstRate = effectiveSgstRate != null ? effectiveSgstRate : halfRate;
                    }

                    if (effectiveCgstRate != null) {
                        cgst = taxableValue
                                .multiply(effectiveCgstRate)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    }
                    if (effectiveSgstRate != null) {
                        sgst = taxableValue
                                .multiply(effectiveSgstRate)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    }
                }
            } else {
                // Fallback: Use LLM-parsed GST amounts directly from invoice level
                // Distribute invoice-level GST proportionally across line items
                BigDecimal totalInvoiceTaxable = parsedInvoice.getTaxableValue();

                if (totalInvoiceTaxable == null
                        || totalInvoiceTaxable.compareTo(BigDecimal.ZERO) <= 0) {

                    totalInvoiceTaxable = parsedLineItems.stream()
                            .map(InvoiceParseResponse.LineItemDTO::getTaxableValue)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    log.warn(
                            "Invoice taxableValue missing. Calculated from line items = {}",
                            totalInvoiceTaxable
                    );
                }
                log.info("Fallback mode: totalInvoiceTaxable={}, taxableValue={}", totalInvoiceTaxable, taxableValue);

                if (totalInvoiceTaxable != null && totalInvoiceTaxable.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal proportion = taxableValue.divide(totalInvoiceTaxable, 4, RoundingMode.HALF_UP);
                    log.info("Fallback: proportion={}", proportion);

                    if (parsedInvoice.getCgst() != null) {
                        cgst = parsedInvoice.getCgst().multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                        log.info("Fallback: cgst calculated from invoice-level cgst={}", cgst);
                    }
                    if (parsedInvoice.getSgst() != null) {
                        sgst = parsedInvoice.getSgst().multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                        log.info("Fallback: sgst calculated from invoice-level sgst={}", sgst);
                    }
                    if (parsedInvoice.getIgst() != null) {
                        igst = parsedInvoice.getIgst().multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                        log.info("Fallback: igst calculated from invoice-level igst={}", igst);
                    }

                    // Calculate rate from the distributed amount
                    if (taxableValue.compareTo(BigDecimal.ZERO) > 0) {

                        if (cgst.compareTo(BigDecimal.ZERO) > 0
                                || sgst.compareTo(BigDecimal.ZERO) > 0) {

                            BigDecimal totalCgstSgst = cgst.add(sgst);

                            gstRate = totalCgstSgst
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(taxableValue, 2, RoundingMode.HALF_UP);

                            log.info("Fallback: gstRate calculated from cgst+sgst={}", gstRate);

                        } else if (igst.compareTo(BigDecimal.ZERO) > 0) {

                            gstRate = igst
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(taxableValue, 2, RoundingMode.HALF_UP);

                            log.info("Fallback: gstRate calculated from igst={}", gstRate);
                        }

                    } else {

                        log.warn(
                                "Fallback: taxableValue is zero. Unable to derive GST rate."
                        );
                    }
                } else {
                    log.warn("Fallback: totalInvoiceTaxable is null or zero, cannot distribute GST. Using invoice-level GST directly.");

                    cgst = parsedInvoice.getCgst() == null
                            ? BigDecimal.ZERO
                            : parsedInvoice.getCgst();

                    sgst = parsedInvoice.getSgst() == null
                            ? BigDecimal.ZERO
                            : parsedInvoice.getSgst();

                    igst = parsedInvoice.getIgst() == null
                            ? BigDecimal.ZERO
                            : parsedInvoice.getIgst();
                }
                
                // Try to use line item level GST amounts if available from LLM
                if (parsedItem.getCgstAmount() != null) {
                    cgst = parsedItem.getCgstAmount();
                    log.info("Fallback: using line item cgstAmount={}", cgst);
                }
                if (parsedItem.getSgstAmount() != null) {
                    sgst = parsedItem.getSgstAmount();
                    log.info("Fallback: using line item sgstAmount={}", sgst);
                }
                if (parsedItem.getIgstAmount() != null) {
                    igst = parsedItem.getIgstAmount();
                    log.info("Fallback: using line item igstAmount={}", igst);
                }
                if (parsedItem.getGstRate() != null) {
                    gstRate = parsedItem.getGstRate();
                    log.info("Fallback: using line item gstRate={}", gstRate);
                }
                if (parsedItem.getHsnCode() != null && !parsedItem.getHsnCode().isBlank()) {
                    lineItem.setHsnCode(parsedItem.getHsnCode());
                    log.info("Fallback: using line item hsnCode={}", parsedItem.getHsnCode());
                } else {
                    lineItem.setHsnCode(null);
                }
            }
            if (gstRate == null
                    && taxableValue.compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal totalTax = cgst.add(sgst).add(igst);

                if (totalTax.compareTo(BigDecimal.ZERO) > 0) {

                    gstRate = totalTax
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    taxableValue,
                                    2,
                                    RoundingMode.HALF_UP);

                    log.info(
                            "Derived GST rate from tax amounts = {}",
                            gstRate);
                }
            }

            lineItem.setGstRate(gstRate);

            lineItem.setCgstAmount(cgst);
            lineItem.setSgstAmount(sgst);
            lineItem.setIgstAmount(igst);
            
            // Log warning if GST amounts are zero
            if (cgst.compareTo(BigDecimal.ZERO) == 0 && sgst.compareTo(BigDecimal.ZERO) == 0 && igst.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("LineItem[{}] has ZERO GST amounts - description={}, taxableValue={}, hasClassification={}, gstRate={}",
                        i, parsedItem.getDescription(), taxableValue, hasClassification, gstRate);
            }
            if (!hasValidClassification) {
                log.warn(
                        "Classification incomplete for '{}'. Falling back to parser GST values.",
                        parsedItem.getDescription()
                );
            }
            log.debug("LineItem[{}] taxable={} cgst={} sgst={} igst={} interState={} typeUnknown={}",
                    parsedItem.getDescription(), taxableValue, cgst, sgst, igst, isInterState, transactionTypeUnknown);

            // Confidence + review flag (mirrors InvoiceService logic)
            BigDecimal confidence = hasClassification && classified != null 
                    ? toBigDecimal(classified.getOrDefault("confidence", BigDecimal.valueOf(0.90)))
                    : BigDecimal.valueOf(0.90);
            lineItem.setHsnConfidence(confidence);

            boolean needsReview =
                    !hasValidClassification
                            || (confidence != null
                            && confidence.compareTo(BigDecimal.valueOf(0.70)) < 0)
                            || transactionTypeUnknown;
            lineItem.setNeedsReview(needsReview);

            if (needsReview) {
                List<String> reasons = new ArrayList<>();
                if (!hasClassification || classified == null || classified.get("hsnCode") == null) reasons.add("HSN_UNKNOWN");
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
                throw new RuntimeException("Failed to parse invoice: " + result.getErrorMessage());
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
