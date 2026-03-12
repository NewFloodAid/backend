package com.example.flood_aid.controllers;

import com.example.flood_aid.exceptions.ReportNotFoundException;
import com.example.flood_aid.models.Report;
import com.example.flood_aid.models.dto.AssistanceTopicStatDto;
import com.example.flood_aid.models.dto.PaginatedResponse;
import com.example.flood_aid.services.ReportService;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            @RequestParam(name = "subDistrict", required = false) String subDistrict,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) Long reportStatusId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Long assistanceTypeId,
            @RequestParam(required = false) String keyword) {
        Timestamp startTimestamp = resolveStartTimestamp(startDate);
        Timestamp endTimestamp = resolveEndTimestamp(endDate);
        String resolvedSubdistrict = firstNonBlank(subdistrict, subDistrict);

        List<Report> reports = reportService.filterReports(
                resolvedSubdistrict,
                district,
                province,
                postalCode,
                reportStatusId,
                startTimestamp,
                endTimestamp,
                sourceApp,
                userId,
                assistanceTypeId,
                keyword);

        applyLiffAnonymization(sourceApp, currentUserId, reports);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/filters/paged")
    public ResponseEntity<PaginatedResponse<Report>> filterReportsPaged(
            @RequestHeader(value = "X-Source-App", required = false) String sourceApp,
            @RequestHeader(value = "X-User-Id", required = false) UUID currentUserId,
            @RequestParam(required = false) String subdistrict,
            @RequestParam(name = "subDistrict", required = false) String subDistrict,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) Long reportStatusId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Long assistanceTypeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "8") Integer size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        Timestamp startTimestamp = resolveStartTimestamp(startDate);
        Timestamp endTimestamp = resolveEndTimestamp(endDate);
        String resolvedSubdistrict = firstNonBlank(subdistrict, subDistrict);
        Page<Report> reportPage = reportService.filterReportsPaged(
                resolvedSubdistrict,
                district,
                province,
                postalCode,
                reportStatusId,
                startTimestamp,
                endTimestamp,
                sourceApp,
                userId,
                assistanceTypeId,
                keyword,
                safePage,
                safeSize);

        applyLiffAnonymization(sourceApp, currentUserId, reportPage.getContent());
        return ResponseEntity.ok(PaginatedResponse.from(reportPage));
    }

    @GetMapping("/stats/assistance-top")
    public ResponseEntity<List<AssistanceTopicStatDto>> getTopAssistanceTopicStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "10") Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        Timestamp startTimestamp = startDate != null ? new Timestamp(startDate.getTime()) : null;
        Timestamp endTimestamp = endDate != null ? new Timestamp(endDate.getTime()) : null;

        return ResponseEntity.ok(
                reportService.getTopAssistanceTopicStats(startTimestamp, endTimestamp, safeLimit));
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

    @GetMapping("/{id}/word-with-images")
    public ResponseEntity<byte[]> exportReportImagesWord(@PathVariable Long id) {
        try {
            byte[] word = wordService.exportReportImagesToWord(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + id + "_images.docx")
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(word);
        } catch (ReportNotFoundException e) {
            log.warn("Word with images export failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error exporting Word with images for report {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Timestamp resolveStartTimestamp(Date startDate) {
        return startDate != null
                ? new Timestamp(startDate.getTime())
                : Timestamp.valueOf("1970-01-01 00:00:00");
    }

    private Timestamp resolveEndTimestamp(Date endDate) {
        return endDate != null
                ? new Timestamp(endDate.getTime())
                : Timestamp.valueOf("2100-01-01 00:00:00");
    }

    private void applyLiffAnonymization(String sourceApp, UUID currentUserId, List<Report> reports) {
        if (!"LIFF".equalsIgnoreCase(sourceApp)) {
            return;
        }

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

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback;
    }
}
