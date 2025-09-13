package com.example.flood_aid.controllers;

import com.example.flood_aid.services.ExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@Slf4j
public class ExcelController {
    @Autowired
    ExcelService exportService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReportsToExcel(
            @RequestHeader(value = "X-Source-App", required = false) String sourceApp,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) UUID userId
    ) {
        try {
            log.info("Export request received - startDate: {}, endDate: {}, userId: {}, sourceApp: {}", 
                    startDate, endDate, userId, sourceApp);
            
            // Parse dates manually to avoid issues with @DateTimeFormat
            Timestamp startTimestamp = null;
            Timestamp endTimestamp = null;
            
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    startTimestamp = Timestamp.valueOf(startDate + " 00:00:00");
                    log.info("Parsed startTimestamp: {}", startTimestamp);
                } catch (Exception e) {
                    log.error("Error parsing startDate: {}", startDate, e);
                    startTimestamp = Timestamp.valueOf("1970-01-01 00:00:00");
                }
            } else {
                startTimestamp = Timestamp.valueOf("1970-01-01 00:00:00");
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    endTimestamp = Timestamp.valueOf(endDate + " 23:59:59");
                    log.info("Parsed endTimestamp: {}", endTimestamp);
                } catch (Exception e) {
                    log.error("Error parsing endDate: {}", endDate, e);
                    endTimestamp = Timestamp.valueOf("2100-01-01 23:59:59");
                }
            } else {
                endTimestamp = Timestamp.valueOf("2100-01-01 23:59:59");
            }
            
            log.info("Final timestamps - startTimestamp: {}, endTimestamp: {}", startTimestamp, endTimestamp);
            
            // เรียกใช้ Service เพื่อสร้างไฟล์ Excel
            byte[] excelData = exportService.exportReportsToExcel(
                    startTimestamp, endTimestamp, userId
            );

            log.info("Excel file generated successfully, size: {} bytes", excelData.length);

            // ส่ง Response ให้ดาวน์โหลดไฟล์ Excel
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reports.xlsx; filename*=UTF-8''reports.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (Exception e) {
            log.error("Error generating Excel file", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
