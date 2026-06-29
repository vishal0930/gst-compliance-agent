package com.gstcompliance.agent;

import com.gstcompliance.agent.base.BaseAgent;
import com.gstcompliance.dto.request.ReconcileRequest;
import com.gstcompliance.dto.response.ReconciliationResponse;
import com.gstcompliance.model.Invoice;
import com.gstcompliance.model.LineItem;
import com.gstcompliance.service.GstrPortalService;
import com.gstcompliance.service.InvoiceService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GSTR-2B Reconciliation Agent
 *
 * Compares book invoices against GSTR-2B portal data and generates:
 * - Matched invoices
 * - Mismatches (Amount, GST, Date, HSN, Line Items)
 * - Missing invoices (in books or portal)
 * - ITC at risk calculation
 * - AI-generated summary
 */
@Slf4j
@Component
public class Gstr2bReconcilerAgent extends BaseAgent<ReconcileRequest, ReconciliationResponse> {

    // Thresholds for mismatch detection
    private static final BigDecimal AMOUNT_MISMATCH_THRESHOLD_PERCENT = new BigDecimal("1.0");
    private static final BigDecimal GST_MISMATCH_THRESHOLD            = new BigDecimal("0.01");
    private static final BigDecimal UNIT_PRICE_MISMATCH_THRESHOLD     = new BigDecimal("0.01");
    private static final BigDecimal QTY_MISMATCH_THRESHOLD            = new BigDecimal("0.001");
    private static final int        ROUNDING_SCALE                    = 4;

    /**
     * Ordered list of date formats the portal may return.
     * ISO (yyyy-MM-dd) is tried first; fallback formats follow.
     */
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                     // 2026-01-30
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),            // 30-01-2026
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),            // 30/01/2026
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),            // 01/30/2026
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),           // 30-Jan-2026
            DateTimeFormatter.ofPattern("dd MMM yyyy"),           // 30 Jan 2026
            DateTimeFormatter.ofPattern("d-M-yyyy"),              // 3-1-2026
            DateTimeFormatter.ofPattern("d/M/yyyy")               // 3/1/2026
    );

    private final GstrPortalService  gstrPortalService;
    private final InvoiceService     invoiceService;
    private final ChatLanguageModel  llmModel;

    public Gstr2bReconcilerAgent(
            GstrPortalService gstrPortalService,
            InvoiceService invoiceService,
            @Qualifier("invoiceParserModel") ChatLanguageModel llmModel) {
        super("GSTR2BReconciler");
        this.gstrPortalService = gstrPortalService;
        this.invoiceService    = invoiceService;
        this.llmModel          = llmModel;
    }

    // ----------------------------------------------------------------
    // Entry point
    // ----------------------------------------------------------------

    @Override
    protected ReconciliationResponse process(ReconcileRequest request) throws Exception {
        log.info("Starting reconciliation for period: {}-{}", request.getMonth(), request.getYear());

        String email = request.getUserEmail();
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("User email is required");
        }

        List<Invoice> bookInvoices = invoiceService.getInvoicesForPeriod(
                email, request.getMonth(), request.getYear());
        log.info("Found {} invoices in books", bookInvoices.size());

        List<Map<String, Object>> portalInvoices = gstrPortalService.fetchGstr2b(
                email, request.getMonth(), request.getYear());
        log.info("Found {} invoices in GSTR-2B", portalInvoices.size());

        ReconciliationResponse response = performReconciliation(bookInvoices, portalInvoices);

        if (!response.getMismatches().isEmpty()) {
            response.setSummary(generateSummary(response));
        }

        log.info("Reconciliation complete: {} matched, {} mismatches, ITC at risk: ₹{}",
                response.getMatchedCount(), response.getMismatchCount(), response.getItcAtRisk());

        return response;
    }

    // ----------------------------------------------------------------
    // Core reconciliation
    // ----------------------------------------------------------------

    private ReconciliationResponse performReconciliation(List<Invoice> bookInvoices,
                                                         List<Map<String, Object>> portalInvoices) {
        ReconciliationResponse response = ReconciliationResponse.builder()
                .totalInvoices(bookInvoices.size())
                .matchedCount(0)
                .mismatchCount(0)
                .itcAtRisk(BigDecimal.ZERO)
                .mismatches(new ArrayList<>())
                .build();

        Map<String, Invoice>             bookMap   = buildBookMap(bookInvoices);
        Map<String, Map<String, Object>> portalMap = buildPortalMap(portalInvoices);

        processPortalInvoices(portalInvoices, bookMap, response);
        processMissingInPortal(bookInvoices, portalMap, response);

        return response;
    }

    private Map<String, Invoice> buildBookMap(List<Invoice> bookInvoices) {
        Map<String, Invoice> map = new HashMap<>();
        for (Invoice inv : bookInvoices) {
            map.put(buildInvoiceKey(inv.getInvoiceNumber(), inv.getVendorGstin()), inv);
        }
        return map;
    }

    private Map<String, Map<String, Object>> buildPortalMap(List<Map<String, Object>> portalInvoices) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> portalInv : portalInvoices) {
            String key = buildInvoiceKey(
                    String.valueOf(portalInv.get("invoiceNumber")),
                    String.valueOf(portalInv.get("supplierGstin")));
            map.put(key, portalInv);
        }
        return map;
    }

    // ----------------------------------------------------------------
    // FIX 1: matchedCount only increments when there are ZERO mismatches
    // ----------------------------------------------------------------

    private void processPortalInvoices(List<Map<String, Object>> portalInvoices,
                                       Map<String, Invoice> bookMap,
                                       ReconciliationResponse response) {
        for (Map<String, Object> portalInv : portalInvoices) {
            String invoiceNumber = String.valueOf(portalInv.get("invoiceNumber"));
            String supplierGstin = String.valueOf(portalInv.get("supplierGstin"));
            String key           = buildInvoiceKey(invoiceNumber, supplierGstin);

            Invoice bookInv = bookMap.get(key);

            if (bookInv == null) {
                handleMissingInBooks(portalInv, response);
            } else {
                List<ReconciliationResponse.MismatchDTO> mismatches =
                        compareInvoiceDetailsAll(bookInv, portalInv);

                if (mismatches.isEmpty()) {
                    // Truly matched — no discrepancy of any kind
                    response.setMatchedCount(response.getMatchedCount() + 1);
                } else {
                    // Invoice exists in both systems but has differences — it is NOT matched
                    response.getMismatches().addAll(mismatches);
                    response.setMismatchCount(response.getMismatchCount() + 1);

                    BigDecimal totalRisk = mismatches.stream()
                            .map(ReconciliationResponse.MismatchDTO::getRiskAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    response.setItcAtRisk(response.getItcAtRisk().add(totalRisk));
                }
            }
        }
    }

    private void handleMissingInBooks(Map<String, Object> portalInv, ReconciliationResponse response) {
        String     invoiceNumber = String.valueOf(portalInv.get("invoiceNumber"));
        String     supplierGstin = String.valueOf(portalInv.get("supplierGstin"));
        BigDecimal portalAmount  = getBigDecimalValue(portalInv.get("taxableValue"));
        BigDecimal totalTax      = getBigDecimalValue(portalInv.get("cgst"))
                .add(getBigDecimalValue(portalInv.get("sgst")))
                .add(getBigDecimalValue(portalInv.get("igst")));

        response.getMismatches().add(
                ReconciliationResponse.MismatchDTO.builder()
                        .status("MISSING_IN_ERP")
                        .invoiceNumber(invoiceNumber)
                        .supplierGstin(supplierGstin)
                        .portalAmount(portalAmount)
                        .portalInvoiceDate(formatLocalDate(parseDate(portalInv.get("invoiceDate"))))
                        .riskAmount(totalTax)
                        .description("Invoice found in GSTR-2B but not recorded in your books")
                        .recommendation("Add this invoice to your books or verify with supplier")
                        .build());
        response.setMismatchCount(response.getMismatchCount() + 1);
        response.setItcAtRisk(response.getItcAtRisk().add(totalTax));
    }

    private void processMissingInPortal(List<Invoice> bookInvoices,
                                        Map<String, Map<String, Object>> portalMap,
                                        ReconciliationResponse response) {
        for (Invoice bookInv : bookInvoices) {
            String key = buildInvoiceKey(bookInv.getInvoiceNumber(), bookInv.getVendorGstin());
            if (!portalMap.containsKey(key)) {
                BigDecimal totalTax = bookInv.getTotalGst() != null
                        ? bookInv.getTotalGst() : BigDecimal.ZERO;

                response.getMismatches().add(
                        ReconciliationResponse.MismatchDTO.builder()
                                .status("MISSING_IN_GSTR2B")
                                .invoiceNumber(bookInv.getInvoiceNumber())
                                .supplierGstin(bookInv.getVendorGstin())
                                .bookAmount(bookInv.getTotalAmount())
                                .bookInvoiceDate(formatLocalDate(bookInv.getInvoiceDate()))
                                .riskAmount(totalTax)
                                .description("Invoice recorded in books but not found in GSTR-2B")
                                .recommendation("Check if supplier has filed their GST returns")
                                .build());
                response.setMismatchCount(response.getMismatchCount() + 1);
                response.setItcAtRisk(response.getItcAtRisk().add(totalTax));
            }
        }
    }

    // ----------------------------------------------------------------
    // Per-invoice comparison — returns ALL mismatches
    // ----------------------------------------------------------------

    private List<ReconciliationResponse.MismatchDTO> compareInvoiceDetailsAll(
            Invoice bookInv, Map<String, Object> portalInv) {

        List<ReconciliationResponse.MismatchDTO> mismatches = new ArrayList<>();

        String     invoiceNumber = bookInv.getInvoiceNumber();
        String     supplierGstin = bookInv.getVendorGstin();
        BigDecimal bookAmount    = bookInv.getTotalAmount();
        BigDecimal portalAmount  = getBigDecimalValue(portalInv.get("taxableValue"));
        LocalDate  bookDate      = bookInv.getInvoiceDate();
        LocalDate  portalDate    = parseDate(portalInv.get("invoiceDate"));

        checkAmountMismatch(mismatches, invoiceNumber, supplierGstin,
                bookAmount, portalAmount, bookDate, portalDate);

        checkGstMismatch(mismatches, invoiceNumber, supplierGstin,
                bookInv, portalInv, bookAmount, portalAmount, bookDate, portalDate);

        checkDateMismatch(mismatches, invoiceNumber, supplierGstin,
                bookAmount, portalAmount, bookDate, portalDate);

        checkLineItemMismatches(mismatches, invoiceNumber, supplierGstin,
                bookInv, portalInv, bookAmount, portalAmount, bookDate, portalDate);

        return mismatches;
    }

    // ----------------------------------------------------------------
    // Amount check
    // ----------------------------------------------------------------

    private void checkAmountMismatch(List<ReconciliationResponse.MismatchDTO> mismatches,
                                     String invoiceNumber, String supplierGstin,
                                     BigDecimal bookAmount, BigDecimal portalAmount,
                                     LocalDate bookDate, LocalDate portalDate) {

        if (portalAmount.compareTo(BigDecimal.ZERO) == 0) {
            mismatches.add(buildAmountMismatch(invoiceNumber, supplierGstin,
                    bookAmount, portalAmount, bookDate, portalDate,
                    "Portal amount is zero while book has value",
                    "Verify invoice amount and check if supplier filed correct returns",
                    bookAmount));
            return;
        }

        if (bookAmount.compareTo(BigDecimal.ZERO) == 0) {
            mismatches.add(buildAmountMismatch(invoiceNumber, supplierGstin,
                    bookAmount, portalAmount, bookDate, portalDate,
                    "Book amount is zero while portal has value",
                    "Verify invoice amount and update books",
                    portalAmount));
            return;
        }

        BigDecimal diff        = bookAmount.subtract(portalAmount).abs();
        BigDecimal diffPercent = diff.divide(portalAmount, ROUNDING_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        if (diffPercent.compareTo(AMOUNT_MISMATCH_THRESHOLD_PERCENT) > 0) {
            mismatches.add(buildAmountMismatch(invoiceNumber, supplierGstin,
                    bookAmount, portalAmount, bookDate, portalDate,
                    String.format("Amount mismatch: Book ₹%.2f vs Portal ₹%.2f (%.2f%% diff)",
                            bookAmount, portalAmount, diffPercent),
                    "Verify the invoice amount with supplier",
                    diff));
        }
    }

    private ReconciliationResponse.MismatchDTO buildAmountMismatch(
            String invoiceNumber, String supplierGstin,
            BigDecimal bookAmount, BigDecimal portalAmount,
            LocalDate bookDate, LocalDate portalDate,
            String description, String recommendation, BigDecimal riskAmount) {

        return ReconciliationResponse.MismatchDTO.builder()
                .status("AMOUNT_MISMATCH")
                .invoiceNumber(invoiceNumber)
                .supplierGstin(supplierGstin)
                .bookAmount(bookAmount)
                .portalAmount(portalAmount)
                .diffAmount(bookAmount.subtract(portalAmount).abs())
                .bookInvoiceDate(formatLocalDate(bookDate))
                .portalInvoiceDate(formatLocalDate(portalDate))
                .riskAmount(riskAmount)
                .description(description)
                .recommendation(recommendation)
                .build();
    }

    // ----------------------------------------------------------------
    // GST check
    // ----------------------------------------------------------------

    private void checkGstMismatch(List<ReconciliationResponse.MismatchDTO> mismatches,
                                  String invoiceNumber, String supplierGstin,
                                  Invoice bookInv, Map<String, Object> portalInv,
                                  BigDecimal bookAmount, BigDecimal portalAmount,
                                  LocalDate bookDate, LocalDate portalDate) {

        BigDecimal bookTotalGst = bookInv.getTotalGst();
        if (bookTotalGst == null) {
            log.info("Skipping GST comparison for invoice {}: GST not extracted", invoiceNumber);
            return;
        }

        BigDecimal portalTotalGst = getBigDecimalValue(portalInv.get("cgst"))
                .add(getBigDecimalValue(portalInv.get("sgst")))
                .add(getBigDecimalValue(portalInv.get("igst")));

        BigDecimal difference = bookTotalGst.subtract(portalTotalGst).abs();

        if (difference.compareTo(GST_MISMATCH_THRESHOLD) > 0) {
            mismatches.add(ReconciliationResponse.MismatchDTO.builder()
                    .status("GST_MISMATCH")
                    .invoiceNumber(invoiceNumber)
                    .supplierGstin(supplierGstin)
                    .bookAmount(bookTotalGst)
                    .portalAmount(portalTotalGst)
                    .diffAmount(difference)
                    .bookInvoiceDate(formatLocalDate(bookDate))
                    .portalInvoiceDate(formatLocalDate(portalDate))
                    .riskAmount(difference)
                    .description(String.format("GST mismatch: Books ₹%.2f vs Portal ₹%.2f",
                            bookTotalGst, portalTotalGst))
                    .recommendation("Verify GST calculation and supplier filing.")
                    .build());
        }
    }

    // ----------------------------------------------------------------
    // Date check
    // ----------------------------------------------------------------

    private void checkDateMismatch(List<ReconciliationResponse.MismatchDTO> mismatches,
                                   String invoiceNumber, String supplierGstin,
                                   BigDecimal bookAmount, BigDecimal portalAmount,
                                   LocalDate bookDate, LocalDate portalDate) {

        if (bookDate != null && portalDate != null && !bookDate.equals(portalDate)) {
            mismatches.add(ReconciliationResponse.MismatchDTO.builder()
                    .status("DATE_MISMATCH")
                    .invoiceNumber(invoiceNumber)
                    .supplierGstin(supplierGstin)
                    .bookAmount(bookAmount)
                    .portalAmount(portalAmount)
                    .bookInvoiceDate(formatLocalDate(bookDate))
                    .portalInvoiceDate(formatLocalDate(portalDate))
                    .description(String.format("Date mismatch: Book %s vs Portal %s", bookDate, portalDate))
                    .recommendation("Verify invoice date with supplier")
                    .build());
        }
    }

    // ----------------------------------------------------------------
    // FIX 2: HSN uses frequency map (not Set) to catch duplicate mismatches
    // FIX 3: Line items now compared by description, quantity, and unit price
    // ----------------------------------------------------------------

    private void checkLineItemMismatches(List<ReconciliationResponse.MismatchDTO> mismatches,
                                         String invoiceNumber, String supplierGstin,
                                         Invoice bookInv, Map<String, Object> portalInv,
                                         BigDecimal bookAmount, BigDecimal portalAmount,
                                         LocalDate bookDate, LocalDate portalDate) {

        List<LineItem>           bookLineItems   = bookInv.getLineItems();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> portalLineItems =
                (List<Map<String, Object>>) portalInv.get("lineItems");

        if (bookLineItems == null || bookLineItems.isEmpty()
                || portalLineItems == null || portalLineItems.isEmpty()) {
            return;
        }

        // --- Count mismatch ---
        if (bookLineItems.size() != portalLineItems.size()) {
            mismatches.add(ReconciliationResponse.MismatchDTO.builder()
                    .status("LINE_ITEM_MISMATCH")
                    .invoiceNumber(invoiceNumber)
                    .supplierGstin(supplierGstin)
                    .bookAmount(bookAmount)
                    .portalAmount(portalAmount)
                    .lineItemCount(bookLineItems.size())
                    .bookInvoiceDate(formatLocalDate(bookDate))
                    .portalInvoiceDate(formatLocalDate(portalDate))
                    .description(String.format("Line item count mismatch: Book %d vs Portal %d",
                            bookLineItems.size(), portalLineItems.size()))
                    .recommendation("Verify line items with supplier")
                    .build());
            // Still continue to compare HSN — partial comparison is better than none
        }

        // FIX 2: HSN frequency map instead of Set
        // Example: Book has [5209, 5209, 6301], Portal has [5209, 6301]
        // Set would show {5209,6301} == {5209,6301} — wrong, count differs
        // Frequency map correctly detects the discrepancy
        Map<String, Long> bookHsnFreq   = buildHsnFrequencyMap(bookLineItems);
        Map<String, Long> portalHsnFreq = buildPortalHsnFrequencyMap(portalLineItems);

        if (!bookHsnFreq.equals(portalHsnFreq)) {
            mismatches.add(ReconciliationResponse.MismatchDTO.builder()
                    .status("HSN_MISMATCH")
                    .invoiceNumber(invoiceNumber)
                    .supplierGstin(supplierGstin)
                    .bookAmount(bookAmount)
                    .portalAmount(portalAmount)
                    .bookInvoiceDate(formatLocalDate(bookDate))
                    .portalInvoiceDate(formatLocalDate(portalDate))
                    .description(String.format("HSN code mismatch: Book %s vs Portal %s",
                            bookHsnFreq, portalHsnFreq))
                    .recommendation("Verify HSN codes with supplier")
                    .build());
        }

        // FIX 3: Compare line items by description + quantity + unit price
        // Match portal items against book items positionally (same index).
        // If sizes differ we only compare the overlapping range; count mismatch
        // is already reported above.
        int compareCount = Math.min(bookLineItems.size(), portalLineItems.size());
        for (int i = 0; i < compareCount; i++) {
            LineItem           bookItem   = bookLineItems.get(i);
            Map<String, Object> portalItem = portalLineItems.get(i);

            checkLineItemDetail(mismatches, invoiceNumber, supplierGstin,
                    bookAmount, portalAmount, bookDate, portalDate,
                    bookItem, portalItem, i);
        }
    }

    /**
     * Compares a single book line item against its portal counterpart.
     * Checks: description similarity, quantity delta, unit price delta.
     */
    private void checkLineItemDetail(List<ReconciliationResponse.MismatchDTO> mismatches,
                                     String invoiceNumber, String supplierGstin,
                                     BigDecimal bookAmount, BigDecimal portalAmount,
                                     LocalDate bookDate, LocalDate portalDate,
                                     LineItem bookItem, Map<String, Object> portalItem,
                                     int index) {

        // Description — case-insensitive, trimmed
        String bookDesc   = bookItem.getDescription() != null ? bookItem.getDescription().trim() : "";
        String portalDesc = portalItem.get("description") != null
                ? String.valueOf(portalItem.get("description")).trim() : "";

        if (!bookDesc.equalsIgnoreCase(portalDesc) && !bookDesc.isEmpty() && !portalDesc.isEmpty()) {
            mismatches.add(ReconciliationResponse.MismatchDTO.builder()
                    .status("LINE_ITEM_DESCRIPTION_MISMATCH")
                    .invoiceNumber(invoiceNumber)
                    .supplierGstin(supplierGstin)
                    .bookAmount(bookAmount)
                    .portalAmount(portalAmount)
                    .bookInvoiceDate(formatLocalDate(bookDate))
                    .portalInvoiceDate(formatLocalDate(portalDate))
                    .description(String.format(
                            "Line item %d description mismatch: Book \"%s\" vs Portal \"%s\"",
                            index + 1, bookDesc, portalDesc))
                    .recommendation("Verify item descriptions with supplier")
                    .build());
        }

        // Quantity
        BigDecimal bookQty   = bookItem.getQuantity();
        BigDecimal portalQty = getBigDecimalValue(portalItem.get("quantity"));

        if (bookQty != null && portalQty != null
                && bookQty.subtract(portalQty).abs().compareTo(QTY_MISMATCH_THRESHOLD) > 0) {
            mismatches.add(ReconciliationResponse.MismatchDTO.builder()
                    .status("LINE_ITEM_QUANTITY_MISMATCH")
                    .invoiceNumber(invoiceNumber)
                    .supplierGstin(supplierGstin)
                    .bookAmount(bookAmount)
                    .portalAmount(portalAmount)
                    .bookInvoiceDate(formatLocalDate(bookDate))
                    .portalInvoiceDate(formatLocalDate(portalDate))
                    .description(String.format(
                            "Line item %d quantity mismatch: Book %s vs Portal %s",
                            index + 1, bookQty.toPlainString(), portalQty.toPlainString()))
                    .recommendation("Verify quantity with supplier")
                    .build());
        }

        // Unit price
        BigDecimal bookPrice   = bookItem.getUnitPrice();
        BigDecimal portalPrice = getBigDecimalValue(portalItem.get("unitPrice"));

        if (bookPrice != null && portalPrice != null
                && bookPrice.subtract(portalPrice).abs().compareTo(UNIT_PRICE_MISMATCH_THRESHOLD) > 0) {
            mismatches.add(ReconciliationResponse.MismatchDTO.builder()
                    .status("LINE_ITEM_PRICE_MISMATCH")
                    .invoiceNumber(invoiceNumber)
                    .supplierGstin(supplierGstin)
                    .bookAmount(bookAmount)
                    .portalAmount(portalAmount)
                    .bookInvoiceDate(formatLocalDate(bookDate))
                    .portalInvoiceDate(formatLocalDate(portalDate))
                    .description(String.format(
                            "Line item %d unit price mismatch: Book ₹%s vs Portal ₹%s",
                            index + 1, bookPrice.toPlainString(), portalPrice.toPlainString()))
                    .recommendation("Verify unit price with supplier")
                    .build());
        }
    }

    // ----------------------------------------------------------------
    // FIX 2: Frequency maps for HSN comparison
    // ----------------------------------------------------------------

    /** Returns {hsnCode -> count} for book line items. */
    private Map<String, Long> buildHsnFrequencyMap(List<LineItem> lineItems) {
        return lineItems.stream()
                .map(LineItem::getHsnCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(h -> !h.isEmpty())
                .collect(Collectors.groupingBy(h -> h, Collectors.counting()));
    }

    /** Returns {hsnCode -> count} for portal line items. */
    private Map<String, Long> buildPortalHsnFrequencyMap(List<Map<String, Object>> lineItems) {
        return lineItems.stream()
                .map(item -> String.valueOf(item.get("hsnCode")))
                .filter(h -> h != null && !h.isEmpty() && !"null".equals(h))
                .map(String::trim)
                .collect(Collectors.groupingBy(h -> h, Collectors.counting()));
    }

    // ----------------------------------------------------------------
    // FIX 4: Multi-format date parser
    // ----------------------------------------------------------------

    /**
     * Parses a date value that may arrive as LocalDate, ISO string,
     * or any of the formats listed in DATE_FORMATTERS.
     * Returns null (with a warn log) rather than throwing.
     */
    private LocalDate parseDate(Object value) {
        if (value == null) return null;

        if (value instanceof LocalDate ld) return ld;

        if (value instanceof String raw) {
            String s = raw.trim();
            if (s.isEmpty()) return null;

            for (DateTimeFormatter fmt : DATE_FORMATTERS) {
                try {
                    return LocalDate.parse(s, fmt);
                } catch (DateTimeParseException ignored) {
                    // try next formatter
                }
            }
            log.warn("Could not parse date '{}' with any known format", s);
            return null;
        }

        log.warn("Unsupported date type: {}", value.getClass().getName());
        return null;
    }

    // ----------------------------------------------------------------
    // Utility helpers
    // ----------------------------------------------------------------

    private String buildInvoiceKey(String invoiceNumber, String supplierGstin) {
        String inv  = invoiceNumber != null ? invoiceNumber.trim().toUpperCase() : "";
        String gstn = supplierGstin  != null ? supplierGstin.trim().toUpperCase()  : "";
        return inv + "|" + gstn;
    }

    private String formatLocalDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    private BigDecimal getBigDecimalValue(Object value) {
        if (value == null)                  return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Double d)      return BigDecimal.valueOf(d);
        if (value instanceof Integer i)     return BigDecimal.valueOf(i);
        if (value instanceof Long l)        return BigDecimal.valueOf(l);
        if (value instanceof String s) {
            try   { return new BigDecimal(s); }
            catch (NumberFormatException e) {
                log.warn("Failed to parse '{}' as BigDecimal", s);
                return BigDecimal.ZERO;
            }
        }
        log.warn("Unsupported type for BigDecimal conversion: {}", value.getClass().getName());
        return BigDecimal.ZERO;
    }

    private String generateSummary(ReconciliationResponse response) {
        String prompt = String.format("""
                You are a GST reconciliation expert. Create a simple summary for a business owner.

                MISMATCHES FOUND: %d
                ITC AT RISK: ₹%.2f

                Create a 2-3 sentence summary explaining the issues and recommending next steps.
                """, response.getMismatchCount(), response.getItcAtRisk());
        return llmModel.generate(prompt);
    }
}