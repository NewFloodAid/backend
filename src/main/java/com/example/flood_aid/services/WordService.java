package com.example.flood_aid.services;

import com.example.flood_aid.repositories.ReportRepository;
import com.example.flood_aid.models.Image;
import com.example.flood_aid.models.Report;
import com.example.flood_aid.exceptions.ReportNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRelation;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.xmlbeans.XmlCursor;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

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
    private final UploadService uploadService;

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

            // Find the target paragraph to insert after
            XWPFParagraph targetParagraph = null;
            for (XWPFParagraph p : document.getParagraphs()) {
                // Look for a unique identifier in the text "นายสมพงษ์"
                if (p.getText() != null && p.getText().contains("นายสมพงษ์")) {
                    targetParagraph = p;
                    break;
                }
            }

            if (targetParagraph != null) {
                // Prepare dynamic data
                String assistanceType = "";
                if (report.getReportAssistances() != null && !report.getReportAssistances().isEmpty()) {
                    assistanceType = report.getReportAssistances().stream()
                            .filter(ra -> Boolean.TRUE.equals(ra.getIsActive()))
                            .map(ra -> ra.getAssistanceType().getName())
                            .collect(Collectors.joining(", "));
                } else {
                    assistanceType = "-";
                }

                String additionalDetail = report.getAdditionalDetail() != null ? report.getAdditionalDetail() : "-";

                String address = "-";
                if (report.getLocation() != null && report.getLocation().getAddress() != null) {
                    address = report.getLocation().getAddress();
                }

                String dynamicText = String.format("%s โดยมีรายละเอียด เพิ่มเติมคือ %s ณ บริเวณบ้านเลขที่ %s",
                        assistanceType, additionalDetail, address);

                // Insert new paragraph after target
                XmlCursor cursor = targetParagraph.getCTP().newCursor();
                cursor.toNextSibling(); // Move cursor to the next position (after current paragraph)

                XWPFParagraph newParagraph = document.insertNewParagraph(cursor);
                // Copy style from target paragraph if desired, or set standard
                newParagraph.setAlignment(ParagraphAlignment.BOTH); // Or matches template

                // Indentation (roughly matching standard Thai format if needed, or just
                // default)
                // newParagraph.setFirstLineIndent(720); // Example indent

                XWPFRun newRun = newParagraph.createRun();
                // Set font family to match template if possible, usually "TH SarabunPSK" or
                // similar
                // But without knowing exact font, we leave default or try to match
                newRun.setFontFamily("TH SarabunPSK");
                newRun.setFontSize(16); // Standard size for Thai official docs
                newRun.setText(dynamicText);
            }

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

                // Add Google Maps Link
                // Add Google Maps Link
                if (report.getLocation() != null && report.getLocation().getLatitude() != null
                        && report.getLocation().getLongitude() != null) {
                    XWPFParagraph linkParagraph = document.createParagraph();
                    linkParagraph.setAlignment(ParagraphAlignment.CENTER);

                    String googleMapUrl = String.format("https://www.google.com/maps/search/?api=1&query=%s,%s",
                            report.getLocation().getLatitude(), report.getLocation().getLongitude());
                    String linkText = "Google Map: " + googleMapUrl;

                    try {
                        String rId = document.getPackagePart()
                                .addExternalRelationship(googleMapUrl, XWPFRelation.HYPERLINK.getRelation()).getId();
                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink cthyperLink = linkParagraph
                                .getCTP().addNewHyperlink();
                        cthyperLink.setId(rId);

                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR ctr = cthyperLink.addNewR();
                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText ctText = ctr.addNewT();
                        ctText.setStringValue(linkText);

                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rPr = ctr.addNewRPr();
                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor color = rPr.addNewColor();
                        color.setVal("0000FF");

                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTUnderline underline = rPr.addNewU();
                        underline.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);

                        // Set font
                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts fonts = rPr.addNewRFonts();
                        fonts.setAscii("TH SarabunPSK");
                        fonts.setHAnsi("TH SarabunPSK");
                        fonts.setCs("TH SarabunPSK");

                    } catch (Exception e) {
                        log.error("Failed to create hyperlink", e);
                        XWPFRun linkRun = linkParagraph.createRun();
                        linkRun.setText(linkText);
                    }
                }
            }

            document.write(baos);
            return baos.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportReportImagesToWord(Long reportId) throws IOException {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        List<Image> images = report.getImages();
        if (images == null || images.isEmpty()) {
            throw new IOException("No images found for report " + reportId);
        }

        // Group images by phase
        Map<String, List<Image>> imagesByPhase = images.stream()
                .collect(Collectors.groupingBy(img -> img.getPhase() != null ? img.getPhase() : "UNKNOWN"));

        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Add title
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("TH SarabunPSK");
            titleRun.setText("รูปภาพประกอบรายงาน #" + reportId);
            titleRun.addBreak();

            // Process images by phase
            for (Map.Entry<String, List<Image>> entry : imagesByPhase.entrySet()) {
                String phase = entry.getKey();
                List<Image> phaseImages = entry.getValue();

                // Add phase header
                XWPFParagraph phaseParagraph = document.createParagraph();
                phaseParagraph.setAlignment(ParagraphAlignment.LEFT);
                XWPFRun phaseRun = phaseParagraph.createRun();
                phaseRun.setBold(true);
                phaseRun.setFontSize(16);
                phaseRun.setFontFamily("TH SarabunPSK");
                String phaseLabel = "BEFORE".equals(phase) ? "ภาพก่อนดำเนินการ"
                        : "AFTER".equals(phase) ? "ภาพหลังดำเนินการ" : phase;
                phaseRun.setText(phaseLabel);
                phaseRun.addBreak();

                // Add each image
                for (Image image : phaseImages) {
                    try {
                        byte[] imageBytes = uploadService.getObject("images", image.getName());
                        if (imageBytes != null && imageBytes.length > 0) {
                            XWPFParagraph imageParagraph = document.createParagraph();
                            imageParagraph.setAlignment(ParagraphAlignment.CENTER);
                            XWPFRun imageRun = imageParagraph.createRun();

                            // Determine image type from filename
                            int pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
                            String imageName = image.getName().toLowerCase();
                            if (imageName.endsWith(".png")) {
                                pictureType = XWPFDocument.PICTURE_TYPE_PNG;
                            } else if (imageName.endsWith(".gif")) {
                                pictureType = XWPFDocument.PICTURE_TYPE_GIF;
                            }

                            try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
                                // Scale image to fit page width (max ~450 points width)
                                int width = 400;
                                int height = 300;
                                imageRun.addPicture(imageInputStream, pictureType, image.getName(),
                                        Units.toEMU(width), Units.toEMU(height));
                            }

                            // Add image caption
                            XWPFParagraph captionParagraph = document.createParagraph();
                            captionParagraph.setAlignment(ParagraphAlignment.CENTER);
                            XWPFRun captionRun = captionParagraph.createRun();
                            captionRun.setFontSize(12);
                            captionRun.setFontFamily("TH SarabunPSK");
                            captionRun.setItalic(true);
                            String categoryName = image.getImageCategory() != null ? image.getImageCategory().getName()
                                    : "รูปภาพ";
                            captionRun.setText(categoryName);
                            captionRun.addBreak();
                        }
                    } catch (Exception e) {
                        log.error("Failed to add image {} to Word document", image.getName(), e);
                        XWPFParagraph errorParagraph = document.createParagraph();
                        XWPFRun errorRun = errorParagraph.createRun();
                        errorRun.setText("[Error loading image: " + image.getName() + "]");
                    }
                }
            }

            document.write(baos);
            return baos.toByteArray();
        }
    }
}
