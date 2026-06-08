package com.example.flood_aid.services;

import com.example.flood_aid.models.*;
import com.example.flood_aid.models.dto.AssistanceTopicStatDto;
import com.example.flood_aid.repositories.AssistanceTypeRepository;
import com.example.flood_aid.repositories.ImageRepository;
import com.example.flood_aid.repositories.ReportRepository;
import com.example.flood_aid.repositories.ReportStatusRepository;
import com.example.flood_aid.repositories.specifications.ReportSpecifications;
import com.example.flood_aid.utils.DateUtils;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReportService {
    private static final int MAX_FILES_PER_BATCH = 4;

    private UploadService uploadService;
    private ReportRepository reportRepository;
    private ImageRepository imageRepository;
    private AssistanceTypeRepository assistanceTypeRepository;
    private ReportStatusRepository reportStatusRepository;
    private DistrictResolutionService districtResolutionService;

    public void setReportStatusForReport(Report report) {
        report.setReportStatus(calculateReportStatus(report));
    }

    @Transactional
    public Report createReport(Report report, Map<String, List<MultipartFile>> imageParams) {
        try {
            setReportAssistancesForReport(report);
            setImagesForReport(report, imageParams, "BEFORE");
            setReportStatusForReport(report);

            // Auto-resolve district from coordinates
            if (report.getLocation() != null && report.getLocation().getLatitude() != null
                    && report.getLocation().getLongitude() != null) {
                District district = districtResolutionService.resolveDistrict(
                        report.getLocation().getLatitude(), report.getLocation().getLongitude());
                if (district != null) {
                    report.setDistrict(district);
                    report.getLocation().setDistrictEntity(district);
                }
            }

            Report savedReport = reportRepository.save(report);
            getImageURLForReport(savedReport);
            return savedReport;
        } catch (Exception e) {
            try {
                uploadService.deleteImages("images", report.getImages());
            } catch (RuntimeException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw new RuntimeException("Failed to save report: " + e.getMessage(), e);
        }
    }

    private ReportStatus calculateReportStatus(Report report) {
        ReportStatus pendingReportStatus = reportStatusRepository.findByStatus(Status.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("ReportStatus with status 'PENDING' not found"));
        ReportStatus processedReportStatus = reportStatusRepository.findByStatus(Status.PROCESS)
                .orElseThrow(() -> new IllegalArgumentException("ReportStatus with status 'PROCESS' not found"));
        ReportStatus successReportStatus = reportStatusRepository.findByStatus(Status.SUCCESS)
                .orElseThrow(() -> new IllegalArgumentException("ReportStatus with status 'SUCCESS' not found"));
        boolean isAnyReportAssistanceActive = report.getReportAssistances().stream()
                .anyMatch(ReportAssistance::getIsActive);

        if (report.getReportStatus() == null) {
            return pendingReportStatus;
        }
        if (report.getReportStatus().equals(processedReportStatus)
                || (report.getReportStatus().equals(successReportStatus) && isAnyReportAssistanceActive)) {
            return processedReportStatus;
        }
        if (report.getReportStatus().equals(processedReportStatus) && !isAnyReportAssistanceActive) {
            return successReportStatus;
        }
        return report.getReportStatus();
    }

    private void setReportAssistancesForReport(Report report) {
        for (ReportAssistance assistance : report.getReportAssistances()) {
            assistance.setReport(report);
            AssistanceType assistanceType = assistanceTypeRepository.findById(assistance.getAssistanceType().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid AssistanceType ID"));
            assistance.setAssistanceType(assistanceType);

            ReportAssistanceLog assistanceLog = new ReportAssistanceLog();
            assistanceLog.setAssistanceType(assistance.getAssistanceType());
            assistanceLog.setQuantity(assistance.getQuantity());
            assistanceLog.setIsActive(assistance.getIsActive());
            assistanceLog.setExtraDetail(assistance.getExtraDetail());
            assistanceLog.setReport(report);

            if (report.getReportAssistanceLogs() == null) {
                report.setReportAssistanceLogs(new ArrayList<>());
            }
            report.getReportAssistanceLogs().add(assistanceLog);
        }
    }

    private void setImagesForReport(Report report, Map<String, List<MultipartFile>> imageParams, String phase) {
        List<Image> images = new ArrayList<>();

        for (java.util.Map.Entry<String, List<MultipartFile>> entry : imageParams.entrySet()) {
            List<MultipartFile> files = entry.getValue();

            if (files != null) {
                if (files.size() > MAX_FILES_PER_BATCH) {
                    throw new IllegalArgumentException("File limit exceeded. Maximum " + MAX_FILES_PER_BATCH + " files.");
                }

                List<Image> uploadedBatchImages = Collections.synchronizedList(new ArrayList<>());
                try {
                    files.parallelStream().forEach(file -> uploadedBatchImages.add(uploadImageFile(report, file, phase)));
                } catch (RuntimeException e) {
                    try {
                        uploadService.deleteImages("images", uploadedBatchImages);
                    } catch (RuntimeException cleanupException) {
                        e.addSuppressed(cleanupException);
                    }
                    throw e;
                }

                images.addAll(uploadedBatchImages);
            }
        }

        if (report.getImages() == null) {
            report.setImages(images);
        } else {
            report.getImages().addAll(images);
        }
    }

    private Image uploadImageFile(Report report, MultipartFile file, String phase) {
        String bucketName = "images";
        String originalFileName = file.getOriginalFilename();
        String extension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID() + extension;
        try (InputStream fileStream = file.getInputStream()) {
            uploadService.putObject(bucketName, uniqueFileName, fileStream, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Error uploading image to Cloudinary: " + e.getMessage(), e);
        }

        return Image.builder()
                .report(report)
                .name(uniqueFileName)
                .phase(phase)
                .build();
    }

    public Report updateReport(Report report, Map<String, List<MultipartFile>> imageParams) {
        Optional<Report> optionalReport = reportRepository.findById(report.getId());
        if (optionalReport.isPresent()) {
            Report existingReport = optionalReport.get();

            Status oldStatus = existingReport.getReportStatus() != null
                    ? existingReport.getReportStatus().getStatus()
                    : null;

            // Manually map fields to avoid null overwriting and collection detachment
            if (report.getFirstName() != null) existingReport.setFirstName(report.getFirstName());
            if (report.getLastName() != null) existingReport.setLastName(report.getLastName());
            if (report.getMainPhoneNumber() != null) existingReport.setMainPhoneNumber(report.getMainPhoneNumber());
            if (report.getReservePhoneNumber() != null) existingReport.setReservePhoneNumber(report.getReservePhoneNumber());
            if (report.getIsAnonymous() != null) existingReport.setIsAnonymous(report.getIsAnonymous());
            if (report.getAdditionalDetail() != null) existingReport.setAdditionalDetail(report.getAdditionalDetail());
            if (report.getAfterAdditionalDetail() != null) existingReport.setAfterAdditionalDetail(report.getAfterAdditionalDetail());
            if (report.getReportStatus() != null) existingReport.setReportStatus(report.getReportStatus());

            if (report.getLocation() != null) {
                if (existingReport.getLocation() == null) {
                    existingReport.setLocation(report.getLocation());
                } else {
                    Location eLoc = existingReport.getLocation();
                    Location nLoc = report.getLocation();
                    if (nLoc.getLatitude() != null) eLoc.setLatitude(nLoc.getLatitude());
                    if (nLoc.getLongitude() != null) eLoc.setLongitude(nLoc.getLongitude());
                    if (nLoc.getAddress() != null) eLoc.setAddress(nLoc.getAddress());
                    if (nLoc.getSubDistrict() != null) eLoc.setSubDistrict(nLoc.getSubDistrict());
                    if (nLoc.getDistrict() != null) eLoc.setDistrict(nLoc.getDistrict());
                    if (nLoc.getProvince() != null) eLoc.setProvince(nLoc.getProvince());
                    if (nLoc.getPostalCode() != null) eLoc.setPostalCode(nLoc.getPostalCode());
                }
            }

            if (report.getReportAssistances() != null) {
                Map<Long, ReportAssistance> existingAssistances = existingReport.getReportAssistances().stream()
                        .filter(ra -> ra.getAssistanceType() != null)
                        .collect(Collectors.toMap(ra -> ra.getAssistanceType().getId(), Function.identity(), (first, second) -> first));

                for (ReportAssistance incoming : report.getReportAssistances()) {
                    if (incoming.getAssistanceType() == null || incoming.getAssistanceType().getId() == null) continue;
                    Long typeId = incoming.getAssistanceType().getId();
                    if (existingAssistances.containsKey(typeId)) {
                        ReportAssistance existing = existingAssistances.get(typeId);
                        existing.setQuantity(incoming.getQuantity());
                        if (incoming.getIsActive() != null) existing.setIsActive(incoming.getIsActive());
                        if (incoming.getExtraDetail() != null) existing.setExtraDetail(incoming.getExtraDetail());
                    } else {
                        incoming.setReport(existingReport);
                        existingReport.getReportAssistances().add(incoming);
                    }
                }
            }

            // Determine phase for new images from the previous status.
            // This allows updating SENT -> SUCCESS and uploading AFTER images in the same request.
            String phase = "BEFORE";
            if (oldStatus == Status.SENT || oldStatus == Status.SUCCESS) {
                phase = "AFTER";
            }
            setImagesForReport(existingReport, imageParams, phase);
            setReportStatusForReport(existingReport);

            // Set status timestamps based on new status
            Status newStatus = existingReport.getReportStatus() != null
                    ? existingReport.getReportStatus().getStatus()
                    : null;
            if (newStatus != null && newStatus != oldStatus) {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                if (newStatus == Status.PROCESS && existingReport.getProcessedAt() == null) {
                    existingReport.setProcessedAt(now);
                }
                if (newStatus == Status.SENT && existingReport.getSentAt() == null) {
                    existingReport.setSentAt(now);
                }
            } else if (oldStatus == Status.PENDING && newStatus == Status.PENDING) {
                existingReport.setEditedAt(Timestamp.valueOf(LocalDateTime.now()));
            }

            Report savedReport = reportRepository.save(existingReport);
            getImageURLForReport(savedReport);
            return savedReport;
        } else {
            throw new IllegalArgumentException("Report not found with ID: " + report.getId());
        }
    }

    @Transactional
    public void deleteReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + id));
        List<Image> reportImages = imageRepository.findAllByReportIds(List.of(id));
        uploadService.deleteImages("images", reportImages);
        reportRepository.delete(report);
    }

    public void getImageURLForReport(Report report) {
        if (report.getImages() == null) {
            report.setImages(Collections.emptyList());
            return;
        }
        for (Image image : report.getImages()) {
            image.setUrl(uploadService.getPresignedURL("images", image.getName()));
        }
    }

    public void getImageURLForReports(List<Report> reports) {
        for (Report report : reports) {
            getImageURLForReport(report);
        }
    }

    public List<Report> filterReports(
            String subdistrict,
            String district,
            String province,
            String postalCode,
            Long reportStatusId,
            Timestamp startDate,
            Timestamp endDate,
            String sourceApp,
            UUID userId,
            Long assistanceTypeId,
            String keyword,
            List<Long> districtIds
    ) {
        Specification<Report> specification = ReportSpecifications.withFilters(
                subdistrict,
                district,
                province,
                postalCode,
                reportStatusId,
                startDate,
                DateUtils.setEndOfDay(endDate),
                userId,
                assistanceTypeId,
                keyword,
                districtIds);

        List<Report> reports = reportRepository.findAll(specification, getSortBySourceApp(sourceApp));
        reports = hydrateReports(reports);
        if (reports.isEmpty()) {
            return reports;
        }

        getImageURLForReports(reports);
        return reports;
    }

    // Backward-compatible overload without districtIds
    public List<Report> filterReports(
            String subdistrict,
            String district,
            String province,
            String postalCode,
            Long reportStatusId,
            Timestamp startDate,
            Timestamp endDate,
            String sourceApp,
            UUID userId,
            Long assistanceTypeId,
            String keyword
    ) {
        return filterReports(subdistrict, district, province, postalCode,
                reportStatusId, startDate, endDate, sourceApp, userId,
                assistanceTypeId, keyword, null);
    }

    public Page<Report> filterReportsPaged(
            String subdistrict,
            String district,
            String province,
            String postalCode,
            Long reportStatusId,
            Timestamp startDate,
            Timestamp endDate,
            String sourceApp,
            UUID userId,
            Long assistanceTypeId,
            String keyword,
            List<Long> districtIds,
            int page,
            int size) {
        Specification<Report> specification = ReportSpecifications.withFilters(
                subdistrict,
                district,
                province,
                postalCode,
                reportStatusId,
                startDate,
                DateUtils.setEndOfDay(endDate),
                userId,
                assistanceTypeId,
                keyword,
                districtIds);

        Pageable pageable = PageRequest.of(page, size, getSortBySourceApp(sourceApp));
        Page<Report> reportsPage = reportRepository.findAll(specification, pageable);
        List<Report> hydratedReports = hydrateReports(reportsPage.getContent());

        if (!hydratedReports.isEmpty()) {
            getImageURLForReports(hydratedReports);
        }

        return new PageImpl<>(hydratedReports, pageable, reportsPage.getTotalElements());
    }

    // Backward-compatible overload without districtIds
    public Page<Report> filterReportsPaged(
            String subdistrict,
            String district,
            String province,
            String postalCode,
            Long reportStatusId,
            Timestamp startDate,
            Timestamp endDate,
            String sourceApp,
            UUID userId,
            Long assistanceTypeId,
            String keyword,
            int page,
            int size) {
        return filterReportsPaged(subdistrict, district, province, postalCode,
                reportStatusId, startDate, endDate, sourceApp, userId,
                assistanceTypeId, keyword, null, page, size);
    }

    public List<AssistanceTopicStatDto> getTopAssistanceTopicStats(
            Timestamp startDate,
            Timestamp endDate,
            int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        Timestamp resolvedStartDate = startDate != null
                ? startDate
                : Timestamp.valueOf("1970-01-01 00:00:00");
        Timestamp resolvedEndDate = endDate != null
                ? DateUtils.setEndOfDay(endDate)
                : Timestamp.valueOf("2100-01-01 00:00:00");

        return reportRepository.findTopAssistanceTopicStats(
                resolvedStartDate,
                resolvedEndDate,
                PageRequest.of(0, safeLimit));
    }

    private Sort getSortBySourceApp(String sourceApp) {
        String orderingPath = "LIFF".equalsIgnoreCase(sourceApp)
                ? "reportStatus.userOrderingNumber"
                : "reportStatus.governmentOrderingNumber";

        return Sort.by(
                Sort.Order.asc(orderingPath),
                Sort.Order.desc("createdAt"));
    }

    private List<Report> hydrateReports(List<Report> reports) {
        if (reports == null || reports.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderedIds = reports.stream()
                .map(Report::getId)
                .toList();

        List<Report> hydratedReports = reportRepository.findDetailedReportsByIds(orderedIds);
        Map<Long, Report> hydratedById = hydratedReports.stream()
                .collect(Collectors.toMap(Report::getId, Function.identity(), (first, second) -> first));

        List<Image> allImages = imageRepository.findAllByReportIds(orderedIds);
        Map<Long, List<Image>> imagesByReportId = allImages.stream()
                .collect(Collectors.groupingBy(Image::getReportId));

        List<Report> orderedHydratedReports = new ArrayList<>(orderedIds.size());
        for (Long id : orderedIds) {
            Report hydratedReport = hydratedById.get(id);
            if (hydratedReport != null) {
                hydratedReport.setImages(imagesByReportId.getOrDefault(id, Collections.emptyList()));
                orderedHydratedReports.add(hydratedReport);
            }
        }
        return orderedHydratedReports;
    }
}
