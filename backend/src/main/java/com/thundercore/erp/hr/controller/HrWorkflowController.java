package com.thundercore.erp.hr.controller;

import com.thundercore.erp.common.dto.ApiResponse;
import com.thundercore.erp.hr.entity.Attendance;
import com.thundercore.erp.hr.entity.LeaveRequest;
import com.thundercore.erp.hr.entity.Payroll;
import com.thundercore.erp.hr.service.HrWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrWorkflowController {
    private final HrWorkflowService hrWorkflowService;

    @GetMapping("/employees/{employeeId}/attendance")
    public ResponseEntity<ApiResponse<List<Attendance>>> getAttendance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Attendance retrieved", hrWorkflowService.getAttendance(employeeId)));
    }

    @PostMapping("/employees/{employeeId}/attendance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Attendance>> markAttendance(@PathVariable Long employeeId, @RequestBody Attendance attendance) {
        return ResponseEntity.ok(ApiResponse.success("Attendance saved", hrWorkflowService.markAttendance(employeeId, attendance)));
    }

    @GetMapping("/employees/{employeeId}/leave-requests")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getLeaveRequests(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Leave requests retrieved", hrWorkflowService.getLeaveRequests(employeeId)));
    }

    @PostMapping("/employees/{employeeId}/leave-requests")
    public ResponseEntity<ApiResponse<LeaveRequest>> requestLeave(@PathVariable Long employeeId, @RequestBody LeaveRequest leaveRequest) {
        return ResponseEntity.ok(ApiResponse.success("Leave requested", hrWorkflowService.requestLeave(employeeId, leaveRequest)));
    }

    @PatchMapping("/leave-requests/{leaveId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<LeaveRequest>> updateLeaveStatus(@PathVariable Long leaveId, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(ApiResponse.success("Leave status updated", hrWorkflowService.updateLeaveStatus(leaveId, payload.getOrDefault("status", "PENDING"))));
    }

    @GetMapping("/employees/{employeeId}/payrolls")
    public ResponseEntity<ApiResponse<List<Payroll>>> getPayrolls(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Payrolls retrieved", hrWorkflowService.getPayrolls(employeeId)));
    }

    @PostMapping("/employees/{employeeId}/payrolls")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Payroll>> calculatePayroll(@PathVariable Long employeeId, @RequestBody Payroll payroll) {
        return ResponseEntity.ok(ApiResponse.success("Payroll calculated", hrWorkflowService.calculatePayroll(employeeId, payroll)));
    }
}
