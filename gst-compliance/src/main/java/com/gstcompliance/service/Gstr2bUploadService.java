package com.gstcompliance.service;

import com.gstcompliance.dto.request.Gstr2bInvoiceDto;
import com.gstcompliance.dto.request.Gstr2bUploadRequest;
import com.gstcompliance.dto.response.Gstr2bUploadResponse;
import com.gstcompliance.model.Gstr2bInvoice;
import com.gstcompliance.model.Gstr2bLineItem;
import com.gstcompliance.model.User;
import com.gstcompliance.exception.DuplicateInvoiceException;
import com.gstcompliance.exception.ResourceNotFoundException;
import com.gstcompliance.repository.Gstr2bInvoiceRepository;
import com.gstcompliance.repository.Gstr2bLineItemRepository;
import com.gstcompliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Gstr2bUploadService {

    private final Gstr2bInvoiceRepository gstr2bInvoiceRepository;
    private final Gstr2bLineItemRepository gstr2bLineItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public Gstr2bUploadResponse uploadGstr2bData(Gstr2bUploadRequest request, String email) {
        log.info("Starting GSTR2B upload processing for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        List<Gstr2bUploadResponse.InvoiceResult> results = new ArrayList<>();
        int successfulCount = 0;
        int failedCount = 0;

        for (Gstr2bInvoiceDto invoiceDto : request.getInvoices()) {
            try {
                // Check for duplicate invoice *before* triggering any entity transaction state changes
                boolean exists = gstr2bInvoiceRepository.existsByUserAndSupplierGstinAndInvoiceNumber(
                        user,
                        invoiceDto.getSupplierGstin(),
                        invoiceDto.getInvoiceNumber());

                if (exists) {
                    log.warn("Duplicate invoice identified: {} for supplier: {}",
                            invoiceDto.getInvoiceNumber(), invoiceDto.getSupplierGstin());
                    failedCount++;
                    results.add(Gstr2bUploadResponse.InvoiceResult.builder()
                            .invoiceNumber(invoiceDto.getInvoiceNumber())
                            .success(false)
                            .message("Invoice already exists: " + invoiceDto.getInvoiceNumber())
                            .build());
                    continue;
                }

                // Delegate to save operation for the verified clean records
                Gstr2bUploadResponse.InvoiceResult result = saveInvoiceRecord(invoiceDto, user);
                results.add(result);
                successfulCount++;

            } catch (Exception e) {
                log.error("Unhandled exception processing invoice {}: {}", invoiceDto.getInvoiceNumber(), e.getMessage(), e);
                failedCount++;
                results.add(Gstr2bUploadResponse.InvoiceResult.builder()
                        .invoiceNumber(invoiceDto.getInvoiceNumber())
                        .success(false)
                        .message("Processing error: " + e.getMessage())
                        .build());
            }
        }

        log.info("GSTR2B upload batch processed for user: {}. Total: {}, Successful: {}, Failed: {}",
                email, request.getInvoices().size(), successfulCount, failedCount);

        return Gstr2bUploadResponse.builder()
                .totalProcessed(request.getInvoices().size())
                .successfulCount(successfulCount)
                .failedCount(failedCount)
                .results(results)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Persists the verified GSTR2B invoice and its nested line items.
     */
    private Gstr2bUploadResponse.InvoiceResult saveInvoiceRecord(Gstr2bInvoiceDto invoiceDto, User user) {
        log.debug("Persisting GSTR2B invoice entity: {}", invoiceDto.getInvoiceNumber());

        Gstr2bInvoice invoice = Gstr2bInvoice.builder()
                .user(user)
                .supplierName(invoiceDto.getSupplierName())
                .supplierGstin(invoiceDto.getSupplierGstin())
                .buyerGstin(invoiceDto.getBuyerGstin())
                .invoiceNumber(invoiceDto.getInvoiceNumber())
                .invoiceDate(invoiceDto.getInvoiceDate())
                .taxableValue(invoiceDto.getTaxableValue())
                .cgst(invoiceDto.getCgst())
                .sgst(invoiceDto.getSgst())
                .igst(invoiceDto.getIgst())
                .grandTotal(invoiceDto.getGrandTotal())

                .build();

        Gstr2bInvoice savedInvoice = gstr2bInvoiceRepository.save(invoice);
        log.debug("Saved GSTR2B invoice header with ID: {}", savedInvoice.getId());

        List<Gstr2bLineItem> lineItems = invoiceDto.getLineItems().stream()
                .map(itemDto -> Gstr2bLineItem.builder()
                        .gstr2bInvoice(savedInvoice)
                        .description(itemDto.getDescription())
                        .quantity(itemDto.getQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .hsnCode(itemDto.getHsnCode())
                        .gstRate(itemDto.getGstRate())
                        .taxableValue(itemDto.getTaxableValue())
                        .cgstAmount(itemDto.getCgstAmount())
                        .sgstAmount(itemDto.getSgstAmount())
                        .igstAmount(itemDto.getIgstAmount())
                        .build())
                .collect(Collectors.toList());

        gstr2bLineItemRepository.saveAll(lineItems);
        log.debug("Successfully saved {} line items for invoice number: {}", lineItems.size(), invoiceDto.getInvoiceNumber());

        return Gstr2bUploadResponse.InvoiceResult.builder()
                .invoiceNumber(invoiceDto.getInvoiceNumber())
                .success(true)
                .message("Invoice uploaded successfully")
                .invoiceId(savedInvoice.getId())
                .build();
    }
}