package com.thundercore.erp.hr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payrolls")
@Data
/**
 * Payroll stores monthly salary calculation results for an employee.
 *
 * <p>net_salary = basic_salary + allowances - deductions.
 * The month/year pair identifies the payroll period.</p>
 */
public class Payroll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Employee employee;

    /** Payroll period month, e.g. "January" or "2025-01". */
    @Column(name = "payroll_month", nullable = false)
    private String month;

    /** Payroll period year, e.g. 2025. */
    @Column(name = "payroll_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    private BigDecimal allowances = BigDecimal.ZERO;
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal netSalary = BigDecimal.ZERO;

    /** Lifecycle status: PENDING, APPROVED, PAID. */
    @Column(nullable = false)
    private String status = "PENDING";

    @CreationTimestamp
    private LocalDateTime createdAt;
}
