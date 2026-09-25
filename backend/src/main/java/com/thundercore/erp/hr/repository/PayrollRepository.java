package com.thundercore.erp.hr.repository;

import com.thundercore.erp.hr.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * PayrollRepository provides payroll persistence and employee-scoped queries.
 */
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    /**
     * Returns payrolls for an employee ordered newest-first by year then month.
     * Uses JPQL field names (year, month) which map to payroll_year/payroll_month columns.
     */
    @Query("SELECT p FROM Payroll p WHERE p.employee.id = :employeeId ORDER BY p.year DESC, p.month DESC")
    List<Payroll> findByEmployeeIdOrderByYearDescMonthDesc(@Param("employeeId") Long employeeId);
}
