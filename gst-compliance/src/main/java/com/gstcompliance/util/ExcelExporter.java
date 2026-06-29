package com.gstcompliance.util;

import com.gstcompliance.dto.response.ReconciliationResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@Component
public class ExcelExporter {

    public ByteArrayInputStream exportReconciliation(ReconciliationResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reconciliation Report");

            // Create header row
            Row header = sheet.createRow(0);
            String[] headers = {"Invoice Number", "Supplier GSTIN", "Type", "Book Amount", "Portal Amount", "Difference", "Description"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            // Add data rows
            int rowNum = 1;
            List<ReconciliationResponse.MismatchDTO> mismatches = response.getMismatches();

            for (ReconciliationResponse.MismatchDTO mismatch : mismatches) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(mismatch.getInvoiceNumber());
                row.createCell(1).setCellValue(mismatch.getSupplierGstin());
                row.createCell(2).setCellValue(mismatch.getStatus());
                row.createCell(3).setCellValue(mismatch.getBookAmount() != null ?
                        mismatch.getBookAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(mismatch.getPortalAmount() != null ?
                        mismatch.getPortalAmount().doubleValue() : 0);
                row.createCell(5).setCellValue(mismatch.getDiffAmount() != null ?
                        mismatch.getDiffAmount().doubleValue() : 0);
                row.createCell(6).setCellValue(mismatch.getDescription());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            log.error("Excel export failed: {}", e.getMessage(), e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}