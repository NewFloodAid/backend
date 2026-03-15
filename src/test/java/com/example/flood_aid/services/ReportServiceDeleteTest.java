package com.example.flood_aid.services;

import com.example.flood_aid.models.Image;
import com.example.flood_aid.models.Report;
import com.example.flood_aid.repositories.AssistanceTypeRepository;
import com.example.flood_aid.repositories.ImageRepository;
import com.example.flood_aid.repositories.ReportRepository;
import com.example.flood_aid.repositories.ReportStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceDeleteTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private AssistanceTypeRepository assistanceTypeRepository;

    @Mock
    private ReportStatusRepository reportStatusRepository;

    @Test
    void deleteReportByIdDeletesCloudinaryAssetsBeforeRemovingReport() {
        StubUploadService uploadService = new StubUploadService();
        ReportService reportService = new ReportService(
                uploadService,
                reportRepository,
                imageRepository,
                assistanceTypeRepository,
                reportStatusRepository);

        Long reportId = 99L;
        Report report = new Report();
        report.setId(reportId);

        Image image = Image.builder().name("img-1.jpg").build();
        List<Image> images = List.of(image);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(imageRepository.findAllByReportIds(List.of(reportId))).thenReturn(images);

        reportService.deleteReportById(reportId);

        assertEquals("images", uploadService.lastBucketName);
        assertEquals(images, uploadService.lastImages);
        verify(reportRepository).delete(report);
    }

    @Test
    void deleteReportByIdDoesNotDeleteReportWhenCloudinaryCleanupFails() {
        StubUploadService uploadService = new StubUploadService();
        ReportService reportService = new ReportService(
                uploadService,
                reportRepository,
                imageRepository,
                assistanceTypeRepository,
                reportStatusRepository);

        Long reportId = 100L;
        Report report = new Report();
        report.setId(reportId);

        Image image = Image.builder().name("img-2.jpg").build();
        List<Image> images = List.of(image);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(imageRepository.findAllByReportIds(List.of(reportId))).thenReturn(images);
        uploadService.exceptionToThrow = new RuntimeException("Cloudinary cleanup incomplete");

        assertThrows(RuntimeException.class, () -> reportService.deleteReportById(reportId));

        verify(reportRepository, never()).delete(report);
    }

    private static class StubUploadService extends UploadService {
        private RuntimeException exceptionToThrow;
        private String lastBucketName;
        private List<Image> lastImages;

        @Override
        public void deleteImages(String bucketName, List<Image> images) {
            this.lastBucketName = bucketName;
            this.lastImages = images;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
        }
    }
}
