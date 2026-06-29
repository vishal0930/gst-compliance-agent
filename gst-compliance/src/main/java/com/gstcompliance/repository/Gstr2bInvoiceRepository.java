package com.gstcompliance.repository;

import com.gstcompliance.model.Gstr2bInvoice;
import com.gstcompliance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Gstr2bInvoiceRepository extends JpaRepository<Gstr2bInvoice, UUID> {

    List<Gstr2bInvoice> findByUser(User user);

    List<Gstr2bInvoice> findByUserId(UUID userId);

    List<Gstr2bInvoice> findByBuyerGstin(String buyerGstin);

    Optional<Gstr2bInvoice> findBySupplierGstinAndInvoiceNumber(
            String supplierGstin,
            String invoiceNumber
    );

    List<Gstr2bInvoice> findByInvoiceDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    List<Gstr2bInvoice> findByUserIdAndInvoiceDateBetween(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate
    );
    Optional<Gstr2bInvoice> findByUserIdAndSupplierGstinAndInvoiceNumber(
            UUID userId,
            String supplierGstin,
            String invoiceNumber
    );

    List<Gstr2bInvoice> findByUserAndInvoiceDateBetween(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );
    boolean existsByUserAndSupplierGstinAndInvoiceNumber(
            User user,
            String supplierGstin,
            String invoiceNumber
    );
}