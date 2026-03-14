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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Comparator;
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
    private static final int WORD_IMAGE_MAX_WIDTH = 430;
    private static final int WORD_IMAGE_MAX_HEIGHT = 560;
    private static final int WORD_IMAGE_FALLBACK_WIDTH = 400;
    private static final int WORD_IMAGE_FALLBACK_HEIGHT = 300;
    private static final int WORD_MAP_MAX_WIDTH = 520;
    private static final int WORD_MAP_MAX_HEIGHT = 390;
    private static final int WORD_MAP_FALLBACK_WIDTH = 500;
    private static final int WORD_MAP_FALLBACK_HEIGHT = 375;

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

            // --- Conditional greeting text replacement ---
            String resolvedAssistanceType = buildActiveAssistanceNames(report);

            if (resolvedAssistanceType.contains("ซ่อมไฟฟ้า")) {
                for (XWPFParagraph p : document.getParagraphs()) {
                    for (XWPFRun run : p.getRuns()) {
                        String text = run.getText(0);
                        if (text != null && text.contains("นายกเทศมนตรีเทศบาลนครเชียงใหม่")) {
                            text = text.replace("นายกเทศมนตรีเทศบาลนครเชียงใหม่",
                                    "ผู้อำนวยการส่วนภูมิภาคเขต 1 ภาคเหนือ");
                            run.setText(text, 0);
                        }
                    }
                }
            }
            // --- End conditional greeting ---

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
                String assistanceType = buildActiveAssistanceNames(report);
                if (!hasText(assistanceType)) {
                    assistanceType = "-";
                }

                String additionalDetail = hasText(report.getAdditionalDetail()) ? report.getAdditionalDetail() : "-";

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
                    int[] mapSize = resolveWordImageSize(
                            mapBytes,
                            WORD_MAP_MAX_WIDTH,
                            WORD_MAP_MAX_HEIGHT,
                            WORD_MAP_FALLBACK_WIDTH,
                            WORD_MAP_FALLBACK_HEIGHT);
                    int width = mapSize[0];
                    int height = mapSize[1];
                    run.addPicture(imageInputStream, XWPFDocument.PICTURE_TYPE_PNG, "map.png", Units.toEMU(width),
                            Units.toEMU(height));
                } catch (Exception e) {
                    log.error("Failed to add map image to Word document", e);
                    run.setText("[Error loading map image]");
                }

                // Add OpenStreetMap link
                if (report.getLocation() != null && report.getLocation().getLatitude() != null
                        && report.getLocation().getLongitude() != null) {
                    XWPFParagraph linkParagraph = document.createParagraph();
                    linkParagraph.setAlignment(ParagraphAlignment.CENTER);

                    String openStreetMapUrl = mapService.buildOpenStreetMapLink(report.getLocation());
                    String linkText = "OpenStreetMap: " + openStreetMapUrl;

                    try {
                        String rId = document.getPackagePart()
                                .addExternalRelationship(openStreetMapUrl, XWPFRelation.HYPERLINK.getRelation()).getId();
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

        List<Image> orderedImages = images.stream()
                .sorted(Comparator
                        .comparingInt((Image img) -> phasePriority(img.getPhase()))
                        .thenComparing(img -> img.getId() != null ? img.getId() : Long.MAX_VALUE))
                .toList();
        Map<String, List<Image>> imagesByPhase = orderedImages.stream()
                .collect(Collectors.groupingBy(img -> img.getPhase() != null ? img.getPhase() : "UNKNOWN"));

        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            if (reportId != null) {
                for (Image image : orderedImages) {
                    try {
                        byte[] imageBytes = uploadService.getObject("images", image.getName());
                        if (imageBytes == null || imageBytes.length == 0) {
                            continue;
                        }

                        XWPFParagraph imageParagraph = document.createParagraph();
                        imageParagraph.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun imageRun = imageParagraph.createRun();

                        int pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
                        String imageName = image.getName().toLowerCase();
                        if (imageName.endsWith(".png")) {
                            pictureType = XWPFDocument.PICTURE_TYPE_PNG;
                        } else if (imageName.endsWith(".gif")) {
                            pictureType = XWPFDocument.PICTURE_TYPE_GIF;
                        }

                        int[] imageSize = resolveWordImageSize(imageBytes);
                        try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
                            imageRun.addPicture(imageInputStream, pictureType, image.getName(),
                                    Units.toEMU(imageSize[0]), Units.toEMU(imageSize[1]));
                            imageRun.addBreak();
                        }
                    } catch (Exception e) {
                        log.error("Failed to add image {} to Word document", image.getName(), e);
                    }
                }

                document.write(baos);
                return baos.toByteArray();
            }

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

                            int[] imageSize = resolveWordImageSize(imageBytes);

                            try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
                                imageRun.addPicture(imageInputStream, pictureType, image.getName(),
                                        Units.toEMU(imageSize[0]), Units.toEMU(imageSize[1]));
                            }

                            // Add image caption
                            XWPFParagraph captionParagraph = document.createParagraph();
                            captionParagraph.setAlignment(ParagraphAlignment.CENTER);
                            XWPFRun captionRun = captionParagraph.createRun();
                            captionRun.setFontSize(12);
                            captionRun.setFontFamily("TH SarabunPSK");
                            captionRun.setItalic(true);
                            captionRun.setText("รูปภาพ");
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

    private int[] resolveWordImageSize(byte[] imageBytes) {
        return resolveWordImageSize(
                imageBytes,
                WORD_IMAGE_MAX_WIDTH,
                WORD_IMAGE_MAX_HEIGHT,
                WORD_IMAGE_FALLBACK_WIDTH,
                WORD_IMAGE_FALLBACK_HEIGHT);
    }

    private int[] resolveWordImageSize(
            byte[] imageBytes,
            int maxWidth,
            int maxHeight,
            int fallbackWidth,
            int fallbackHeight) {
        try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
            BufferedImage bufferedImage = ImageIO.read(imageInputStream);
            if (bufferedImage == null) {
                return new int[] { fallbackWidth, fallbackHeight };
            }

            int imageWidth = bufferedImage.getWidth();
            int imageHeight = bufferedImage.getHeight();
            if (imageWidth <= 0 || imageHeight <= 0) {
                return new int[] { fallbackWidth, fallbackHeight };
            }

            double widthScale = (double) maxWidth / imageWidth;
            double heightScale = (double) maxHeight / imageHeight;
            double scale = Math.min(widthScale, heightScale);
            scale = Math.min(scale, 1.0d);

            int targetWidth = Math.max(1, (int) Math.round(imageWidth * scale));
            int targetHeight = Math.max(1, (int) Math.round(imageHeight * scale));
            return new int[] { targetWidth, targetHeight };
        } catch (Exception e) {
            log.warn("Failed to detect image dimensions for Word export, using fallback size: {}", e.getMessage());
            return new int[] { fallbackWidth, fallbackHeight };
        }
    }

    private int phasePriority(String phase) {
        if ("BEFORE".equalsIgnoreCase(phase)) {
            return 0;
        }
        if ("AFTER".equalsIgnoreCase(phase)) {
            return 1;
        }
        return 2;
    }

    private String buildActiveAssistanceNames(Report report) {
        if (report.getReportAssistances() == null || report.getReportAssistances().isEmpty()) {
            return "";
        }

        return report.getReportAssistances().stream()
                .filter(ra -> Boolean.TRUE.equals(ra.getIsActive()))
                .map(ra -> {
                    String typeName = ra.getAssistanceType().getName();
                    String extraDetail = ra.getExtraDetail();
                    return hasText(extraDetail)
                            ? typeName + ": " + extraDetail.trim()
                            : typeName;
                })
                .collect(Collectors.joining(", "));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
