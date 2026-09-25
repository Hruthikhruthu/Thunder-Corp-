package com.thundercore.erp.finance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Invoice stores billable customer activity for finance dashboards and reports.
 *
 * <p>The entity is intentionally customer-name based instead of requiring a
 * customer foreign key, which keeps invoice capture independent from CRM record
 * lifecycle changes during demos.</p>
 */
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String customerEmail;

    @Column(nullable = false)
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount cannot be negative")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @DecimalMin(value = "0.0", inclusive = true, message = "Tax amount cannot be negative")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @DecimalMin(value = "0.0", inclusive = true, message = "Net amount cannot be negative")
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Supported payment lifecycle states for invoice operations. */
    public enum InvoiceStatus {
        PENDING, PAID, OVERDUE, CANCELLED
    }
}
