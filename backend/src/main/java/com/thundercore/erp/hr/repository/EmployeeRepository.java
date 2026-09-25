package com.thundercore.erp.hr.repository;

import com.thundercore.erp.hr.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * EmployeeRepository provides HR persistence and natural-key lookups.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    /** Finds employees by their unique business code. */
    Optional<Employee> findByEmployeeCode(String employeeCode);

    /** Resolves the HR profile linked to a login user. */
    Optional<Employee> findByUserId(Long userId);
}
