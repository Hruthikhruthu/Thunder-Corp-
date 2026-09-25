package com.thundercore.erp.hr.service;

import com.thundercore.erp.dashboard.service.DashboardEventService;
import com.thundercore.erp.hr.entity.Attendance;
import com.thundercore.erp.hr.entity.Employee;
import com.thundercore.erp.hr.entity.LeaveRequest;
import com.thundercore.erp.hr.entity.Payroll;
import com.thundercore.erp.hr.repository.AttendanceRepository;
import com.thundercore.erp.hr.repository.EmployeeRepository;
import com.thundercore.erp.hr.repository.LeaveRequestRepository;
import com.thundercore.erp.hr.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrWorkflowService {
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final DashboardEventService dashboardEventService;

    public List<Attendance> getAttendance(Long employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByDateDesc(employeeId);
    }

    @Transactional
    public Attendance markAttendance(Long employeeId, Attendance payload) {
        Employee employee = employee(employeeId);
        LocalDate date = payload.getDate() == null ? LocalDate.now() : payload.getDate();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, date).orElseGet(Attendance::new);
        attendance.setEmployee(employee);
        attendance.setDate(date);
        attendance.setStatus(payload.getStatus() == null ? "PRESENT" : payload.getStatus().toUpperCase());
        attendance.setCheckInTime(payload.getCheckInTime());
        attendance.setCheckOutTime(payload.getCheckOutTime());
        Attendance saved = attendanceRepository.save(attendance);
        dashboardEventService.broadcastDashboardUpdate("attendance-marked");
        return saved;
    }

    public List<LeaveRequest> getLeaveRequests(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    @Transactional
    public LeaveRequest requestLeave(Long employeeId, LeaveRequest payload) {
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee(employeeId));
        leaveRequest.setStartDate(payload.getStartDate());
        leaveRequest.setEndDate(payload.getEndDate());
        leaveRequest.setReason(payload.getReason());
        leaveRequest.setStatus("PENDING");
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        dashboardEventService.broadcastDashboardUpdate("leave-requested");
        return saved;
    }

    @Transactional
    public LeaveRequest updateLeaveStatus(Long leaveId, String status) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        leaveRequest.setStatus(status.toUpperCase());
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        dashboardEventService.broadcastDashboardUpdate("leave-" + saved.getStatus().toLowerCase());
        return saved;
    }

    public List<Payroll> getPayrolls(Long employeeId) {
        return payrollRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId);
    }

    @Transactional
    public Payroll calculatePayroll(Long employeeId, Payroll payload) {
        Employee employee = employee(employeeId);
        BigDecimal basic = payload.getBasicSalary() == null
                ? (employee.getBaseSalary() == null ? BigDecimal.ZERO : employee.getBaseSalary())
                : payload.getBasicSalary();
        BigDecimal allowances = payload.getAllowances() == null ? BigDecimal.ZERO : payload.getAllowances();
        BigDecimal deductions = payload.getDeductions() == null ? BigDecimal.ZERO : payload.getDeductions();
        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setMonth(payload.getMonth());
        payroll.setYear(payload.getYear());
        payroll.setBasicSalary(basic);
        payroll.setAllowances(allowances);
        payroll.setDeductions(deductions);
        payroll.setNetSalary(basic.add(allowances).subtract(deductions));
        payroll.setStatus(payload.getStatus() == null ? "PENDING" : payload.getStatus().toUpperCase());
        Payroll saved = payrollRepository.save(payroll);
        dashboardEventService.broadcastDashboardUpdate("payroll-calculated");
        return saved;
    }

    private Employee employee(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }
}
