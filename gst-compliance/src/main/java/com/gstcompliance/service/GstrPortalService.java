package com.gstcompliance.service;

import com.gstcompliance.dto.response.Gstr2bInvoiceResponse;
import com.gstcompliance.dto.response.Gstr2bSummaryResponse;
import com.gstcompliance.model.Gstr2bImportSession;
import com.gstcompliance.model.Gstr2bInvoice;
import com.gstcompliance.model.Gstr2bLineItem;
import com.gstcompliance.model.User;
import com.gstcompliance.repository.Gstr2bImportSessionRepository;
import com.gstcompliance.repository.Gstr2bInvoiceRepository;
import com.gstcompliance.repository.Gstr2bLineItemRepository;
import com.gstcompliance.repository.UserRepository;
import com.gstcompliance.util.GstinValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GstrPortalService {

    private final Gstr2bInvoiceRepository       gstr2bInvoiceRepository;
    private final Gstr2bLineItemRepository      gstr2bLineItemRepository;
    private final Gstr2bImportSessionRepository importSessionRepository;
    private final UserRepository                userRepository;
    private final GstinValidator                gstinValidator;

    // ─────────────────────────────────────────────────────────────────────
    // 1.  Paged + filtered invoice list  (replaces List<Map>)
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Gstr2bInvoiceResponse> getInvoices(
            String email,
            int month,
            int year,
            String supplierGstin,
            String supplierName,
            String invoiceNumber,
            String importStatus,
            String matchStatus,
            Pageable pageable) {

        UUID userId = getUser(email).getId();
        String taxPeriod = taxPeriod(month, year);

        supplierGstin = blankToNull(supplierGstin);
        supplierName = blankToNull(supplierName);
        invoiceNumber = blankToNull(invoiceNumber);
        importStatus = blankToNull(importStatus);
        matchStatus = blankToNull(matchStatus);

        Page<Gstr2bInvoice> page;

        // If NO filters are supplied, use the simple query
        if (supplierGstin == null &&
                supplierName == null &&
                invoiceNumber == null &&
                importStatus == null &&
                matchStatus == null) {

            page = gstr2bInvoiceRepository.findByUserIdAndTaxPeriod(
                    userId,
                    taxPeriod,
                    pageable);

        } else {

            // Use filtered search only when filters exist
            page = gstr2bInvoiceRepository.findFiltered(
                    userId,
                    taxPeriod,
                    supplierGstin,
                    supplierName,
                    invoiceNumber,
                    importStatus,
                    matchStatus,
                    pageable);
        }

        return page.map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2.  Real summary with supplier count, all tax components
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Gstr2bSummaryResponse getSummary(String email, int month, int year) {

        UUID   userId    = getUser(email).getId();
        String taxPeriod = taxPeriod(month, year);

        List<Gstr2bInvoice> invoices =
                gstr2bInvoiceRepository.findByUserIdAndTaxPeriod(userId, taxPeriod);

        if (invoices.isEmpty()) {
            return Gstr2bSummaryResponse.builder()
                    .taxPeriod(taxPeriod)
                    .invoiceCount(0)
                    .supplierCount(0)
                    .taxableValue(BigDecimal.ZERO)
                    .cgst(BigDecimal.ZERO).sgst(BigDecimal.ZERO).igst(BigDecimal.ZERO)
                    .cess(BigDecimal.ZERO).grandTotal(BigDecimal.ZERO)
                    .totalItc(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal taxable    = BigDecimal.ZERO;
        BigDecimal cgst       = BigDecimal.ZERO;
        BigDecimal sgst       = BigDecimal.ZERO;
        BigDecimal igst       = BigDecimal.ZERO;
        BigDecimal cess       = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Gstr2bInvoice inv : invoices) {
            taxable    = taxable.add(nvl(inv.getTaxableValue()));
            cgst       = cgst.add(nvl(inv.getCgst()));
            sgst       = sgst.add(nvl(inv.getSgst()));
            igst       = igst.add(nvl(inv.getIgst()));
            cess       = cess.add(nvl(inv.getCess()));
            grandTotal = grandTotal.add(nvl(inv.getGrandTotal()));
        }

        long supplierCount = gstr2bInvoiceRepository
                .countDistinctSuppliers(userId, taxPeriod);

        return Gstr2bSummaryResponse.builder()
                .taxPeriod(taxPeriod)
                .invoiceCount(invoices.size())
                .supplierCount((int) supplierCount)
                .taxableValue(taxable)
                .cgst(cgst).sgst(sgst).igst(igst).cess(cess)
                .grandTotal(grandTotal)
                .totalItc(cgst.add(sgst).add(igst))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3.  Import history
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Gstr2bImportSession> getImportHistory(String email, Pageable pageable) {
        UUID userId = getUser(email).getId();
        return importSessionRepository
                .findByUserIdOrderByImportedAtDesc(userId, pageable)
                .getContent();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 4.  Used by Gstr2bReconcilerAgent (returns Map for internal use only)
    //     Once agent is migrated to typed DTOs this method can be removed.
    // ─────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> fetchGstr2b(String email, int month, int year) {
        log.info("fetchGstr2b (internal) — user: {}, period: {}-{}", email, month, year);
        try {
            User   user      = getUser(email);
            String taxPeriod = taxPeriod(month, year);

            List<Gstr2bInvoice> dbInvoices =
                    gstr2bInvoiceRepository.findByUserAndTaxPeriod(user, taxPeriod);

            if (dbInvoices.isEmpty()) {
                // Fallback to invoice-date range so old data still reconciles
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
                dbInvoices = gstr2bInvoiceRepository
                        .findByUserAndInvoiceDateBetween(user, start, end);
            }

            return dbInvoices.stream().map(inv -> {
                Map<String, Object> m = new HashMap<>();
                m.put("supplierName",   inv.getSupplierName());
                m.put("supplierGstin",  inv.getSupplierGstin());
                m.put("buyerGstin",     inv.getBuyerGstin());
                m.put("invoiceNumber",  inv.getInvoiceNumber());
                m.put("invoiceDate",    inv.getInvoiceDate());
                m.put("taxableValue",   nvl(inv.getTaxableValue()));
                m.put("cgst",           nvl(inv.getCgst()));
                m.put("sgst",           nvl(inv.getSgst()));
                m.put("igst",           nvl(inv.getIgst()));
                m.put("grandTotal",     nvl(inv.getGrandTotal()));

                List<Gstr2bLineItem> items =
                        gstr2bLineItemRepository.findByGstr2bInvoiceId(inv.getId());
                List<Map<String, Object>> itemMaps = items.stream().map(li -> {
                    Map<String, Object> im = new HashMap<>();
                    im.put("description", li.getDescription());
                    im.put("quantity",    li.getQuantity());
                    im.put("unitPrice",   li.getUnitPrice());
                    im.put("hsnCode",     li.getHsnCode());
                    im.put("gstRate",     li.getGstRate());
                    im.put("taxableValue",li.getTaxableValue());
                    im.put("cgstAmount",  nvl(li.getCgstAmount()));
                    im.put("sgstAmount",  nvl(li.getSgstAmount()));
                    im.put("igstAmount",  nvl(li.getIgstAmount()));
                    return im;
                }).collect(Collectors.toList());
                m.put("lineItems", itemMaps);
                return m;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("fetchGstr2b failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public boolean validateGstin(String gstin) {
        return gstinValidator.isValid(gstin);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.gstcompliance.exception
                        .ResourceNotFoundException("User not found: " + email));
    }

    private static String taxPeriod(int month, int year) {
        return String.format("%02d-%04d", month, year);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private Gstr2bInvoiceResponse toResponse(Gstr2bInvoice inv) {
        List<Gstr2bLineItem> items =
                gstr2bLineItemRepository.findByGstr2bInvoiceId(inv.getId());

        List<Gstr2bInvoiceResponse.LineItemResponse> lineResponses = items.stream()
                .map(li -> Gstr2bInvoiceResponse.LineItemResponse.builder()
                        .id(li.getId())
                        .description(li.getDescription())
                        .quantity(li.getQuantity())
                        .unitPrice(li.getUnitPrice())
                        .hsnCode(li.getHsnCode())
                        .gstRate(li.getGstRate())
                        .taxableValue(li.getTaxableValue())
                        .cgstAmount(nvl(li.getCgstAmount()))
                        .sgstAmount(nvl(li.getSgstAmount()))
                        .igstAmount(nvl(li.getIgstAmount()))
                        .cess(nvl(li.getCess()))
                        .itcEligible(li.getItcEligible())
                        .reverseCharge(li.getReverseCharge())
                        .build())
                .collect(Collectors.toList());

        return Gstr2bInvoiceResponse.builder()
                .id(inv.getId())
                .taxPeriod(inv.getTaxPeriod())
                .supplierName(inv.getSupplierName())
                .supplierGstin(inv.getSupplierGstin())
                .buyerGstin(inv.getBuyerGstin())
                .invoiceNumber(inv.getInvoiceNumber())
                .invoiceDate(inv.getInvoiceDate())
                .taxableValue(nvl(inv.getTaxableValue()))
                .cgst(nvl(inv.getCgst()))
                .sgst(nvl(inv.getSgst()))
                .igst(nvl(inv.getIgst()))
                .cess(nvl(inv.getCess()))
                .grandTotal(nvl(inv.getGrandTotal()))
                .importStatus(inv.getImportStatus())
                .matchStatus(inv.getMatchStatus())
                .uploadedAt(inv.getUploadedAt())
                .lineItems(lineResponses)
                .build();
    }

    // Needed by fetchGstr2b fallback path
    private List<Gstr2bInvoice> findByUserAndInvoiceDateBetween(
            User user, LocalDate start, LocalDate end) {
        // delegate to repository using the old date-range method still present
        return gstr2bInvoiceRepository
                .findByUserAndInvoiceDateBetween(user, start, end);
    }
}
