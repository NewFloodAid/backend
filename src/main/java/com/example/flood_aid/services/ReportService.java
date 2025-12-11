package com.example.flood_aid.services;

import com.example.flood_aid.exceptions.ReportNotFoundException;
import com.example.flood_aid.models.*;
import com.example.flood_aid.repositories.AssistanceTypeRepository;
import com.example.flood_aid.repositories.ImageCategoryRepository;
import com.example.flood_aid.repositories.ReportRepository;
import com.example.flood_aid.repositories.ReportStatusRepository;
import com.example.flood_aid.utils.DateUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class ReportService {
    private UploadService uploadService;
    private ReportRepository reportRepository;
    private AssistanceTypeRepository assistanceTypeRepository;
    private ReportStatusRepository reportStatusRepository;
    private ImageCategoryRepository imageTypeRepository;

    public void setReportStatusForReport(Report report) {
        report.setReportStatus(calculateReportStatus(report));
    }

    @Transactional
    public Report createReport(Report report , Map<String, List<MultipartFile>> imageParams) {
        try {
            setReportAssistancesForReport(report);
            setImagesForReport(report, imageParams, "BEFORE");
            setReportStatusForReport(report);
            return reportRepository.save(report);
        } catch (Exception e) {
            uploadService.deleteImages("images", report.getImages());
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
        boolean isAnyReportAssistanceActive = report.getReportAssistances().stream().anyMatch(ReportAssistance::getIsActive);

        if (report.getReportStatus() == null) {
            return pendingReportStatus;
        }
        if(report.getReportStatus().equals(processedReportStatus) || (report.getReportStatus().equals(successReportStatus) && isAnyReportAssistanceActive) ){
            return processedReportStatus;
        }
        if (report.getReportStatus().equals(processedReportStatus) && !isAnyReportAssistanceActive) {
            return successReportStatus;
        }
        return report.getReportStatus() ;
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
                assistanceLog.setReport(report);

                if (report.getReportAssistanceLogs() == null) {
                    report.setReportAssistanceLogs(new ArrayList<>());
                }
                report.getReportAssistanceLogs().add(assistanceLog);
            }
    }

    private void setImagesForReport(Report report, Map<String, List<MultipartFile>> imageParams) {
        List<Image> images = new ArrayList<>();

        for (java.util.Map.Entry<String, List<MultipartFile>> entry : imageParams.entrySet()) {
            String paramName = entry.getKey();
            List<MultipartFile> files = entry.getValue();

            if (files != null) {
                ImageCategory imageType = imageTypeRepository.findByName(paramName)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid image type: " + paramName));

                if (files.size() > imageType.getFileLimit()) {
                    throw new IllegalArgumentException("Exceeded file limit for " + paramName);
                }

                for (MultipartFile file : files) {
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
                        throw new RuntimeException("Error uploading image to MinIO: " + e.getMessage());
                    }

                    Image image = Image.builder()
                            .report(report)
                            .name(uniqueFileName)
                            .imageCategory(imageType)
                            .phase("BEFORE")
                            .build();
                    images.add(image);
                }
            }
        }

        if (report.getImages() == null) {
            report.setImages(images);
        } else {
            report.getImages().addAll(images);
        }
    }

    private void setImagesForReport(Report report, Map<String, List<MultipartFile>> imageParams, String phase) {
        List<Image> images = new ArrayList<>();

        for (java.util.Map.Entry<String, List<MultipartFile>> entry : imageParams.entrySet()) {
            String paramName = entry.getKey();
            List<MultipartFile> files = entry.getValue();

            if (files != null) {
                ImageCategory imageType = imageTypeRepository.findByName(paramName)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid image type: " + paramName));

                if (files.size() > imageType.getFileLimit()) {
                    throw new IllegalArgumentException("File limit exceeded for image type: " + paramName);
                }

                for (MultipartFile file : files) {
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
                        throw new RuntimeException("Error uploading image to MinIO: " + e.getMessage());
                    }

                    Image image = Image.builder()
                            .report(report)
                            .name(uniqueFileName)
                            .imageCategory(imageType)
                            .phase(phase)
                            .build();
                    images.add(image);
                }
            }
        }

        if (report.getImages() == null) {
            report.setImages(images);
        } else {
            report.getImages().addAll(images);
        }
    }


    public Report updateReport(Report report, Map<String, List<MultipartFile>> imageParams) {
        Optional<Report> optionalReport = reportRepository.findById(report.getId());
        if (optionalReport.isPresent()) {
            Report existingReport = optionalReport.get();

            BeanUtils.copyProperties(report, existingReport, "createdAt", "updatedAt");
            for (ReportAssistance assistance : existingReport.getReportAssistances()) {
                assistance.setReport(existingReport);
            }
            for (Image image : existingReport.getImages()) {
                image.setReport(existingReport);
            }

            // Determine phase for new images
            String phase = "BEFORE";
            if (existingReport.getReportStatus() != null && existingReport.getReportStatus().getStatus() == Status.SENT) {
                phase = "AFTER";
            }
            setImagesForReport(existingReport, imageParams, phase);
            setReportStatusForReport(existingReport);

            return reportRepository.save(existingReport);
        } else {
            throw new IllegalArgumentException("Report not found with ID: " + report.getId());
        }
    }

    public void deleteReportById(Long id) {
        reportRepository.deleteById(id);
    }

    public void getImageURLForReport(Report report){
        for(Image image : report.getImages()){
            image.setUrl(uploadService.getPresignedURL("images", image.getName()));
        }
    }

    public void getImageURLForReports(List<Report> reports){
        for(Report report : reports){
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
        Long assistanceTypeId // back to single
    ) {

        List<Report> reports = reportRepository.findReportsByConditions(userId, startDate, DateUtils.setEndOfDay(endDate));
        reports = reportRepository.filterReportsByLocation(reports, subdistrict, district, province, postalCode);
        reports = reportRepository.filterReportsByStatus(reports, reportStatusId , sourceApp);
        if (assistanceTypeId != null) {
            reports = reportRepository.filterReportsByAssistanceType(reports, assistanceTypeId);
        }
        getImageURLForReports(reports);
        return reports;
    }
}
