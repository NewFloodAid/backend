package com.example.flood_aid.services;

import com.example.flood_aid.repositories.ReportRepository;
import com.example.flood_aid.models.Report;
import com.example.flood_aid.exceptions.ReportNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordService {

    private final ReportRepository reportRepository;
    private final MapService mapService;

    @Transactional(readOnly = true)
    public byte[] exportReportToWord(Long reportId) throws IOException {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        // Load template
        InputStream templateInputStream = null;
        try {
            log.info("Current Working Directory: {}", System.getProperty("user.dir"));
            ClassPathResource resource = new ClassPathResource("templates/request_form_template.docx");
            log.info("Checking classpath resource: {}", resource.getPath());
            if (resource.exists()) {
                log.info("Found in classpath");
                templateInputStream = resource.getInputStream();
            } else {
                log.info("Not found in classpath");
                // Fallback: try loading from file system (useful for dev env)
                java.io.File file = new java.io.File("src/main/resources/templates/request_form_template.docx");
                log.info("Checking file system: {}", file.getAbsolutePath());
                if (file.exists()) {
                    log.info("Found in file system");
                    templateInputStream = new java.io.FileInputStream(file);
                } else {
                    log.info("Not found in file system");
                    // Try absolute path fallback based on known structure
                    java.io.File absFile = new java.io.File(
                            "c:\\Users\\chaya\\Desktop\\SeniorProject\\backend\\src\\main\\resources\\templates\\request_form_template.docx");
                    log.info("Checking absolute path: {}", absFile.getAbsolutePath());
                    if (absFile.exists()) {
                        log.info("Found in absolute path");
                        templateInputStream = new java.io.FileInputStream(absFile);
                    } else {
                        // Final Fallback: Check project root for the original file name
                        java.io.File rootFile = new java.io.File("แบบคำร้องเทศบาล.docx");
                        log.info("Checking project root: {}", rootFile.getAbsolutePath());
                        if (rootFile.exists()) {
                            log.info("Found in project root");
                            templateInputStream = new java.io.FileInputStream(rootFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load template: {}", e.getMessage());
        }

        if (templateInputStream == null) {
            throw new IOException("Template file not found");
        }

        try (InputStream is = templateInputStream;
                XWPFDocument document = new XWPFDocument(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Generate Map
            byte[] mapBytes = mapService.generateStaticMapWithPin(report.getLocation(), reportId);

            if (mapBytes != null && mapBytes.length > 0) {
                // Add a new paragraph at the end
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setAlignment(ParagraphAlignment.CENTER);
                paragraph.setSpacingBefore(0);
                paragraph.setSpacingAfter(0);
                XWPFRun run = paragraph.createRun();
                // Removed "แผนที่" text and breaks to keep it on the same page/compact

                // Add image
                try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(mapBytes)) {
                    // Reduced size to fit on one page
                    int width = 500;
                    int height = 500;
                    run.addPicture(imageInputStream, XWPFDocument.PICTURE_TYPE_PNG, "map.png", Units.toEMU(width),
                            Units.toEMU(height));
                } catch (Exception e) {
                    log.error("Failed to add map image to Word document", e);
                    run.setText("[Error loading map image]");
                }
            }

            document.write(baos);
            return baos.toByteArray();
        }
    }
}
