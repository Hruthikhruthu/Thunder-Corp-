package com.thundercore.erp.hr.controller;

import com.thundercore.erp.common.dto.ApiResponse;
import com.thundercore.erp.hr.entity.Employee;
import com.thundercore.erp.hr.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/employees")
@RequiredArgsConstructor
/**
 * EmployeeController manages HR employee records.
 *
 * <p>Employee changes refresh dashboard metrics. Optional user linking lets a
 * login account be associated with an employee profile while preserving HR data
 * when the user relationship is not required.</p>
 */
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    /** Returns all employee records for HR tables and dashboards. */
    public ResponseEntity<ApiResponse<List<Employee>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved", employeeService.getAllEmployees()));
    }

    @GetMapping("/{id}")
    /** Returns one employee by primary key. */
    public ResponseEntity<ApiResponse<Employee>> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved", employeeService.getEmployeeById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /**
     * Creates an employee and optionally links it to an existing user account.
     *
     * @param employee validated HR profile payload
     * @param userId optional user primary key to associate with the employee
     * @return persisted employee
     */
    public ResponseEntity<ApiResponse<Employee>> createEmployee(@Valid @RequestBody Employee employee, @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Employee created", employeeService.createEmployee(employee, userId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /** Updates employee payroll/profile fields and broadcasts dashboard state. */
    public ResponseEntity<ApiResponse<Employee>> updateEmployee(@PathVariable Long id, @Valid @RequestBody Employee employee) {
        return ResponseEntity.ok(ApiResponse.success("Employee updated", employeeService.updateEmployee(id, employee)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    /** Deletes an employee profile and refreshes HR dashboard totals. */
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted", null));
    }
}
