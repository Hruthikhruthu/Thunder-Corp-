package com.thundercore.erp.sales.repository;

import com.thundercore.erp.sales.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * CustomerRepository provides CRM persistence and lookup by unique email.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    /** Finds a customer by email for duplicate checks or integrations. */
    Optional<Customer> findByEmail(String email);
}
