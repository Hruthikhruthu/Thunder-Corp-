package com.thundercore.erp.sales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Customer represents a CRM account tracked by the sales module.
 *
 * <p>Tier values such as STANDARD, PREMIUM, and VIP support lightweight account
 * segmentation in the frontend dashboard.</p>
 */
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Customer name is required")
    private String name;

    @Column(unique = true)
    @Email(message = "Email must be valid")
    private String email;

    private String phone;
    
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "varchar(50) default 'STANDARD'")
    private String tier = "STANDARD";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
