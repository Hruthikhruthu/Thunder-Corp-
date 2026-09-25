package com.thundercore.erp.finance.controller;

import com.thundercore.erp.common.dto.ApiResponse;
import com.thundercore.erp.finance.entity.Invoice;
import com.thundercore.erp.finance.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
/**
 * InvoiceController exposes finance APIs for invoice management and revenue
 * reporting.
 *
 * <p>Status updates are separated from full invoice creation because payment
 * state changes are operational events that can trigger overdue notifications
 * and realtime dashboard refreshes.</p>
 */
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/invoices")
    /** Returns all invoices for finance tables and report generation. */
    public ResponseEntity<ApiResponse<List<Invoice>>> getAllInvoices() {
        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved", invoiceService.getAllInvoices()));
    }

    @GetMapping("/invoices/{id}")
    /** Returns a single invoice by primary key. */
    public ResponseEntity<ApiResponse<Invoice>> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice retrieved", invoiceService.getInvoiceById(id)));
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /** Creates an invoice and calculates net amount from total plus tax. */
    public ResponseEntity<ApiResponse<Invoice>> createInvoice(@Valid @RequestBody Invoice invoice) {
        return ResponseEntity.ok(ApiResponse.success("Invoice created", invoiceService.createInvoice(invoice)));
    }

    @PatchMapping("/invoices/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /**
     * Updates payment lifecycle status without rewriting invoice amounts.
     *
     * @param id invoice primary key
     * @param payload request body containing a status value
     * @return updated invoice
     */
    public ResponseEntity<ApiResponse<Invoice>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String statusValue = payload.get("status");
        if (statusValue == null || statusValue.isBlank()) {
            throw new IllegalArgumentException("Invoice status is required");
        }
        Invoice.InvoiceStatus status = Invoice.InvoiceStatus.valueOf(statusValue.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Status updated", invoiceService.updateInvoiceStatus(id, status)));
    }

    @DeleteMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /** Deletes an invoice and refreshes dashboard finance metrics. */
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice deleted", null));
    }

    @GetMapping("/stats/revenue")
    /** Returns revenue from PAID invoices only. */
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalRevenue() {
        return ResponseEntity.ok(ApiResponse.success("Revenue retrieved", invoiceService.getTotalRevenue()));
    }
}
