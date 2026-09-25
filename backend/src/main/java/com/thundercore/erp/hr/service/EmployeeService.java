package com.thundercore.erp.hr.service;

import com.thundercore.erp.auth.entity.User;
import com.thundercore.erp.auth.repository.UserRepository;
import com.thundercore.erp.dashboard.service.DashboardEventService;
import com.thundercore.erp.hr.entity.Employee;
import com.thundercore.erp.hr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * EmployeeService handles HR business rules.
 *
 * <p>Responsibilities include employee CRUD, optional user profile linkage,
 * payroll base-salary storage, employment status updates, and dashboard
 * WebSocket broadcasting after HR mutations.</p>
 */
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DashboardEventService dashboardEventService;

    /** Returns every employee record. */
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    /**
     * Loads an employee or fails with a domain-friendly message.
     *
     * @param id employee primary key
     * @return persisted employee
     */
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }

    @Transactional
    /**
     * Creates an employee and optionally links it to a login user.
     *
     * @param employee HR record to persist
     * @param userId optional user account id
     * @return saved employee
     */
    public Employee createEmployee(Employee employee, Long userId) {
        if (userId != null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            employee.setUser(user);
        }
        Employee saved = employeeRepository.save(employee);
        dashboardEventService.broadcastDashboardUpdate("employee-created");
        return saved;
    }

    @Transactional
    /**
     * Applies non-null HR profile updates while preserving unchanged fields.
     *
     * @param id employee primary key
     * @param employeeDetails update payload
     * @return saved employee
     */
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee employee = getEmployeeById(id);
        if (employeeDetails.getEmployeeCode() != null) employee.setEmployeeCode(employeeDetails.getEmployeeCode());
        if (employeeDetails.getDepartment() != null) employee.setDepartment(employeeDetails.getDepartment());
        if (employeeDetails.getDesignation() != null) employee.setDesignation(employeeDetails.getDesignation());
        if (employeeDetails.getBaseSalary() != null) employee.setBaseSalary(employeeDetails.getBaseSalary());
        if (employeeDetails.getStatus() != null) employee.setStatus(employeeDetails.getStatus());
        if (employeeDetails.getJoiningDate() != null) employee.setJoiningDate(employeeDetails.getJoiningDate());
        Employee saved = employeeRepository.save(employee);
        dashboardEventService.broadcastDashboardUpdate("employee-updated");
        return saved;
    }

    @Transactional
    /**
     * Deletes an employee record after validating existence.
     *
     * @param id employee primary key
     */
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
        dashboardEventService.broadcastDashboardUpdate("employee-deleted");
    }

    /** Returns the number of HR records for reporting and dashboards. */
    public long countEmployees() {
        return employeeRepository.count();
    }
}
