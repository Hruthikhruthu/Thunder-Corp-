package com.thundercore.erp.hr.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thundercore.erp.auth.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Employee represents the HR profile and payroll baseline for a worker.
 *
 * <p>The optional one-to-one user relationship links operational login access
 * to the HR record without exposing the lazy User object in API JSON.</p>
 */
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optional login account associated with this employee profile. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Employee code is required")
    private String employeeCode;

    private String department;
    private String designation;

    @DecimalMin(value = "0.0", inclusive = true, message = "Base salary cannot be negative")
    private BigDecimal baseSalary;
    private LocalDate joiningDate;

    @Column(columnDefinition = "varchar(50) default 'ACTIVE'")
    private String status = "ACTIVE";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
