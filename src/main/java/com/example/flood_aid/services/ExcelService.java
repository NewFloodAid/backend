package com.example.flood_aid.services;

import com.example.flood_aid.models.Report;
import com.example.flood_aid.repositories.ReportRepository;
import com.example.flood_aid.utils.DateUtils;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

@Service
@AllArgsConstructor
public class ExcelService {
    private final ReportRepository reportRepository;
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String[] HEADERS = {
            "วันที่-เวลา", "ชื่อ", "ระดับความรุนแรง", "ผู้บาดเจ็บหนัก(คน)", "ผู้บาดเจ็บ(คน)",
            "ต้องการขนย้ายผู้ป่วยติดเตียง(คน)", "ต้องการขนย้ายผู้สูงอายุ(คน)", "ต้องการอาหาร น้ำดื่ม(ชุด)",
            "รายละเอียด", "ที่อยู่", "ตำบล", "อำเภอ", "จังหวัด", "เบอร์โทร"
    };

    public byte[] exportReportsToExcel(Timestamp startDate, Timestamp endDate, UUID userId) throws IOException {
        List<Report> reports = reportRepository.findReportsByConditions(userId, startDate, DateUtils.setEndOfDay(endDate), false);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("รายงานผู้ขอความช่วยเหลือ");
            createHeaderRow(sheet, workbook);
            generateReportRows(sheet, workbook, reports);
            workbook.write(outputStream);
            return outputStream.toByteArray();
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

        int[] summaryCounts = new int[5];
        int rowNum = 1;

        for (Report report : reports) {
            Row row = sheet.createRow(rowNum++);
            populateReportRow(row, report, dateStyle, commonStyle, summaryCounts);
        }
        appendSummaryRows(sheet, rowNum, commonStyle, summaryCounts);
        autoSizeColumns(sheet);
    }

    private void populateReportRow(Row row, Report report, CellStyle dateStyle, CellStyle commonStyle, int[] summaryCounts) {
        row.createCell(0).setCellValue(Date.from(report.getCreatedAt().toInstant()));
        row.getCell(0).setCellStyle(dateStyle);

        row.createCell(1).setCellValue(report.getFirstName() + " " + report.getLastName());

        if (report.getReportAssistances() != null) {
            for (var assistance : report.getReportAssistances()) {
                int index = getAssistanceIndex(String.valueOf(assistance.getAssistanceType().getId()));
                if (index != -1) {
                    row.createCell(3 + index).setCellValue(assistance.getQuantity());
                    summaryCounts[index] += assistance.getQuantity();
                }
            }
        }

        row.createCell(8).setCellValue(report.getAdditionalDetail());
        row.createCell(9).setCellValue(report.getLocation().getAddress());
        row.createCell(10).setCellValue(report.getLocation().getSubDistrict());
        row.createCell(11).setCellValue(report.getLocation().getDistrict());
        row.createCell(12).setCellValue(report.getLocation().getProvince());
        row.createCell(13).setCellValue(report.getMainPhoneNumber());

        for (int i = 1; i < HEADERS.length; i++) {
            if (row.getCell(i) == null) {
                row.createCell(i).setCellValue("0");
            }
            row.getCell(i).setCellStyle(commonStyle);
        }
    }

    private void appendSummaryRows(Sheet sheet, int startRow, CellStyle style, int[] summaryCounts) {
        Row priorityRow = sheet.createRow(startRow);
        Cell priorityLabelCell = priorityRow.createCell(0);
        priorityLabelCell.setCellValue("สรุป");
        priorityLabelCell.setCellStyle(style);

        for (int i = 0; i < summaryCounts.length; i++) {
            Cell labelCell = priorityRow.createCell(3+i);
            labelCell.setCellValue(summaryCounts[i]);
            labelCell.setCellStyle(style);
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
            case "1" -> 0; case "2" -> 1; case "3" -> 2; case "4" -> 3; case "5" -> 4;
            default -> -1;
        };
    }
}
