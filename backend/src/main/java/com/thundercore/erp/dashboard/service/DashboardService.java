package com.thundercore.erp.dashboard.service;

import com.thundercore.erp.auth.repository.UserRepository;
import com.thundercore.erp.finance.entity.Invoice;
import com.thundercore.erp.finance.repository.InvoiceRepository;
import com.thundercore.erp.hr.repository.EmployeeRepository;
import com.thundercore.erp.inventory.entity.Product;
import com.thundercore.erp.inventory.repository.ProductRepository;
import com.thundercore.erp.sales.repository.CustomerRepository;
import com.thundercore.erp.sales.repository.SaleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * DashboardService builds cross-module KPIs for the executive workspace.
 *
 * <p>The service reads from domain repositories and produces a stable map
 * contract for cards, category charts, invoice status charts, and monthly paid
 * revenue trends.</p>
 */
public class DashboardService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ProductRepository productRepository;
    private final EmployeeRepository employeeRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SaleOrderRepository saleOrderRepository;

    /**
     * Aggregates dashboard metrics from inventory, HR, finance, auth, and CRM.
     *
     * @return frontend-ready map of KPI values and chart series
     */
    public Map<String, Object> getStats() {
        List<Product> products = productRepository.findAll();
        List<Invoice> invoices = invoiceRepository.findAll();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalProducts", productRepository.count());
        stats.put("totalEmployees", employeeRepository.count());
        stats.put("totalInvoices", invoiceRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalCustomers", customerRepository.count());
        stats.put("totalSalesOrders", saleOrderRepository.count());
        stats.put("totalRevenue", invoiceRepository.sumNetAmountByStatus(Invoice.InvoiceStatus.PAID));
        stats.put("pendingInvoices", invoiceRepository.countByStatus(Invoice.InvoiceStatus.PENDING));
        stats.put("lowStockItems", products.stream().filter(this::isLowStock).count());
        stats.put("categoryCounts", buildCategoryCounts(products));
        stats.put("invoiceStatusCounts", buildInvoiceStatusCounts(invoices));
        stats.put("monthlyRevenue", buildMonthlyRevenue(invoices));
        return stats;
    }

    /** Applies the same low-stock rule used by inventory notifications. */
    private boolean isLowStock(Product product) {
        int quantity = product.getQuantity() == null ? 0 : product.getQuantity();
        int threshold = product.getReorderThreshold() == null ? 0 : product.getReorderThreshold();
        return quantity <= threshold;
    }

    /** Groups products by category for the inventory bar chart. */
    private Map<String, Long> buildCategoryCounts(List<Product> products) {
        return products.stream()
                .collect(Collectors.groupingBy(
                        product -> normalizeLabel(product.getCategory(), "Uncategorized"),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    /** Counts invoices across every supported lifecycle status. */
    private Map<String, Long> buildInvoiceStatusCounts(List<Invoice> invoices) {
        Map<String, Long> counts = new LinkedHashMap<>();
        Arrays.stream(Invoice.InvoiceStatus.values()).forEach(status -> counts.put(status.name(), 0L));
        invoices.forEach(invoice -> {
            Invoice.InvoiceStatus status = invoice.getStatus() == null ? Invoice.InvoiceStatus.PENDING : invoice.getStatus();
            counts.computeIfPresent(status.name(), (key, value) -> value + 1);
        });
        return counts;
    }

    /** Builds month-keyed revenue totals from invoices marked PAID. */
    private Map<String, BigDecimal> buildMonthlyRevenue(List<Invoice> invoices) {
        Map<String, BigDecimal> revenue = new TreeMap<>();
        invoices.stream()
                .filter(invoice -> invoice.getStatus() == Invoice.InvoiceStatus.PAID)
                .forEach(invoice -> {
                    String month = invoice.getCreatedAt() == null
                            ? "Unscheduled"
                            : invoice.getCreatedAt().format(MONTH_FORMATTER);
                    BigDecimal amount = invoice.getNetAmount() == null ? BigDecimal.ZERO : invoice.getNetAmount();
                    revenue.merge(month, amount, BigDecimal::add);
                });
        return revenue;
    }

    /** Normalizes blank labels so charts never render empty legend values. */
    private String normalizeLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
