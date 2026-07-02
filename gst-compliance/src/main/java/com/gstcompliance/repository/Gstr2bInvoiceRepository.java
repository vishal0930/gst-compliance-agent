package com.gstcompliance.repository;

import com.gstcompliance.model.Gstr2bInvoice;
import com.gstcompliance.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Gstr2bInvoiceRepository extends JpaRepository<Gstr2bInvoice, UUID> {

    // ── Period-based (primary query pattern) ───────────────────────────

    Page<Gstr2bInvoice> findByUserIdAndTaxPeriod(UUID userId, String taxPeriod, Pageable pageable);

    List<Gstr2bInvoice> findByUserIdAndTaxPeriod(UUID userId, String taxPeriod);

    boolean existsByUserIdAndTaxPeriod(UUID userId, String taxPeriod);

    long countByUserIdAndTaxPeriod(UUID userId, String taxPeriod);

    // ── Duplicate detection (period-scoped unique key) ──────────────────

    boolean existsByUserIdAndSupplierGstinAndInvoiceNumberAndTaxPeriod(
            UUID userId, String supplierGstin, String invoiceNumber, String taxPeriod);

    Optional<Gstr2bInvoice> findByUserIdAndSupplierGstinAndInvoiceNumberAndTaxPeriod(
            UUID userId, String supplierGstin, String invoiceNumber, String taxPeriod);

    // ── Filtered + paginated search ─────────────────────────────────────

    @Query("""
        SELECT i FROM Gstr2bInvoice i
        WHERE i.user.id = :userId
          AND i.taxPeriod = :taxPeriod
          AND (:supplierGstin IS NULL OR i.supplierGstin LIKE %:supplierGstin%)
          AND (:supplierName  IS NULL OR i.supplierName LIKE CONCAT('%', :supplierName, '%'))
          AND (:invoiceNumber IS NULL OR i.invoiceNumber LIKE CONCAT('%', :invoiceNumber, '%'))
          AND (:importStatus  IS NULL OR i.importStatus = :importStatus)
          AND (:matchStatus   IS NULL OR i.matchStatus  = :matchStatus)
        """)
    Page<Gstr2bInvoice> findFiltered(
            @Param("userId")        UUID    userId,
            @Param("taxPeriod")     String  taxPeriod,
            @Param("supplierGstin") String  supplierGstin,
            @Param("supplierName")  String  supplierName,
            @Param("invoiceNumber") String  invoiceNumber,
            @Param("importStatus")  String  importStatus,
            @Param("matchStatus")   String  matchStatus,
            Pageable pageable);

    // ── Summary aggregates (used by getSummary) ─────────────────────────

    @Query("""
        SELECT COUNT(DISTINCT i.supplierGstin)
        FROM Gstr2bInvoice i
        WHERE i.user.id = :userId AND i.taxPeriod = :taxPeriod
        """)
    long countDistinctSuppliers(@Param("userId") UUID userId, @Param("taxPeriod") String taxPeriod);

    // ── Reconciliation agent still fetches by user + period ─────────────

    List<Gstr2bInvoice> findByUserAndTaxPeriod(User user, String taxPeriod);

    // ── Bulk delete for "replace" import ────────────────────────────────

    void deleteByUserIdAndTaxPeriod(UUID userId, String taxPeriod);

    // ── Legacy: keep for GstrPortalService until fully migrated ─────────

    boolean existsByUserAndSupplierGstinAndInvoiceNumber(
            User user, String supplierGstin, String invoiceNumber);

    // Fallback date-range query (used when taxPeriod has no data yet)
    List<Gstr2bInvoice> findByUserAndInvoiceDateBetween(
            User user,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate);
}
