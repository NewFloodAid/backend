package com.example.flood_aid.services;

import com.example.flood_aid.models.Report;
import com.example.flood_aid.models.Status;
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
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ExcelService {
    private final ReportRepository reportRepository;
    private static final String DATE_FORMAT = "dd/MM/yy HH:mm:ss";
    private static final String[] HEADERS = {
            "วันที่-เวลาที่แจ้งเข้ามา", "วันที่-เวลาที่รับเรื่อง", "วันที่-เวลาที่ส่งเรื่องต่อ",
            "วันที่-เวลาที่แจ้งว่าเสร็จสิ้น",
            "ชื่อ", "นามสกุล", "เบอร์โทรหลัก", "เบอร์โทรสำรอง",
            "รายละเอียดเพิ่มเติม", "รายละเอียดหลังดำเนินการ",
            "ที่อยู่", "ตำบล", "อำเภอ", "จังหวัด", "รหัสไปรษณีย์",
            "ประเภทของการแจ้งเหตุ"
    };

    public byte[] exportReportsToExcel(Timestamp startDate, Timestamp endDate, UUID userId) throws IOException {
        log.info("Starting Excel export - startDate: {}, endDate: {}, userId: {}", startDate, endDate, userId);

        List<Report> reports = reportRepository.findReportsByConditions(userId, startDate, endDate);
        log.info("Found {} reports for export", reports.size());

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

        int rowNum = 1;
        for (Report report : reports) {
            Row row = sheet.createRow(rowNum++);
            populateReportRow(row, report, dateStyle, commonStyle);
        }
        autoSizeColumns(sheet);
    }

    private void populateReportRow(Row row, Report report, CellStyle dateStyle, CellStyle commonStyle) {
        log.info("Populating row for report ID: {}, Name: {} {}", report.getId(), report.getFirstName(),
                report.getLastName());

        // Column 0: วันที่-เวลาที่แจ้งเข้ามา
        // If status is PENDING and editedAt exists, use editedAt (user edited before
        // acceptance)
        // Otherwise use createdAt
        Timestamp reportedDate = report.getCreatedAt();
        if (report.getReportStatus() != null
                && report.getReportStatus().getStatus() == Status.PENDING
                && report.getEditedAt() != null) {
            reportedDate = report.getEditedAt();
        }
        setCellDate(row, 0, reportedDate, dateStyle);

        // Column 1: วันที่-เวลาที่รับเรื่อง (processedAt)
        setCellDate(row, 1, report.getProcessedAt(), dateStyle);

        // Column 2: วันที่-เวลาที่ส่งเรื่องต่อ (sentAt)
        setCellDate(row, 2, report.getSentAt(), dateStyle);

        // Column 3: วันที่-เวลาที่แจ้งว่าเสร็จสิ้น (updatedAt, only if status is
        // SUCCESS)
        Timestamp completedDate = null;
        if (report.getReportStatus() != null
                && report.getReportStatus().getStatus() == Status.SUCCESS) {
            completedDate = report.getUpdatedAt();
        }
        setCellDate(row, 3, completedDate, dateStyle);

        // Column 4-5: Name fields
        row.createCell(4).setCellValue(report.getFirstName() != null ? report.getFirstName() : "");
        row.createCell(5).setCellValue(report.getLastName() != null ? report.getLastName() : "");

        // Column 6-7: Phone numbers
        row.createCell(6).setCellValue(report.getMainPhoneNumber() != null ? report.getMainPhoneNumber() : "");
        row.createCell(7).setCellValue(report.getReservePhoneNumber() != null ? report.getReservePhoneNumber() : "");

        // Column 8-9: Details
        row.createCell(8).setCellValue(report.getAdditionalDetail() != null ? report.getAdditionalDetail() : "");
        row.createCell(9)
                .setCellValue(report.getAfterAdditionalDetail() != null ? report.getAfterAdditionalDetail() : "");

        // Column 10-14: Location fields
        if (report.getLocation() != null) {
            row.createCell(10)
                    .setCellValue(report.getLocation().getAddress() != null ? report.getLocation().getAddress() : "");
            row.createCell(11).setCellValue(
                    report.getLocation().getSubDistrict() != null ? report.getLocation().getSubDistrict() : "");
            row.createCell(12)
                    .setCellValue(report.getLocation().getDistrict() != null ? report.getLocation().getDistrict() : "");
            row.createCell(13)
                    .setCellValue(report.getLocation().getProvince() != null ? report.getLocation().getProvince() : "");
            row.createCell(14).setCellValue(
                    report.getLocation().getPostalCode() != null ? report.getLocation().getPostalCode() : "");
        } else {
            log.warn("Location is null for report ID: {}", report.getId());
            for (int i = 10; i <= 14; i++) {
                row.createCell(i).setCellValue("");
            }
        }

        // Column 15: ประเภทของการแจ้งเหตุ (assistance type names, comma-separated)
        if (report.getReportAssistances() != null && !report.getReportAssistances().isEmpty()) {
            String assistanceNames = report.getReportAssistances().stream()
                    .filter(a -> a.getAssistanceType() != null && a.getAssistanceType().getName() != null
                            && a.getQuantity() != null && a.getQuantity() == 1)
                    .map(a -> a.getAssistanceType().getName())
                    .collect(Collectors.joining(", "));
            row.createCell(15).setCellValue(assistanceNames);
        } else {
            row.createCell(15).setCellValue("");
        }

        // Apply common style to non-date cells (columns 4 onward)
        for (int i = 4; i < HEADERS.length; i++) {
            if (row.getCell(i) != null) {
                row.getCell(i).setCellStyle(commonStyle);
            }
        }
    }

    private void setCellDate(Row row, int colIndex, Timestamp timestamp, CellStyle dateStyle) {
        Cell cell = row.createCell(colIndex);
        if (timestamp != null) {
            cell.setCellValue(Date.from(timestamp.toInstant()));
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellValue("");
            // Still apply date style for consistent column width
            cell.setCellStyle(dateStyle);
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
}
