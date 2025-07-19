package com.example.flood_aid.controllers;

import com.example.flood_aid.services.ExcelService;
import lombok.RequiredArgsConstructor;
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

public class ExcelController {
    @Autowired
    ExcelService exportService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReportsToExcel(
            @RequestHeader(value = "X-Source-App", required = false) String sourceApp,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) UUID userId
    ) {
        try {
            // กำหนดช่วงเวลาให้เป็น Timestamp
            Timestamp startTimestamp = (startDate != null) ? new Timestamp(startDate.getTime()) : Timestamp.valueOf("1970-01-01 00:00:00");
            Timestamp endTimestamp = (endDate != null) ? new Timestamp(endDate.getTime()) : Timestamp.valueOf("2100-01-01 00:00:00");
            // เรียกใช้ Service เพื่อสร้างไฟล์ Excel
            byte[] excelData = exportService.exportReportsToExcel(
                    startTimestamp, endTimestamp, userId
            );

            // ส่ง Response ให้ดาวน์โหลดไฟล์ Excel
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reports.xlsx; filename*=UTF-8''reports.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
