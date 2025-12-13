package com.example.flood_aid.controllers;

import com.example.flood_aid.models.Report;
import com.example.flood_aid.services.ExcelService;
import com.example.flood_aid.services.ReportService;

import com.example.flood_aid.exceptions.ReportNotFoundException;
import org.springframework.http.HttpHeaders;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Array;
import java.sql.Timestamp;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@AllArgsConstructor
@Slf4j
public class ReportController {

    private ReportService reportService;

    private com.example.flood_aid.services.WordService wordService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Report> createReport(
            @RequestParam("report") String reportString,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        Gson gson = new Gson();
        Report report = gson.fromJson(reportString, Report.class);

        Map<String, List<MultipartFile>> imageParams = Map.of(
                "files", files != null ? files : List.of());

        Report createdReport = reportService.createReport(report, imageParams);

        return ResponseEntity.ok(createdReport);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Report> updateReport(
            @RequestParam("report") String reportString,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        Gson gson = new Gson();
        Report report = gson.fromJson(reportString, Report.class);
        Map<String, List<MultipartFile>> imageParams = Map.of(
                "files", files != null ? files : List.of());

        return ResponseEntity.ok(reportService.updateReport(report, imageParams));
    }

    @GetMapping("/filters")
    public ResponseEntity<List<Report>> filterReports(
            @RequestHeader(value = "X-Source-App", required = false) String sourceApp,
            @RequestHeader(value = "X-User-Id", required = false) UUID currentUserId,
            @RequestParam(required = false) String subdistrict,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) Long reportStatusId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Long assistanceTypeId) {
        Timestamp startTimestamp = (startDate != null) ? new Timestamp(startDate.getTime())
                : Timestamp.valueOf("1970-01-01 00:00:00");
        ;
        Timestamp endTimestamp = (endDate != null) ? new Timestamp(endDate.getTime())
                : Timestamp.valueOf("2100-01-01 00:00:00");
        List<Report> reports = reportService.filterReports(
                subdistrict, district, province, postalCode, reportStatusId, startTimestamp, endTimestamp, sourceApp,
                userId, assistanceTypeId);

        if ("LIFF".equalsIgnoreCase(sourceApp)) {
            for (Report report : reports) {

                boolean isAnonymous = Boolean.TRUE.equals(report.getIsAnonymous());
                boolean isOwner = currentUserId != null && currentUserId.equals(report.getUserId());

                if (isAnonymous && !isOwner) {
                    report.setFirstName("ไม่ระบุตัวตน");
                    report.setLastName("");
                    report.setMainPhoneNumber("");
                    report.setReservePhoneNumber("");
                }
            }
        }

        return ResponseEntity.ok(reports);
    }

    @DeleteMapping("/{id}")
    public void deleteReportById(@PathVariable Long id) {
        reportService.deleteReportById(id);
    }

    @GetMapping("/{id}/word")
    public ResponseEntity<byte[]> exportReportWord(@PathVariable Long id) {
        try {
            byte[] word = wordService.exportReportToWord(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + id + ".docx")
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(word);
        } catch (ReportNotFoundException e) {
            log.warn("Word export failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error exporting Word for report {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
