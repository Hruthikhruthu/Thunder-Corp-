package com.thundercore.erp.sales.controller;

import com.thundercore.erp.common.dto.ApiResponse;
import com.thundercore.erp.dashboard.service.DashboardEventService;
import com.thundercore.erp.sales.entity.Customer;
import com.thundercore.erp.sales.repository.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales/customers")
@RequiredArgsConstructor
/**
 * CustomerController manages CRM customer records used by the sales module.
 *
 * <p>Customer mutations refresh dashboard metrics so frontend KPI cards and
 * charts remain aligned with the latest CRM state.</p>
 */
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final DashboardEventService dashboardEventService;

    @GetMapping
    /** Returns all CRM customer records. */
    public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved", customerRepository.findAll()));
    }

    @GetMapping("/{id}")
    /** Returns one customer by primary key. */
    public ResponseEntity<ApiResponse<Customer>> getCustomerById(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved", customer));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /** Creates a customer and broadcasts a dashboard refresh event. */
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@Valid @RequestBody Customer customer) {
        Customer saved = customerRepository.save(customer);
        dashboardEventService.broadcastDashboardUpdate("customer-created");
        return ResponseEntity.ok(ApiResponse.success("Customer created", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /** Partially updates customer profile fields used by CRM screens. */
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        if (customerDetails.getName() != null) customer.setName(customerDetails.getName());
        if (customerDetails.getEmail() != null) customer.setEmail(customerDetails.getEmail());
        if (customerDetails.getPhone() != null) customer.setPhone(customerDetails.getPhone());
        if (customerDetails.getAddress() != null) customer.setAddress(customerDetails.getAddress());
        if (customerDetails.getTier() != null) customer.setTier(customerDetails.getTier());
        Customer saved = customerRepository.save(customer);
        dashboardEventService.broadcastDashboardUpdate("customer-updated");
        return ResponseEntity.ok(ApiResponse.success("Customer updated", saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /** Deletes a customer record and updates live dashboard counts. */
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerRepository.deleteById(id);
        dashboardEventService.broadcastDashboardUpdate("customer-deleted");
        return ResponseEntity.ok(ApiResponse.success("Customer deleted", null));
    }
}
