package com.gstcompliance.service;

import com.gstcompliance.dto.request.Gstr2bInvoiceDto;
import com.gstcompliance.dto.request.Gstr2bLineItemDto;
import com.gstcompliance.dto.request.Gstr2bUploadRequest;
import com.gstcompliance.dto.response.Gstr2bUploadResponse;
import com.gstcompliance.exception.ResourceNotFoundException;
import com.gstcompliance.model.*;
import com.gstcompliance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Gstr2bUploadService {

    private final Gstr2bInvoiceRepository     gstr2bInvoiceRepository;
    private final Gstr2bLineItemRepository    gstr2bLineItemRepository;
    private final Gstr2bImportSessionRepository importSessionRepository;
    private final UserRepository              userRepository;

    /**
     * Imports an entire month's GSTR-2B statement.
     *
     * If the period already exists and {@code request.isReplace()} is false,
     * throws {@link IllegalStateException} — the caller (controller) returns 409.
     *
     * If {@code request.isReplace()} is true, existing invoices for that period
     * are deleted before the fresh import (atomic within the transaction).
     */
    @Transactional
    public Gstr2bUploadResponse uploadGstr2bData(Gstr2bUploadRequest request, String email) {

        String taxPeriod = request.getTaxPeriod();
        log.info("GSTR-2B upload — user: {}, period: {}, invoices: {}, replace: {}",
                email, taxPeriod, request.getInvoices().size(), request.isReplace());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // ── Duplicate period guard ─────────────────────────────────────
        boolean periodExists = gstr2bInvoiceRepository
                .existsByUserIdAndTaxPeriod(user.getId(), taxPeriod);

        if (periodExists && !request.isReplace()) {
            throw new IllegalStateException(
                    "GSTR-2B for period " + taxPeriod + " already exists. " +
                    "Set replace=true to overwrite.");
        }

        if (periodExists) {
            log.info("Replacing existing GSTR-2B data for period: {}", taxPeriod);
            gstr2bInvoiceRepository.deleteByUserIdAndTaxPeriod(user.getId(), taxPeriod);
        }

        // ── Process each invoice ───────────────────────────────────────
        List<Gstr2bUploadResponse.InvoiceResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount    = 0;

        // Running totals for the session record
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst    = BigDecimal.ZERO;
        BigDecimal totalSgst    = BigDecimal.ZERO;
        BigDecimal totalIgst    = BigDecimal.ZERO;

        for (Gstr2bInvoiceDto dto : request.getInvoices()) {
            try {
                Gstr2bInvoice saved = saveInvoice(dto, user, taxPeriod);

                totalTaxable = totalTaxable.add(nvl(saved.getTaxableValue()));
                totalCgst    = totalCgst.add(nvl(saved.getCgst()));
                totalSgst    = totalSgst.add(nvl(saved.getSgst()));
                totalIgst    = totalIgst.add(nvl(saved.getIgst()));

                results.add(Gstr2bUploadResponse.InvoiceResult.builder()
                        .invoiceNumber(dto.getInvoiceNumber())
                        .success(true)
                        .message("Imported")
                        .invoiceId(saved.getId())
                        .build());
                successCount++;

            } catch (Exception e) {
                log.error("Failed invoice {}: {}", dto.getInvoiceNumber(), e.getMessage());
                results.add(Gstr2bUploadResponse.InvoiceResult.builder()
                        .invoiceNumber(dto.getInvoiceNumber())
                        .success(false)
                        .message(e.getMessage())
                        .build());
                failCount++;
            }
        }

        BigDecimal totalItc = totalCgst.add(totalSgst).add(totalIgst);

        // ── Save import session ────────────────────────────────────────
        Gstr2bImportSession session = Gstr2bImportSession.builder()
                .user(user)
                .taxPeriod(taxPeriod)
                .totalInvoices(request.getInvoices().size())
                .successful(successCount)
                .failed(failCount)
                .totalTaxable(totalTaxable)
                .totalCgst(totalCgst)
                .totalSgst(totalSgst)
                .totalIgst(totalIgst)
                .totalItc(totalItc)
                .status(failCount == 0 ? "COMPLETED" : "PARTIAL")
                .importedAt(LocalDateTime.now())
                .build();

        importSessionRepository.save(session);

        log.info("GSTR-2B import done — period: {}, ok: {}, failed: {}, ITC: {}",
                taxPeriod, successCount, failCount, totalItc);

        return Gstr2bUploadResponse.builder()
                .totalProcessed(request.getInvoices().size())
                .successfulCount(successCount)
                .failedCount(failCount)
                .taxPeriod(taxPeriod)
                .totalItc(totalItc)
                .results(results)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ── Private helpers ────────────────────────────────────────────────

    private Gstr2bInvoice saveInvoice(Gstr2bInvoiceDto dto, User user, String taxPeriod) {

        Gstr2bInvoice invoice = Gstr2bInvoice.builder()
                .user(user)
                .taxPeriod(taxPeriod)
                .supplierName(dto.getSupplierName())
                .supplierGstin(dto.getSupplierGstin())
                .buyerGstin(dto.getBuyerGstin())
                .invoiceNumber(dto.getInvoiceNumber())
                .invoiceDate(dto.getInvoiceDate())
                .taxableValue(dto.getTaxableValue())
                .cgst(nvl(dto.getCgst()))
                .sgst(nvl(dto.getSgst()))
                .igst(nvl(dto.getIgst()))
                .grandTotal(dto.getGrandTotal())
                .importStatus(Gstr2bInvoice.ImportStatus.IMPORTED.name())
                .matchStatus(Gstr2bInvoice.MatchStatus.NOT_CHECKED.name())
                .build();

        Gstr2bInvoice saved = gstr2bInvoiceRepository.save(invoice);

        // Line items (optional — some JSON exports omit them)
        if (dto.getLineItems() != null && !dto.getLineItems().isEmpty()) {
            List<Gstr2bLineItem> lineItems = dto.getLineItems().stream()
                    .map(item -> buildLineItem(item, saved))
                    .collect(Collectors.toList());
            gstr2bLineItemRepository.saveAll(lineItems);
        }

        return saved;
    }

    private Gstr2bLineItem buildLineItem(Gstr2bLineItemDto dto, Gstr2bInvoice invoice) {
        return Gstr2bLineItem.builder()
                .gstr2bInvoice(invoice)
                .description(dto.getDescription())
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .hsnCode(dto.getHsnCode())
                .gstRate(dto.getGstRate())
                .taxableValue(dto.getTaxableValue())
                .cgstAmount(nvl(dto.getCgstAmount()))
                .sgstAmount(nvl(dto.getSgstAmount()))
                .igstAmount(nvl(dto.getIgstAmount()))
                .cess(nvl(dto.getCessAmount()))
                .itcEligible(dto.getItcEligible() != null ? dto.getItcEligible() : true)
                .reverseCharge(dto.getReverseCharge() != null ? dto.getReverseCharge() : false)
                .build();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
