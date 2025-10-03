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
                float margin = 50f;
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float leading = 20f;
                float maxTextWidth = pageWidth - 2 * margin;

                // 1) Header (centered)
                float y = pageHeight - 70f;
                drawCenteredLine(content, primaryFont, fallbackFont, 20f, "คำร้อง", pageWidth, y);
                y -= 26f;
                drawCenteredLine(content, primaryFont, fallbackFont, 18f, "เทศบาลนครเชียงใหม่", pageWidth, y);

                // 2) Right top (location + date)
                y -= 18f;
                drawRightAlignedLine(content, primaryFont, fallbackFont, 12f, "เขียนที่ เทศบาลนครเชียงใหม่", pageWidth, margin, y);
                y -= 18f;
                String thaiDate = formatThaiDate(report.getCreatedAt());
                drawRightAlignedLine(content, primaryFont, fallbackFont, 12f, thaiDate, pageWidth, margin, y);

                // 3) Body
                y -= 30f;
                int lines = 0;
                content.beginText();
                content.newLineAtOffset(margin, y);
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14f,
                        "เรื่อง  ขอความอนุเคราะห์", maxTextWidth, leading);
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14f,
                        "เรียน  นายกเทศมนตรีเทศบาลนครเชียงใหม่", maxTextWidth, leading);
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14f,
                        "", maxTextWidth, leading);

                // Compose introduction line
                Location loc = report.getLocation();
                String fullName = (safe(report.getFirstName()) + " " + safe(report.getLastName())).trim();
                String addressPart = loc != null ? safe(loc.getAddress()) : "-";
                String subdistrict = loc != null ? safe(loc.getSubDistrict()) : "-";
                String district = loc != null ? safe(loc.getDistrict()) : "-";
                String province = loc != null ? safe(loc.getProvince()) : "-";
                String phone = safe(report.getMainPhoneNumber());
                String assistance = buildAssistanceSummary(report);

                String intro = String.format(
                        "                    ข้าพเจ้า %s อยู่บ้านเลขที่ %s ตำบล %s อำเภอ %s จังหวัด %s โทรศัพท์ %s มีความประสงค์ใคร่ขอความอนุเคราะห์ ให้ท่านช่วยเหลือและแก้ไขปัญหาความเดือดร้อนให้ข้าพเจ้า ดังนี้ - ต้องการ %s",
                        fullName, addressPart, subdistrict, district, province, phone, assistance
                );
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14f, intro, maxTextWidth, leading);

                // Additional detail (what is being requested)
                String details = safe(report.getAdditionalDetail());
                if (!details.isBlank()) {
                    lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14f, details, maxTextWidth, leading);
                } else {
                    }

                // Courtesy line
                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14f,
                        "หากเป็นผลประการใด โปรดแจ้งให้ข้าพเจ้าทราบด้วย จักขอบคุณยิ่ง", maxTextWidth, leading);

                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14f,
                        "               จึงเรียนมาเพื่อโปรดทราบ", maxTextWidth, leading);

                lines += writeWrappedWithFallback(content, primaryFont, fallbackFont, 14f,
                        "                                                                                       ขอแสดงความนับถือ", maxTextWidth, leading);
                content.endText();

                // 4) Image at bottom; scale to fit space under the body
                float yAfterBody = y - (lines * leading) - 10f;
                float maxImageHeight = Math.max(0f, yAfterBody - (margin + 30f));
                float imageTopY = drawBottomImageAndGetTopY(document, content, page, report, margin, maxImageHeight);

                // 5) Signature block (right side) above image
                float signBlockY = Math.max(yAfterBody, (imageTopY > 0 ? imageTopY + 20f : 140f));
                String lineDots = "...............................";
                drawRightAlignedLine(content, primaryFont, fallbackFont, 14f,
                        "ลงชื่อ " + lineDots + " ผู้ยื่นคำร้อง", pageWidth, margin, signBlockY);
                float rightPadding = 80f; // space from the right edge
                drawRightAlignedLine(content, primaryFont, fallbackFont, 14f,
                                    "(" + fullName + ")", 
                                    pageWidth - rightPadding,   // pretend the page is a bit narrower
                                    margin, 
                                    signBlockY - 20f);
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

    // Draws the first report image at the bottom margin and returns the top Y used.
    // If there is no image or not enough space (maxHeight <= 0), returns 0 without drawing.
    private float drawBottomImageAndGetTopY(PDDocument document, PDPageContentStream content, PDPage page,
                                           Report report, float margin, float maxHeight) throws IOException {
        List<Image> images = report.getImages();
        if (images == null || images.isEmpty() || maxHeight <= 0f) return 0f;

        Optional<Image> chosen = images.stream()
                .filter(img -> "BEFORE".equalsIgnoreCase(img.getPhase()))
                .findFirst();
        if (chosen.isEmpty()) chosen = images.stream().findFirst();
        if (chosen.isEmpty()) return 0f;

        PDImageXObject pdImage;
        try {
            byte[] bytes = uploadService.getObject("images", chosen.get().getName());
            pdImage = PDImageXObject.createFromByteArray(document, bytes, chosen.get().getName());
        } catch (Exception e) {
            log.warn("Failed to fetch or decode image '{}' from MinIO: {}. Skipping image.", chosen.get().getName(), e.getMessage());
            return 0f;
        }

        float pageWidth = page.getMediaBox().getWidth();
        float maxWidth = pageWidth - 2 * margin;
        float imgWidth = pdImage.getWidth();
        float imgHeight = pdImage.getHeight();
        float scale = Math.min(maxWidth / imgWidth, maxHeight / imgHeight);
        if (!Float.isFinite(scale) || scale <= 0f) return 0f;
        float drawWidth = imgWidth * scale;
        float drawHeight = imgHeight * scale;

        float x = margin;
        float y = margin; // bottom margin
        content.drawImage(pdImage, x, y, drawWidth, drawHeight);
        return y + drawHeight; // top Y of the image
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }

    private static void writeLineWithFallback(PDPageContentStream content, PDFont primary, PDFont fallback, float fontSize, String text, float leading) throws IOException {
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
        content.newLineAtOffset(0, -leading);
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
                        writeLineWithFallback(content, primary, fallback, fontSize, toWrite, leading);
                        lines++;
                    } else {
                        // no content before space, force wrap at current pos
                        writeLineWithFallback(content, primary, fallback, fontSize, line.toString(), leading);
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
                    writeLineWithFallback(content, primary, fallback, fontSize, line.toString(), leading);
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
            writeLineWithFallback(content, primary, fallback, fontSize, line.toString(), leading);
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

    // Utilities for absolute-positioned single lines
    private static void drawCenteredLine(PDPageContentStream content, PDFont primary, PDFont fallback,
                                         float fontSize, String text, float pageWidth, float y) throws IOException {
        float width = computeWidthWithFallback(primary, fallback, text, fontSize);
        float x = (pageWidth - width) / 2f;
        drawLineAt(content, primary, fallback, fontSize, text, x, y);
    }

    private static void drawRightAlignedLine(PDPageContentStream content, PDFont primary, PDFont fallback,
                                             float fontSize, String text, float pageWidth, float marginRight, float y) throws IOException {
        float width = computeWidthWithFallback(primary, fallback, text, fontSize);
        float x = pageWidth - marginRight - width;
        drawLineAt(content, primary, fallback, fontSize, text, x, y);
    }

    private static void drawLineAt(PDPageContentStream content, PDFont primary, PDFont fallback,
                                   float fontSize, String text, float x, float y) throws IOException {
        content.beginText();
        content.newLineAtOffset(x, y);
        showTextWithFallback(content, primary, fallback, fontSize, text);
        content.endText();
    }

    private static void showTextWithFallback(PDPageContentStream content, PDFont primary, PDFont fallback,
                                             float fontSize, String text) throws IOException {
        if (text == null) text = "";
        StringBuilder run = new StringBuilder();
        PDFont current = primary;
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            boolean can = canEncode(current, ch);
            if (can) {
                run.append(ch);
            } else {
                if (run.length() > 0) {
                    content.setFont(current, fontSize);
                    content.showText(run.toString());
                    run.setLength(0);
                }
                PDFont use = canEncode(primary, ch) ? primary : (canEncode(fallback, ch) ? fallback : PDType1Font.HELVETICA);
                content.setFont(use, fontSize);
                content.showText(ch);
                current = primary; // reset to primary after single char
            }
            i += Character.charCount(cp);
        }
        if (run.length() > 0) {
            content.setFont(current, fontSize);
            content.showText(run.toString());
        }
    }

    private static float computeWidthWithFallback(PDFont primary, PDFont fallback, String text, float fontSize) {
        if (text == null) return 0f;
        float width = 0f;
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            PDFont use = canEncode(primary, ch) ? primary : (canEncode(fallback, ch) ? fallback : PDType1Font.HELVETICA);
            width += stringWidth(use, ch, fontSize);
            i += Character.charCount(cp);
        }
        return width;
    }

    private static String formatThaiDate(java.sql.Timestamp ts) {
        java.time.LocalDate date = (ts != null ? ts.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate() : java.time.LocalDate.now());
        String[] thaiMonths = {
                "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
                "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
        };
        int day = date.getDayOfMonth();
        String month = thaiMonths[date.getMonthValue() - 1];
        int beYear = date.getYear() + 543;
        return String.format("วันที่ %d %s พ.ศ. %d", day, month, beYear);
    }
}
