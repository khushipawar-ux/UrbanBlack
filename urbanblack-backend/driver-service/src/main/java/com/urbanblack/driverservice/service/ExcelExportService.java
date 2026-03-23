package com.urbanblack.driverservice.service;

import com.urbanblack.driverservice.dto.MonthlyPerformanceReportDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportMonthlyReport(List<MonthlyPerformanceReportDTO> data) {
        String[] columns = {"Driver Name", "Emp ID", "Depot Name", "Present Days", "Total Rides", "Total KM", "Avg Rating", "Online Hours", "Estimated Salary"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Monthly Performance Report");

            // Font for header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Header Style
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Row for Header
            Row headerRow = sheet.createRow(0);

            // Header Cells
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Data Rows
            int rowIdx = 1;
            for (MonthlyPerformanceReportDTO dto : data) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(dto.getDriverName() != null ? dto.getDriverName() : "");
                row.createCell(1).setCellValue(dto.getEmpId() != null ? dto.getEmpId() : "");
                row.createCell(2).setCellValue(dto.getDepotName() != null ? dto.getDepotName() : "");
                row.createCell(3).setCellValue(dto.getPresentDays() != null ? dto.getPresentDays() : 0);
                row.createCell(4).setCellValue(dto.getTotalRides() != null ? dto.getTotalRides() : 0);
                row.createCell(5).setCellValue(dto.getTotalKm() != null ? dto.getTotalKm() : 0.0);
                row.createCell(6).setCellValue(dto.getAvgRating() != null ? dto.getAvgRating() : 0.0);
                row.createCell(7).setCellValue(dto.getOnlineHours() != null ? dto.getOnlineHours() : 0.0);
                row.createCell(8).setCellValue(dto.getEstimatedSalary() != null ? dto.getEstimatedSalary() : 0.0);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Fail to import data to Excel file: " + e.getMessage());
        }
    }
}
