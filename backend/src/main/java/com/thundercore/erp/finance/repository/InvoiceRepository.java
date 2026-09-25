package com.thundercore.erp.finance.repository;

import com.thundercore.erp.finance.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
/**
 * InvoiceRepository provides finance persistence and aggregate queries used by
 * revenue KPIs, invoice status charts, and report generation.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    /** Returns invoices for a single payment lifecycle state. */
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    /** Sums net amount for invoices matching the supplied status. */
    @Query("SELECT COALESCE(SUM(i.netAmount), 0) FROM Invoice i WHERE i.status = :status")
    BigDecimal sumNetAmountByStatus(@Param("status") Invoice.InvoiceStatus status);

    /** Counts invoices in one lifecycle status for dashboard cards. */
    Long countByStatus(Invoice.InvoiceStatus status);
}
