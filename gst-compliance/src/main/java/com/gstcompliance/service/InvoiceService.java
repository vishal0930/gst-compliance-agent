package com.gstcompliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gstcompliance.dto.response.ComplianceBriefResponse;
import com.gstcompliance.dto.response.InvoiceResponse;
import com.gstcompliance.dto.response.ReconciliationResponse;
import com.gstcompliance.exception.ResourceNotFoundException;
import com.gstcompliance.model.*;
import com.gstcompliance.repository.*;
import com.gstcompliance.pipeline.AgentPipelineService;
import com.gstcompliance.pipeline.PipelineState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ComplianceBriefRepository complianceBriefRepository;
    private final FileStorageService fileStorageService;
    private final AgentPipelineService pipelineService;
    private final NotificationService notificationService;
    private final LineItemRepository lineItemRepository;
    private final ObjectMapper objectMapper;

    /**
     * Review reasons for line items that need manual review.
     * These provide clear context to the frontend about why an item was flagged.
     */
    public enum ReviewReason {
        HSN_UNKNOWN("HSN code could not be determined"),
        GST_UNKNOWN("GST rate could not be determined"),
        GSTIN_INVALID("Invalid or missing GSTIN for transaction type determination"),
        RATE_MISSING("Tax rates missing from classification"),
        OCR_LOW_CONFIDENCE("OCR confidence below threshold"),
        AMOUNT_MISMATCH("Amount mismatch with portal"),
        HSN_MISMATCH("HSN mismatch with portal"),
        GST_MISMATCH("GST mismatch with portal"),
        DATE_MISMATCH("Date mismatch with portal"),
        LINE_ITEM_DIFFERENCE("Line item difference with portal");

        private final String description;

        ReviewReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Transactional
    public String processInvoice(MultipartFile file, String email) {
        User user = getUserByEmail(email);
        String fileKey = fileStorageService.uploadFile(file);

        // Use unique identifiers to avoid constraint violations during processing
        String pendingPrefix = UUID.randomUUID().toString().substring(0, 8);

        Invoice invoice = Invoice.builder()
                .user(user)
                .vendorName("PENDING_" + pendingPrefix)
                .vendorGstin("PENDING_" + pendingPrefix)
                .invoiceNumber("PENDING_" + pendingPrefix)
                .invoiceDate(LocalDate.now())
                .totalAmount(BigDecimal.ZERO)
                .totalGst(BigDecimal.ZERO)
                .fileKey(fileKey)
                .parseStatus(Invoice.ParseStatus.PENDING.name())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created with ID: {}", saved.getId());

        processInvoiceAsync(saved.getId(), email);

        return saved.getId().toString();
    }

    @Async
    public void processInvoiceAsync(UUID invoiceId, String email) {
        try {
            log.info("Processing invoice async: {}", invoiceId);

            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

            invoice.setParseStatus(Invoice.ParseStatus.PROCESSING.name());
            invoiceRepository.save(invoice);

            List<String> fileKeys = List.of(invoice.getFileKey());

            LocalDate invoiceDate = invoice.getInvoiceDate() != null ?
                    invoice.getInvoiceDate() : LocalDate.now();

            String period = String.format("%02d-%04d",
                    invoiceDate.getMonthValue(),
                    invoiceDate.getYear());

            CompletableFuture<PipelineState> future = pipelineService.runPipeline(
                    invoice.getUser().getId(),
                    invoice.getUser().getEmail(),
                    period,
                    fileKeys,
                    invoiceId
            );
            PipelineState state = future.get();

            if (state.isAllAgentsSuccessful()) {
                // AgentPipelineService.runPipeline() has already updated header fields,
                // persisted line items, and run reconciliation. Re-fetching ensures we see the
                // latest totalGst written during that processing.
                invoice = invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
                invoice.setParseStatus(Invoice.ParseStatus.DONE.name());
                invoiceRepository.save(invoice);

                log.info("Invoice {} processing complete. Status=DONE", invoiceId);
            } else {
                invoice.setParseStatus(Invoice.ParseStatus.FAILED.name());
                invoiceRepository.save(invoice);
                log.error("Invoice processing failed: {}", state.getErrors());
            }

        } catch (Exception e) {
            log.error("Async invoice processing failed for invoice {}: {}", invoiceId, e.getMessage(), e);
            try {
                Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
                if (invoice != null) {
                    invoice.setParseStatus(Invoice.ParseStatus.FAILED.name());
                    invoiceRepository.save(invoice);
                    log.info("Invoice {} marked as FAILED after processing error", invoiceId);
                }
            } catch (Exception ignored) {
                log.error("Failed to update invoice {} status to FAILED", invoiceId, ignored);
            }
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        if (value instanceof String s) {
            s = s.trim();
            if (s.isBlank()) {
                return null;
            }
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                log.warn("Unable to convert '{}' to BigDecimal", s);
                return null;
            }
        }

        log.warn("Unsupported numeric value: {}", value);
        return null;
    }
    public Page<Invoice> getInvoices(
            String email,
            int month,
            int year,
            Pageable pageable
    ) {
        User user = getUserByEmail(email);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return invoiceRepository.findByUserIdAndInvoiceDateBetween(
                user.getId(),
                startDate,
                endDate,
                pageable
        );
    }
    public Invoice getInvoice(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invoice not found: " + id));
    }
    @Transactional
    public InvoiceResponse updateInvoiceAndLineItems(UUID id, InvoiceResponse dto, String email) {
        User user = getUserByEmail(email);
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Invoice not found");
        }

        existing.setVendorName(dto.getVendorName());
        existing.setVendorGstin(dto.getVendorGstin());
        existing.setInvoiceNumber(dto.getInvoiceNumber());
        existing.setInvoiceDate(dto.getInvoiceDate());
        existing.setTotalAmount(dto.getTotalAmount());
        existing.setTotalGst(dto.getTotalGst());
        existing.setParseStatus(Invoice.ParseStatus.DONE.name());

        if (dto.getLineItems() != null) {
            for (InvoiceResponse.LineItemResponse itemDto : dto.getLineItems()) {
                LineItem li;
                if (itemDto.getId() != null) {
                    li = lineItemRepository.findById(itemDto.getId())
                            .orElseGet(() -> {
                                LineItem newItem = new LineItem();
                                newItem.setInvoice(existing);
                                return newItem;
                            });
                } else {
                    li = new LineItem();
                    li.setInvoice(existing);
                }

                li.setDescription(itemDto.getDescription());
                li.setQuantity(itemDto.getQuantity());
                li.setUnitPrice(itemDto.getUnitPrice());
                li.setHsnCode(itemDto.getHsnCode());
                li.setGstRate(itemDto.getGstRate());
                li.setTaxableValue(itemDto.getTaxableValue());
                li.setCgstAmount(itemDto.getCgstAmount());
                li.setSgstAmount(itemDto.getSgstAmount());
                li.setIgstAmount(itemDto.getIgstAmount());
                li.setHsnConfidence(itemDto.getHsnConfidence());
                li.setNeedsReview(Boolean.TRUE.equals(itemDto.getNeedsReview()));
                li.setReviewReason(itemDto.getReviewReason());

                lineItemRepository.save(li);
            }
        }

        Invoice saved = invoiceRepository.save(existing);
        return toResponse(saved);
    }

    @Transactional
    public void deleteInvoice(UUID id, String email) {
        User user = getUserByEmail(email);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Invoice not found");
        }

        invoiceRepository.delete(invoice);
    }

    public List<Invoice> getInvoicesForPeriod(
            String email,
            int month,
            int year
    ) {

        User user = getUserByEmail(email);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return invoiceRepository
                .findByUserIdAndInvoiceDateBetween(
                        user.getId(),
                        start,
                        end,
                        Pageable.unpaged()
                )
                .getContent();
    }

    /**
     * Gets reconciliation for a specific period.
     * This reads from the ReconciliationRecord created by ReconciliationAgent.
     */
    public ReconciliationResponse getReconciliation(String email, String period) {
        User user = getUserByEmail(email);

        Optional<ReconciliationRecord> recordOpt = reconciliationRepository
                .findByUserIdAndTaxPeriod(user.getId(), period);

        if (recordOpt.isEmpty()) {
            log.info("No reconciliation record found for user {} period {}", user.getEmail(), period);
            return ReconciliationResponse.builder()
                    .period(period)
                    .totalInvoices(0)
                    .matchedCount(0)
                    .mismatchCount(0)
                    .itcAtRisk(BigDecimal.ZERO)
                    .mismatches(new ArrayList<>())
                    .summary("Reconciliation not yet performed")
                    .build();
        }

        ReconciliationRecord record = recordOpt.get();
        log.debug("Retrieved reconciliation for user {} period {}: matched={}, mismatched={}",
                user.getEmail(), period, record.getMatchedCount(), record.getMismatchCount());

        ReconciliationResponse report = getReconciliationResponse(record);

        return ReconciliationResponse.builder()
                .period(record.getTaxPeriod())
                .totalInvoices(record.getTotalInvoices())
                .matchedCount(record.getMatchedCount())
                .mismatchCount(record.getMismatchCount())
                .itcAtRisk(record.getItcAtRisk())
                .mismatches(report.getMismatches())
                .summary(report.getSummary())
                .build();
    }

    /**
     * Gets mismatches for a specific period, optionally filtered by type.
     * This reads from the ReconciliationRecord created by ReconciliationAgent.
     */
    public Object getMismatches(String email, String period, String type) {
        User user = getUserByEmail(email);

        Optional<ReconciliationRecord> recordOpt = reconciliationRepository
                .findByUserIdAndTaxPeriod(user.getId(), period);

        if (recordOpt.isEmpty()) {
            log.info("No reconciliation record found for user {} period {}", user.getEmail(), period);
            return new ArrayList<>();
        }

        ReconciliationRecord record = recordOpt.get();
        ReconciliationResponse report = getReconciliationResponse(record);

        List<ReconciliationResponse.MismatchDTO> mismatches = report.getMismatches();

        if (mismatches == null || mismatches.isEmpty()) {
            return new ArrayList<>();
        }

        if (type == null || type.isEmpty() || "all".equalsIgnoreCase(type)) {
            return mismatches;
        }

        // Filter by mismatch status field (backend uses "status", not "type")
        return mismatches.stream()
                .filter(m -> type.equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Resolves a specific mismatch for a period.
     * Updates the ReconciliationRecord with resolution details.
     */
    @Transactional
    public Object resolveMismatch(String period, String mismatchId, String note, String email) {
        User user = getUserByEmail(email);

        Optional<ReconciliationRecord> recordOpt = reconciliationRepository
                .findByUserIdAndTaxPeriod(user.getId(), period);

        if (recordOpt.isEmpty()) {
            log.warn("No reconciliation record found for user {} period {}", user.getEmail(), period);
            Map<String, Object> result = new HashMap<>();
            result.put("resolved", false);
            result.put("message", "Reconciliation record not found");
            return result;
        }

        ReconciliationRecord record = recordOpt.get();
        ReconciliationResponse report = getReconciliationResponse(record);

        List<ReconciliationResponse.MismatchDTO> mismatches = report.getMismatches();

        if (mismatches == null || mismatches.isEmpty()) {
            log.warn("No mismatches found for user {} period {}", user.getEmail(), period);
            Map<String, Object> result = new HashMap<>();
            result.put("resolved", false);
            result.put("message", "No mismatches found");
            return result;
        }

        boolean found = false;
        // Find and update the specific mismatch
        for (ReconciliationResponse.MismatchDTO mismatch : mismatches) {
            // Using invoiceNumber as the identifier
            if (mismatchId.equals(mismatch.getInvoiceNumber())) {
                found = true;
                log.info("Resolved mismatch {} for user {} period {}. Note: {}",
                        mismatchId, user.getEmail(), period, note);
                break;
            }
        }
        if (!found) {
            log.warn("Mismatch {} not found for user {} period {}", mismatchId, user.getEmail(), period);
            Map<String, Object> result = new HashMap<>();
            result.put("resolved", false);
            result.put("message", "Mismatch not found");
            return result;
        }

        try {
            record.setReportJson(objectMapper.writeValueAsString(report));
            reconciliationRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to serialize reconciliation report", e);
            Map<String, Object> result = new HashMap<>();
            result.put("resolved", false);
            result.put("message", "Failed to update reconciliation report");
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("resolved", true);
        result.put("note", note);
        result.put("message", "Mismatch resolved successfully");
        return result;
    }

    public Page<ReconciliationRecord> getReconciliationRecords(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        return reconciliationRepository.findByUserId(user.getId(), pageable);
    }

    public Page<ReconciliationRecord> getReconciliationRecords(String email, Integer month, Integer year, Pageable pageable) {
        if (month == null || year == null) {
            return getReconciliationRecords(email, pageable);
        }
        User user = getUserByEmail(email);
        String taxPeriod = String.format("%02d-%04d", month, year);
        return reconciliationRepository.findByUserIdAndTaxPeriod(user.getId(), taxPeriod, pageable);
    }

    /**
     * Generates a return draft for a specific period.
     * This reads reconciliation data and creates a compliance brief.
     * It does NOT create reconciliation data - that is done by ReconciliationAgent.
     */
    @Async
    public void generateReturnDraftAsync(String email, int month, int year, String jobId) {
        log.info("Generating return draft for: {}, {}-{}, jobId: {}", email, month, year, jobId);

        try {
            User user = getUserByEmail(email);
            String period = String.format("%02d-%04d", month, year);

            // Read reconciliation data from database (created by ReconciliationAgent)
            Optional<ReconciliationRecord> recordOpt = reconciliationRepository
                    .findByUserIdAndTaxPeriod(user.getId(), period);

            ReconciliationRecord reconciliation = recordOpt.orElse(null);

            // Get invoices for the period
            List<Invoice> invoices = getInvoicesForPeriod(email, month, year);

            // Calculate totals from invoices
            BigDecimal totalSales = invoices.stream()
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalGst = invoices.stream()
                    .map(Invoice::getTotalGst)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int totalInvoices = invoices.size();
            int matchedCount = 0;
            int mismatchCount = 0;
            BigDecimal itcAtRisk = BigDecimal.ZERO;

            if (reconciliation != null) {
                matchedCount = reconciliation.getMatchedCount();
                mismatchCount = reconciliation.getMismatchCount();
                itcAtRisk = reconciliation.getItcAtRisk() != null ? reconciliation.getItcAtRisk() : BigDecimal.ZERO;
                log.debug("Using reconciliation data for period {}: matched={}, mismatched={}, itcAtRisk={}",
                        period, matchedCount, mismatchCount, itcAtRisk);
            } else {
                log.info("No reconciliation data found for period {}, using fallback calculation", period);
                // Fallback: calculate from line items
                for (Invoice invoice : invoices) {
                    List<LineItem> lineItems = lineItemRepository.findByInvoiceId(invoice.getId());
                    boolean hasMismatch = false;
                    for (LineItem item : lineItems) {
                        if (Boolean.TRUE.equals(item.getNeedsReview())) {
                            hasMismatch = true;
                            BigDecimal cgst = Optional.ofNullable(item.getCgstAmount()).orElse(BigDecimal.ZERO);
                            BigDecimal sgst = Optional.ofNullable(item.getSgstAmount()).orElse(BigDecimal.ZERO);
                            BigDecimal igst = Optional.ofNullable(item.getIgstAmount()).orElse(BigDecimal.ZERO);
                            itcAtRisk = itcAtRisk.add(cgst).add(sgst).add(igst);
                        }
                    }
                    if (hasMismatch) {
                        mismatchCount++;
                    } else if (!lineItems.isEmpty()) {
                        matchedCount++;
                    }
                }
                log.warn("Fallback calculation for period {}: matched={}, mismatched={}, itcAtRisk={}",
                        period, matchedCount, mismatchCount, itcAtRisk);
            }

            // Calculate eligible ITC from line items (excluding those at risk)
            BigDecimal eligibleItc = BigDecimal.ZERO;
            for (Invoice invoice : invoices) {
                List<LineItem> lineItems = lineItemRepository.findByInvoiceId(invoice.getId());
                for (LineItem item : lineItems) {
                    BigDecimal cgst = Optional.ofNullable(item.getCgstAmount()).orElse(BigDecimal.ZERO);
                    BigDecimal sgst = Optional.ofNullable(item.getSgstAmount()).orElse(BigDecimal.ZERO);
                    BigDecimal igst = Optional.ofNullable(item.getIgstAmount()).orElse(BigDecimal.ZERO);
                    BigDecimal itemGst = cgst.add(sgst).add(igst);
                    if (!Boolean.TRUE.equals(item.getNeedsReview())) {
                        eligibleItc = eligibleItc.add(itemGst);
                    }
                }
            }

            // Create compliance brief
            ComplianceBrief brief = new ComplianceBrief();
            brief.setUser(user);
            brief.setTaxPeriod(period);
            brief.setBriefText(generateBriefText(totalInvoices, totalGst, eligibleItc, itcAtRisk, matchedCount, mismatchCount));
            brief.setTotalSales(totalSales);
            brief.setTotalGst(totalGst);
            brief.setTotalItc(eligibleItc);
            brief.setTaxLiability(BigDecimal.ZERO);
            brief.setItcAtRisk(itcAtRisk);
            brief.setIsComplete(true);
            brief.setGeneratedAt(LocalDateTime.now());

            complianceBriefRepository.save(brief);
            log.info("Compliance brief saved for user {} period {}", user.getEmail(), period);

            // Send notification
            notificationService.sendEmail(
                    email,
                    "Return Draft Ready",
                    String.format("Your GST return draft for %s is ready.\n\n" +
                                    "Total ITC: ₹%.2f\n" +
                                    "ITC at Risk: ₹%.2f\n" +
                                    "Matched Invoices: %d\n" +
                                    "Mismatched Invoices: %d\n\n" +
                                    "Please review before filing.",
                            period, eligibleItc, itcAtRisk, matchedCount, mismatchCount)
            );

        } catch (Exception e) {
            log.error("Return draft generation failed for user {} period {}-{}: {}",
                    email, month, year, e.getMessage(), e);
        }
    }

    private String generateBriefText(int invoiceCount, BigDecimal totalGst, BigDecimal eligibleItc,
                                     BigDecimal itcAtRisk, int matchedCount, int mismatchCount) {
        return String.format(
                "GST Compliance Summary\n" +
                        "========================\n" +
                        "Total Invoices: %d\n" +
                        "Matched Invoices: %d\n" +
                        "Mismatched Invoices: %d\n" +
                        "Total GST (ITC): ₹%.2f\n" +
                        "Eligible ITC: ₹%.2f\n" +
                        "ITC at Risk: ₹%.2f\n" +
                        "========================\n" +
                        "Please review mismatches before filing.",
                invoiceCount, matchedCount, mismatchCount, totalGst, eligibleItc, itcAtRisk
        );
    }

    public Page<ComplianceBrief> getReturnDrafts(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        return complianceBriefRepository.findByUserId(user.getId(), pageable);
    }

    /**
     * Returns a page of ComplianceBriefResponse DTOs (safe for serialisation).
     */
    public Page<ComplianceBriefResponse> getReturnDraftResponses(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        return complianceBriefRepository.findByUserId(user.getId(), pageable)
                .map(this::toBriefResponse);
    }

    private ComplianceBriefResponse toBriefResponse(ComplianceBrief brief) {
        return ComplianceBriefResponse.builder()
                .id(brief.getId())
                .userId(brief.getUser().getEmail())
                .period(brief.getTaxPeriod())
                .brief(brief.getBriefText())
                .totalSales(brief.getTotalSales())
                .totalGst(brief.getTotalGst())
                .totalItc(brief.getTotalItc())
                .taxLiability(brief.getTaxLiability())
                .itcAtRisk(brief.getItcAtRisk())
                .isComplete(Boolean.TRUE.equals(brief.getIsComplete()))
                .isApproved(Boolean.TRUE.equals(brief.getIsApproved()))
                .generatedAt(brief.getGeneratedAt())
                .approvedAt(brief.getApprovedAt())
                .build();
    }

    public ComplianceBriefResponse getReturnDraft(UUID id, String email) {
        ComplianceBrief brief = complianceBriefRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance brief not found: " + id));
        return toBriefResponse(brief);
    }

    public Map<String, Object> getGstr3bDraft(UUID id, String email) {
        ComplianceBrief brief = complianceBriefRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance brief not found: " + id));
        Map<String, Object> draft = new HashMap<>();
        draft.put("id", id);
        draft.put("period", brief.getTaxPeriod());
        draft.put("totalSales", brief.getTotalSales());
        draft.put("totalGst", brief.getTotalGst());
        draft.put("totalItc", brief.getTotalItc());
        draft.put("taxLiability", brief.getTaxLiability());
        return draft;
    }

    @Transactional
    public void approveReturnDraft(UUID id, String email) {
        ComplianceBrief brief = complianceBriefRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance brief not found: " + id));

        brief.setIsApproved(true);
        brief.setApprovedAt(LocalDateTime.now());
        complianceBriefRepository.save(brief);

        log.info("Return draft approved: {}", id);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private ReconciliationResponse getReconciliationResponse(ReconciliationRecord record) {
        try {
            if (record.getReportJson() == null || record.getReportJson().isBlank()) {
                return ReconciliationResponse.builder()
                        .mismatches(new ArrayList<>())
                        .summary("No reconciliation report available")
                        .build();
            }

            return objectMapper.readValue(
                    record.getReportJson(),
                    ReconciliationResponse.class
            );

        } catch (Exception e) {
            log.error("Failed to deserialize reconciliation report", e);
            return ReconciliationResponse.builder()
                    .mismatches(new ArrayList<>())
                    .summary("Failed to load reconciliation report")
                    .build();
        }
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        // Fetch line items explicitly (LAZY relation is not loaded in list queries)
        List<LineItem> items = lineItemRepository.findByInvoiceId(invoice.getId());

        // Build line-item DTOs
        List<InvoiceResponse.LineItemResponse> lineItemResponses = items.stream()
                .map(li -> InvoiceResponse.LineItemResponse.builder()
                        .id(li.getId())
                        .description(li.getDescription())
                        .quantity(li.getQuantity())
                        .unitPrice(li.getUnitPrice())
                        .hsnCode(li.getHsnCode())
                        .gstRate(li.getGstRate())
                        .taxableValue(li.getTaxableValue())
                        .cgstAmount(li.getCgstAmount())
                        .sgstAmount(li.getSgstAmount())
                        .igstAmount(li.getIgstAmount())
                        .hsnConfidence(li.getHsnConfidence())
                        .needsReview(li.getNeedsReview())
                        .reviewReason(li.getReviewReason())
                        .build())
                .collect(Collectors.toList());

        // Aggregate tax amounts from line items for the header display
        BigDecimal cgstTotal = items.stream()
                .map(li -> li.getCgstAmount() != null ? li.getCgstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sgstTotal = items.stream()
                .map(li -> li.getSgstAmount() != null ? li.getSgstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal igstTotal = items.stream()
                .map(li -> li.getIgstAmount() != null ? li.getIgstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxableTotal = items.stream()
                .map(li -> li.getTaxableValue() != null ? li.getTaxableValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fall back to invoice-level totalAmount - totalGst when no line items yet
        if (taxableTotal.compareTo(BigDecimal.ZERO) == 0 && invoice.getTotalAmount() != null) {
            BigDecimal gst = invoice.getTotalGst() != null ? invoice.getTotalGst() : BigDecimal.ZERO;
            taxableTotal = invoice.getTotalAmount().subtract(gst);
        }

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .vendorName(invoice.getVendorName())
                .vendorGstin(invoice.getVendorGstin())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .taxableValue(taxableTotal)
                .totalAmount(invoice.getTotalAmount())
                .totalGst(invoice.getTotalGst())
                .cgstAmount(cgstTotal)
                .sgstAmount(sgstTotal)
                .igstAmount(igstTotal)
                .parseStatus(invoice.getParseStatus())
                .confidenceScore(invoice.getConfidenceScore())
                .createdAt(invoice.getCreatedAt())
                .lineItems(lineItemResponses)
                .build();
    }

    public Page<InvoiceResponse> getInvoiceResponses(String email,int month,int year, Pageable pageable) {
        return getInvoices(email, month, year, pageable)
                .map(this::toResponse);
    }

    public InvoiceResponse getInvoiceResponse(UUID id, String email) {
        User user = getUserByEmail(email);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Invoice not found");
        }

        return toResponse(invoice);
    }
}