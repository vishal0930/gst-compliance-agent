package com.gstcompliance.service;

import com.gstcompliance.model.Gstr2bInvoice;
import com.gstcompliance.model.Gstr2bLineItem;
import com.gstcompliance.model.User;
import com.gstcompliance.repository.Gstr2bInvoiceRepository;
import com.gstcompliance.repository.Gstr2bLineItemRepository;
import com.gstcompliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final Gstr2bInvoiceRepository gstr2bInvoiceRepository;
    private final Gstr2bLineItemRepository gstr2bLineItemRepository;
    private final UserRepository userRepository;

    /**
     * Fetch GSTR-2B invoices from the database matching the user and period.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> fetchGstr2b(String email, int month, int year) {
        log.info("Fetching database-backed GSTR-2B for user: {}, period: {}-{}", email, month, year);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));

            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            List<Gstr2bInvoice> databaseInvoices = gstr2bInvoiceRepository
                    .findByUserAndInvoiceDateBetween(user, startDate, endDate);

            if (databaseInvoices == null || databaseInvoices.isEmpty()) {
                log.warn("No GSTR-2B database records found for period: {}-{}", month, year);
                return new ArrayList<>();
            }

            // Transform entities into the exact Map structure expected by Gstr2bReconcilerAgent
            List<Map<String, Object>> portalInvoices = databaseInvoices.stream().map(invoice -> {
                Map<String, Object> map = new HashMap<>();
                map.put("supplierName", invoice.getSupplierName());
                map.put("supplierGstin", invoice.getSupplierGstin());
                map.put("buyerGstin", invoice.getBuyerGstin());
                map.put("invoiceNumber", invoice.getInvoiceNumber());
                map.put("invoiceDate", invoice.getInvoiceDate());
                map.put("taxableValue", invoice.getTaxableValue());
                map.put("cgst", invoice.getCgst());
                map.put("sgst", invoice.getSgst());
                map.put("igst", invoice.getIgst());
                map.put("grandTotal", invoice.getGrandTotal());

                // Fetch line items explicitly within the transaction
                List<Gstr2bLineItem> lineItems = gstr2bLineItemRepository
                        .findByGstr2bInvoiceId(invoice.getId());

                // Convert line items to Map structure to avoid lazy loading issues
                List<Map<String, Object>> lineItemMaps = lineItems.stream().map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("description", item.getDescription());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("hsnCode", item.getHsnCode());
                    itemMap.put("gstRate", item.getGstRate());
                    itemMap.put("taxableValue", item.getTaxableValue());
                    itemMap.put("cgstAmount", item.getCgstAmount());
                    itemMap.put("sgstAmount", item.getSgstAmount());
                    itemMap.put("igstAmount", item.getIgstAmount());
                    return itemMap;
                }).collect(Collectors.toList());

                map.put("lineItems", lineItemMaps);
                return map;
            }).collect(Collectors.toList());

            log.info("Successfully retrieved and mapped {} GSTR-2B records from database", portalInvoices.size());
            return portalInvoices;

        } catch (Exception e) {
            log.error("Failed to fetch GSTR-2B records: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public boolean validateGstin(String gstin) {
        if (gstin == null || gstin.length() != 15) {
            return false;
        }
        return gstin.matches("\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}\\d{1}[A-Z]{1}\\d{1}");
    }

    public Map<String, Object> getGstPortalStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "CONNECTED");
        status.put("dataSource", "PostgreSQL_Database");
        status.put("lastSync", new Date());
        return status;
    }
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(
            String email,
            int month,
            int year) {

        List<Map<String, Object>> invoices =
                fetchGstr2b(email, month, year);

        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map<String, Object> invoice : invoices) {

            taxable = taxable.add(
                    (BigDecimal) invoice.getOrDefault(
                            "taxableValue",
                            BigDecimal.ZERO
                    )
            );

            cgst = cgst.add(
                    (BigDecimal) invoice.getOrDefault(
                            "cgst",
                            BigDecimal.ZERO
                    )
            );

            sgst = sgst.add(
                    (BigDecimal) invoice.getOrDefault(
                            "sgst",
                            BigDecimal.ZERO
                    )
            );

            igst = igst.add(
                    (BigDecimal) invoice.getOrDefault(
                            "igst",
                            BigDecimal.ZERO
                    )
            );

            grandTotal = grandTotal.add(
                    (BigDecimal) invoice.getOrDefault(
                            "grandTotal",
                            BigDecimal.ZERO
                    )
            );
        }

        Map<String, Object> summary = new HashMap<>();

        summary.put("invoiceCount", invoices.size());
        summary.put("taxableValue", taxable);
        summary.put("cgst", cgst);
        summary.put("sgst", sgst);
        summary.put("igst", igst);
        summary.put("grandTotal", grandTotal);
        summary.put("totalItc", cgst.add(sgst).add(igst));

        return summary;
    }
}