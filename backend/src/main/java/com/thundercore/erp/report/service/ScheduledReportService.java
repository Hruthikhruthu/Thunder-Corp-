package com.thundercore.erp.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledReportService {
    private final ReportService reportService;

    @Scheduled(cron = "${reports.schedule.cron:0 0 2 * * *}")
    public void generateDailyReportSnapshot() {
        try {
            reportService.generateInventoryExcel(null, null);
            reportService.generateInvoicePdf(null, null);
            log.info("Scheduled report snapshot generated");
        } catch (Exception ex) {
            log.warn("Scheduled report snapshot failed: {}", ex.getMessage());
        }
    }
}
