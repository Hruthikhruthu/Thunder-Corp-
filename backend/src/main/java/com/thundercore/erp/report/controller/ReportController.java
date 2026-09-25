package com.thundercore.erp.report.controller;

import com.thundercore.erp.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
/**
 * ReportController streams generated business reports to authenticated users.
 *
 * <p>Reports are created in memory and returned as binary responses with
 * download-friendly content disposition headers.</p>
 */
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/inventory/excel")
    /** Downloads the current inventory catalog as an Excel workbook. */
    public ResponseEntity<byte[]> downloadInventoryExcel(@RequestParam(required = false) LocalDate from,
                                                         @RequestParam(required = false) LocalDate to) throws Exception {
        byte[] data = reportService.generateInventoryExcel(from, to);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "inventory-report.xlsx");
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/invoices/pdf")
    /** Downloads invoice finance data as a PDF report. */
    public ResponseEntity<byte[]> downloadInvoicePdf(@RequestParam(required = false) LocalDate from,
                                                     @RequestParam(required = false) LocalDate to) throws Exception {
        byte[] data = reportService.generateInvoicePdf(from, to);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-report.pdf");
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
