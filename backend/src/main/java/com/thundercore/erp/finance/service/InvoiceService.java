package com.thundercore.erp.finance.service;

import com.thundercore.erp.dashboard.service.DashboardEventService;
import com.thundercore.erp.finance.entity.Invoice;
import com.thundercore.erp.finance.repository.InvoiceRepository;
import com.thundercore.erp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * InvoiceService owns finance business logic.
 *
 * <p>Responsibilities include invoice numbering, tax/net calculations, payment
 * status transitions, overdue notification triggering, revenue aggregation, and
 * dashboard WebSocket broadcasting.</p>
 */
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final DashboardEventService dashboardEventService;
    private final NotificationService notificationService;

    /** Returns every invoice for the finance workspace. */
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    /**
     * Loads an invoice or fails with a useful API error.
     *
     * @param id invoice primary key
     * @return persisted invoice
     */
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
    }

    /**
     * Creates an invoice with a generated business number and calculated net.
     *
     * @param invoice invoice details from the API request
     * @return persisted invoice
     */
    public Invoice createInvoice(Invoice invoice) {
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        if (invoice.getTaxAmount() == null) invoice.setTaxAmount(BigDecimal.ZERO);
        if (invoice.getTotalAmount() == null) invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setNetAmount(invoice.getTotalAmount().add(invoice.getTaxAmount()));
        Invoice saved = invoiceRepository.save(invoice);
        dashboardEventService.broadcastDashboardUpdate("invoice-created");
        return saved;
    }

    /**
     * Updates invoice status and sends manager alerts when invoices go overdue.
     *
     * @param id invoice primary key
     * @param status new payment lifecycle status
     * @return updated invoice
     */
    public Invoice updateInvoiceStatus(Long id, Invoice.InvoiceStatus status) {
        Invoice invoice = getInvoiceById(id);
        invoice.setStatus(status);
        Invoice saved = invoiceRepository.save(invoice);
        if (status == Invoice.InvoiceStatus.OVERDUE) {
            notificationService.sendNotificationToRoles(
                    Set.of("SUPER_ADMIN", "MANAGER"),
                    "Invoice overdue",
                    saved.getInvoiceNumber() + " for " + saved.getCustomerName() + " is overdue."
            );
        }
        dashboardEventService.broadcastDashboardUpdate("invoice-status-updated");
        return saved;
    }

    /** Deletes an invoice and broadcasts a dashboard update. */
    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
        dashboardEventService.broadcastDashboardUpdate("invoice-deleted");
    }

    /** Calculates recognized revenue from invoices marked PAID. */
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = invoiceRepository.sumNetAmountByStatus(Invoice.InvoiceStatus.PAID);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    /** Counts invoices awaiting payment for finance KPI cards. */
    public Long getPendingCount() {
        return invoiceRepository.countByStatus(Invoice.InvoiceStatus.PENDING);
    }
}
