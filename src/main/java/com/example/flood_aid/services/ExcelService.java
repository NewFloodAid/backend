package com.example.flood_aid.services;

import com.example.flood_aid.models.Report;
import com.example.flood_aid.repositories.ReportRepository;
import com.example.flood_aid.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class ExcelService {
    private final ReportRepository reportRepository;
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String[] HEADERS = {
            "วันที่-เวลา", "ชื่อ", "นามสกุล", "เบอร์โทรหลัก", "เบอร์โทรสำรอง",
            "รายละเอียดเพิ่มเติม", "รายละเอียดหลังดำเนินการ", "ที่อยู่", "ตำบล", "อำเภอ", "จังหวัด", "รหัสไปรษณีย์",
            "ตัดหญ้า - ต้นไม้", "ขุดลอกทางระบายน้ำ", "เก็บขยะ", "ซ่อมแซมถนน", "ซ่อมไฟฟ้า", "ซ่อมเสียงตามสาย", "อื่นๆ"
    };

    public byte[] exportReportsToExcel(Timestamp startDate, Timestamp endDate, UUID userId) throws IOException {
        log.info("Starting Excel export - startDate: {}, endDate: {}, userId: {}", startDate, endDate, userId);
        
        // Debug: First check if there are any reports at all
        List<Report> allReports = reportRepository.findAll();
        log.info("Total reports in database: {}", allReports.size());
        for (Report report : allReports) {
            log.info("All report - ID: {}, Name: {} {}, Created: {}, UserId: {}", 
                    report.getId(), report.getFirstName(), report.getLastName(), report.getCreatedAt(), report.getUserId());
        }
        
        List<Report> reports = reportRepository.findReportsByConditions(userId, startDate, endDate);
        log.info("Found {} reports for export", reports.size());
        
        // Debug: Log each report found
        for (Report report : reports) {
            log.info("Report found - ID: {}, Name: {} {}, Created: {}", 
                    report.getId(), report.getFirstName(), report.getLastName(), report.getCreatedAt());
        }
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("รายงานผู้ขอความช่วยเหลือ");
            createHeaderRow(sheet, workbook);
            generateReportRows(sheet, workbook, reports);
            workbook.write(outputStream);
            byte[] result = outputStream.toByteArray();
            log.info("Excel file created successfully, size: {} bytes", result.length);
            return result;
        }
    }

    private void createHeaderRow(Sheet sheet, Workbook workbook) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = getHeaderCellStyle(workbook);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void generateReportRows(Sheet sheet, Workbook workbook, List<Report> reports) {
        CellStyle dateStyle = getDateCellStyle(workbook);
        CellStyle commonStyle = getCommonCellStyle(workbook);

        int[] summaryCounts = new int[7]; // Changed from 5 to 7 to match the number of assistance types
        int rowNum = 1;

        for (Report report : reports) {
            Row row = sheet.createRow(rowNum++);
            populateReportRow(row, report, dateStyle, commonStyle, summaryCounts);
        }
        appendSummaryRows(sheet, rowNum, commonStyle, summaryCounts);
        autoSizeColumns(sheet);
    }

    private void populateReportRow(Row row, Report report, CellStyle dateStyle, CellStyle commonStyle, int[] summaryCounts) {
        log.info("Populating row for report ID: {}, Name: {} {}", report.getId(), report.getFirstName(), report.getLastName());
        
        // Date and time
        row.createCell(0).setCellValue(Date.from(report.getCreatedAt().toInstant()));
        row.getCell(0).setCellStyle(dateStyle);

        // Name fields
        row.createCell(1).setCellValue(report.getFirstName());
        row.createCell(2).setCellValue(report.getLastName());
        
        // Phone numbers
        row.createCell(3).setCellValue(report.getMainPhoneNumber());
        row.createCell(4).setCellValue(report.getReservePhoneNumber());
        
        // Details
        row.createCell(5).setCellValue(report.getAdditionalDetail());
        row.createCell(6).setCellValue(report.getAfterAdditionalDetail());
        
        // Location fields
        if (report.getLocation() != null) {
            row.createCell(7).setCellValue(report.getLocation().getAddress());
            row.createCell(8).setCellValue(report.getLocation().getSubDistrict());
            row.createCell(9).setCellValue(report.getLocation().getDistrict());
            row.createCell(10).setCellValue(report.getLocation().getProvince());
            row.createCell(11).setCellValue(report.getLocation().getPostalCode());
        } else {
            log.warn("Location is null for report ID: {}", report.getId());
        }

        // Assistance types (columns 12-18)
        if (report.getReportAssistances() != null) {
            log.info("Report has {} assistance entries", report.getReportAssistances().size());
            for (var assistance : report.getReportAssistances()) {
                log.info("Assistance - Type ID: {}, Name: {}, Quantity: {}", 
                        assistance.getAssistanceType().getId(), 
                        assistance.getAssistanceType().getName(), 
                        assistance.getQuantity());
                int index = getAssistanceIndex(String.valueOf(assistance.getAssistanceType().getId()));
                if (index != -1) {
                    row.createCell(12 + index).setCellValue(assistance.getQuantity());
                    summaryCounts[index] += assistance.getQuantity();
                } else {
                    log.warn("Unknown assistance type ID: {}", assistance.getAssistanceType().getId());
                }
            }
        } else {
            log.warn("ReportAssistances is null for report ID: {}", report.getId());
        }

        // Apply styles to all cells
        for (int i = 1; i < HEADERS.length; i++) {
            if (row.getCell(i) == null) {
                row.createCell(i).setCellValue("");
            }
            row.getCell(i).setCellStyle(commonStyle);
        }
    }

    private void appendSummaryRows(Sheet sheet, int startRow, CellStyle style, int[] summaryCounts) {
        Row summaryRow = sheet.createRow(startRow);
        Cell summaryLabelCell = summaryRow.createCell(0);
        summaryLabelCell.setCellValue("สรุป");
        summaryLabelCell.setCellStyle(style);

        for (int i = 0; i < summaryCounts.length; i++) {
            Cell summaryCell = summaryRow.createCell(12 + i);
            summaryCell.setCellValue(summaryCounts[i]);
            summaryCell.setCellStyle(style);
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle getHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle getCommonCellStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
    }

    private CellStyle getDateCellStyle(Workbook workbook) {
        CellStyle style = getCommonCellStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat(DATE_FORMAT));
        return style;
    }

    private int getAssistanceIndex(String id) {
        return switch (id) {
            case "1" -> 0; // ตัดหญ้า - ต้นไม้
            case "2" -> 1; // ขุดลอกทางระบายน้ำ
            case "3" -> 2; // เก็บขยะ
            case "4" -> 3; // ซ่อมแซมถนน
            case "5" -> 4; // ซ่อมไฟฟ้า
            case "6" -> 5; // ซ่อมเสียงตามสาย
            case "7" -> 6; // อื่นๆ
            default -> -1;
        };
    }
}
