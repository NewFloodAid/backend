package com.example.flood_aid.services;

import com.example.flood_aid.models.Image;
import com.example.flood_aid.models.Location;
import com.example.flood_aid.models.Report;
import com.example.flood_aid.models.ReportAssistance;
import com.example.flood_aid.models.ReportAssistanceLog;
import com.example.flood_aid.repositories.ReportRepository;
import com.example.flood_aid.exceptions.ReportNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {
    private final ReportRepository reportRepository;
    private final UploadService uploadService;

    @Transactional(readOnly = true)
    public byte[] exportReportToPdf(Long reportId) throws IOException {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Load fonts: primary (Thai-capable) and fallback (Latin-capable)
            PDFont[] fonts = loadFonts(document);
            PDFont primaryFont = fonts[0];
            PDFont fallbackFont = fonts[1];

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float leading = 18;
                float pageWidth = page.getMediaBox().getWidth();
                float maxTextWidth = pageWidth - 2 * margin;

                content.beginText();
                content.newLineAtOffset(margin, y);
                // No report ID as requested
                int lines = 0;
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "ชื่อ: " + safe(report.getFirstName()) + " " + safe(report.getLastName()), maxTextWidth, leading);

                // Phone numbers
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "เบอร์โทร: " + safe(report.getMainPhoneNumber()), maxTextWidth, leading);
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "เบอร์สำรอง: " + safe(report.getReservePhoneNumber()), maxTextWidth, leading);

                // Assistance types
                String assistanceSummary = buildAssistanceSummary(report);
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "ความต้องการ: " + assistanceSummary, maxTextWidth, leading);

                // Additional details
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "รายละเอียดเพิ่มเติม: " + safe(report.getAdditionalDetail()), maxTextWidth, leading);

                // Address
                Location loc = report.getLocation();
                if (loc != null) {
                    lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "ที่อยู่: " + safe(loc.getAddress()), maxTextWidth, leading);
                    lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "ตำบล: " + safe(loc.getSubDistrict()), maxTextWidth, leading);
                    lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "อำเภอ: " + safe(loc.getDistrict()), maxTextWidth, leading);
                    lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "จังหวัด: " + safe(loc.getProvince()), maxTextWidth, leading);
                    lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14, "รหัสไปรษณีย์: " + safe(loc.getPostalCode()), maxTextWidth, leading);
                }

                content.endText();

                // Draw image below the text block
                float imageTopY = y - (lines * leading) - 20; // position image below written text
                drawFirstImageIfPresent(document, content, page, report, margin, imageTopY);
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }

    private PDFont[] loadFonts(PDDocument document) throws IOException {
        PDFont primary = null;
        PDFont fallback = null;
        // Primary: Thai capable
        try (InputStream thai = getClass().getResourceAsStream("/fonts/NotoSansThai-Regular.ttf")) {
            if (thai != null) primary = PDType0Font.load(document, thai, true);
        } catch (Exception e) {
            log.warn("Couldn't load NotoSansThai-Regular.ttf: {}", e.getMessage());
        }
        // Fallback: Latin capable
        try (InputStream generic = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            if (generic != null) fallback = PDType0Font.load(document, generic, true);
        } catch (Exception e) {
            log.warn("Couldn't load NotoSans-Regular.ttf: {}", e.getMessage());
        }
        if (primary == null && fallback == null) {
            // Absolutely last resort
            return new PDFont[] { PDType1Font.HELVETICA, PDType1Font.HELVETICA };
        }
        if (primary == null) primary = fallback != null ? fallback : PDType1Font.HELVETICA;
        if (fallback == null) fallback = PDType1Font.HELVETICA;
        return new PDFont[] { primary, fallback };
    }

    private void drawFirstImageIfPresent(PDDocument document, PDPageContentStream content, PDPage page,
                                         Report report, float margin, float topY) throws IOException {
        List<Image> images = report.getImages();
        if (images == null || images.isEmpty()) return;

        // Prefer BEFORE phase, else any
        Optional<Image> chosen = images.stream()
                .filter(img -> "BEFORE".equalsIgnoreCase(img.getPhase()))
                .findFirst();
        if (chosen.isEmpty()) {
            chosen = images.stream().findFirst();
        }

        if (chosen.isEmpty()) return;

        PDImageXObject pdImage = null;
        try {
            byte[] bytes = uploadService.getObject("images", chosen.get().getName());
            pdImage = PDImageXObject.createFromByteArray(document, bytes, chosen.get().getName());
        } catch (Exception e) {
            log.warn("Failed to fetch or decode image '{}' from MinIO: {}. Skipping image.", chosen.get().getName(), e.getMessage());
            return; // Skip drawing image; continue with text-only PDF
        }

        float pageWidth = page.getMediaBox().getWidth();
        float maxWidth = pageWidth - 2 * margin;
        float imgWidth = pdImage.getWidth();
        float imgHeight = pdImage.getHeight();
        float scale = Math.min(maxWidth / imgWidth, 300f / imgHeight); // cap height roughly
        float drawWidth = imgWidth * scale;
        float drawHeight = imgHeight * scale;

        float x = margin;
        float y = Math.max(margin, topY - drawHeight);

        content.drawImage(pdImage, x, y, drawWidth, drawHeight);
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }

    private static void writeLineWithFallback(PDPageContentStream content, PDFont primary, PDFont fallback, float fontSize, String text) throws IOException {
        // Write text switching fonts for characters the primary font cannot encode
        StringBuilder run = new StringBuilder();
        PDFont current = primary;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            if (canEncode(current, ch)) {
                run.append(ch);
            } else {
                // flush current run
                if (run.length() > 0) {
                    content.setFont(current, fontSize);
                    content.showText(run.toString());
                    run.setLength(0);
                }
                // switch to fallback for this char
                PDFont use = canEncode(fallback, ch) ? fallback : PDType1Font.HELVETICA;
                if (canEncode(use, ch)) {
                    content.setFont(use, fontSize);
                    content.showText(ch);
                } else {
                    // replace with '?'
                    content.setFont(use, fontSize);
                    content.showText("?");
                }
                // resume with primary
                current = primary;
            }
            i += Character.charCount(cp);
        }
        if (run.length() > 0) {
            content.setFont(current, fontSize);
            content.showText(run.toString());
        }
        content.newLineAtOffset(0, -18);
    }

    private static boolean canEncode(PDFont font, String s) {
        try {
            font.encode(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static float stringWidth(PDFont font, String s, float fontSize) {
        try {
            return font.getStringWidth(s) / 1000f * fontSize;
        } catch (Exception e) {
            return 0f;
        }
    }

    // Writes text wrapped to maxWidth, returns number of lines written
    private static int writeWrappedWithFallback(
            PDPageContentStream content,
            PDFont primary,
            PDFont fallback,
            float fontSize,
            String text,
            float maxWidth,
            float leading
    ) throws IOException {
        if (text == null) text = "";
        int lines = 0;
        StringBuilder line = new StringBuilder();
        int lastSpaceIndex = -1; // index in line
        float lineWidth = 0f;
        float widthAtLastSpace = 0f;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            boolean isNewline = ch.equals("\n");
            PDFont useFont = canEncode(primary, ch) ? primary : (canEncode(fallback, ch) ? fallback : PDType1Font.HELVETICA);
            float chWidth = isNewline ? 0f : stringWidth(useFont, ch, fontSize);

            boolean isSpace = Character.isWhitespace(cp) && !isNewline;

            if (isNewline || lineWidth + chWidth > maxWidth) {
                if (!isNewline && lastSpaceIndex >= 0) {
                    // wrap at last space
                    String toWrite = line.substring(0, lastSpaceIndex);
                    if (!toWrite.isEmpty()) {
                        writeLineWithFallback(content, primary, fallback, fontSize, toWrite);
                        lines++;
                    } else {
                        // no content before space, force wrap at current pos
                        writeLineWithFallback(content, primary, fallback, fontSize, line.toString());
                        lines++;
                        line.setLength(0);
                        lastSpaceIndex = -1;
                        lineWidth = 0f;
                    }
                    // prepare next line: leftover after space
                    String leftover = line.substring(Math.min(lastSpaceIndex + 1, line.length()));
                    line.setLength(0);
                    line.append(leftover);
                    // recompute width for leftover
                    lineWidth = 0f;
                    for (int j = 0; j < line.length();) {
                        int cp2 = line.codePointAt(j);
                        String ch2 = new String(Character.toChars(cp2));
                        PDFont f2 = canEncode(primary, ch2) ? primary : (canEncode(fallback, ch2) ? fallback : PDType1Font.HELVETICA);
                        lineWidth += stringWidth(f2, ch2, fontSize);
                        if (Character.isWhitespace(cp2)) {
                            lastSpaceIndex = j; widthAtLastSpace = lineWidth;
                        }
                        j += Character.charCount(cp2);
                    }
                } else {
                    // force wrap at current position
                    writeLineWithFallback(content, primary, fallback, fontSize, line.toString());
                    lines++;
                    line.setLength(0);
                    lastSpaceIndex = -1;
                    lineWidth = 0f;
                }
                if (isNewline) {
                    // start new line after explicit newline, don't add newline char
                } else {
                    // append current char to new line
                    line.append(ch);
                    lineWidth += chWidth;
                    if (isSpace) { lastSpaceIndex = line.length() - 1; widthAtLastSpace = lineWidth; }
                }
            } else {
                // still fits in current line
                line.append(ch);
                lineWidth += chWidth;
                if (isSpace) { lastSpaceIndex = line.length() - 1; widthAtLastSpace = lineWidth; }
            }
            i += Character.charCount(cp);
        }

        if (line.length() > 0) {
            writeLineWithFallback(content, primary, fallback, fontSize, line.toString());
            lines++;
        }
        return lines;
    }

    private String buildAssistanceSummary(Report report) {
        List<ReportAssistanceLog> logs = report.getReportAssistanceLogs();
        if (logs == null || logs.isEmpty()) return "-";
        // Collect unique assistance names with isActive = true in first-seen order, without quantities
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        for (ReportAssistanceLog log : logs) {
            if (log == null || log.getIsActive() == null || !log.getIsActive()) continue;
            String name = (log.getAssistanceType() != null) ? safe(log.getAssistanceType().getName()) : "-";
            if (name != null && !name.isBlank()) names.add(name);
        }
        if (names.isEmpty()) return "-";
        return String.join(", ", names);
    }
}
