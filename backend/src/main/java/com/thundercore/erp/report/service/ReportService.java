package com.thundercore.erp.report.service;

import com.thundercore.erp.finance.entity.Invoice;
import com.thundercore.erp.finance.repository.InvoiceRepository;
import com.thundercore.erp.inventory.entity.Product;
import com.thundercore.erp.inventory.repository.ProductRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * ReportService generates export-ready inventory and finance reports.
 *
 * <p>Apache POI is used for Excel workbooks and iText 7 is used for PDFs. Both
 * reports are generated in memory to avoid filesystem state inside containers.</p>
 */
public class ReportService {

    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Builds an XLSX inventory workbook from current product records.
     *
     * @return workbook bytes ready for an HTTP attachment response
     */
    public byte[] generateInventoryExcel(LocalDate from, LocalDate to) throws IOException {
        List<Product> products = productRepository.findAll().stream()
                .filter(product -> withinRange(product.getCreatedAt() == null ? null : product.getCreatedAt().toLocalDate(), from, to))
                .toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inventory Report");

            // Header styling keeps the generated workbook presentation-ready.
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Static report columns mirror the inventory table in the frontend.
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "SKU", "Name", "Category", "Quantity", "Unit Price", "Reorder Threshold"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Each product is written with null-safe values for reliable exports.
            int rowNum = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(nullSafe(p.getSku()));
                row.createCell(2).setCellValue(nullSafe(p.getName()));
                row.createCell(3).setCellValue(p.getCategory() != null ? p.getCategory() : "");
                row.createCell(4).setCellValue(p.getQuantity() == null ? 0 : p.getQuantity());
                row.createCell(5).setCellValue(money(p.getUnitPrice()).doubleValue());
                row.createCell(6).setCellValue(p.getReorderThreshold() == null ? 0 : p.getReorderThreshold());
            }

            // Auto-size columns after writing rows so downloaded files open cleanly.
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Builds a PDF invoice report from current finance records.
     *
     * @return PDF bytes ready for an HTTP attachment response
     */
    public byte[] generateInvoicePdf(LocalDate from, LocalDate to) {
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(invoice -> withinRange(invoice.getCreatedAt() == null ? null : invoice.getCreatedAt().toLocalDate(), from, to))
                .toList();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDocument = new PdfDocument(writer);
        try (Document document = new Document(pdfDocument)) {
            document.add(new Paragraph("ThunderCore ERP - Invoice Report")
                    .setBold()
                    .setFontSize(18)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 1.4f, 1.4f, 1.5f}))
                    .useAllAvailableWidth();

            String[] headers = {"Invoice #", "Customer", "Total", "Tax", "Status"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(header).setBold().setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(new DeviceRgb(63, 81, 181))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(8));
            }

            for (Invoice invoice : invoices) {
                table.addCell(pdfCell(nullSafe(invoice.getInvoiceNumber())));
                table.addCell(pdfCell(nullSafe(invoice.getCustomerName())));
                table.addCell(pdfCell("$" + money(invoice.getTotalAmount())));
                table.addCell(pdfCell("$" + money(invoice.getTaxAmount())));
                table.addCell(pdfCell(invoice.getStatus() == null ? "PENDING" : invoice.getStatus().name()));
            }

            document.add(table);
        }

        return out.toByteArray();
    }

    /** Creates a consistent table cell for PDF report rows. */
    private com.itextpdf.layout.element.Cell pdfCell(String value) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(value))
                .setFontSize(10)
                .setPadding(6);
    }

    /** Normalizes nullable money fields to zero for reports and calculations. */
    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Prevents null string values from rendering as literal "null" in reports. */
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean withinRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return true;
        }
        boolean afterStart = from == null || !date.isBefore(from);
        boolean beforeEnd = to == null || !date.isAfter(to);
        return afterStart && beforeEnd;
    }
}
