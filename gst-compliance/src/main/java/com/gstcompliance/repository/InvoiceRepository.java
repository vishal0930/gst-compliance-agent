package com.gstcompliance.repository;

import com.gstcompliance.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByUserId(UUID userId, Pageable pageable);

    Optional<Invoice> findByUserIdAndInvoiceNumberAndVendorGstin(
            UUID userId, String invoiceNumber, String vendorGstin);

    List<Invoice> findByUserIdAndInvoiceDateBetween(
            UUID userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId AND i.parseStatus = :status")
    List<Invoice> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId AND i.invoiceDate BETWEEN :start AND :end")
    List<Invoice> findInvoicesForPeriod(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    long countByUserIdAndParseStatus(UUID userId, String status);
}