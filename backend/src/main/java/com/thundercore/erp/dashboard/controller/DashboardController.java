package com.thundercore.erp.dashboard.controller;

import com.thundercore.erp.common.dto.ApiResponse;
import com.thundercore.erp.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
/**
 * DashboardController exposes the aggregate operational snapshot consumed by
 * the React dashboard.
 *
 * <p>The response intentionally combines inventory, HR, finance, sales, and
 * user counts so the frontend can render KPI cards and charts with one API
 * request.</p>
 */
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    /** Returns dashboard KPIs, chart series, and realtime-friendly aggregates. */
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", dashboardService.getStats()));
    }
}
