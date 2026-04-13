package com.careerai.builder.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service chuyên dụng cho việc bóc tách nội dung từ file PDF.
 * Tách ra từ CVService để giảm tải và tăng khả năng tái sử dụng.
 */
@Service
@Slf4j
public class PdfParsingService {

    /**
     * Bóc tách văn bản searchable từ MultipartFile (PDF hoặc text).
     */
    public String extractSearchableText(MultipartFile file, String originalFileName) throws IOException {
        String extractedPdfText = extractPdfText(file);
        
        if (!extractedPdfText.isBlank()) {
            String normalizedText = normalizeExtractedText(extractedPdfText);
            return String.join(" ",
                    "file-name:", originalFileName,
                    "pdf-text:", normalizedText);
        }

        // Fallback cho file không phải PDF hoặc bóc tách lỗi
        String bytePreview = new String(file.getBytes(), StandardCharsets.ISO_8859_1);
        String normalizedPreview = normalizeExtractedText(bytePreview);
        return String.join(" ",
                "file-name:", originalFileName,
                "heuristic-preview:", normalizedPreview);
    }

    /**
     * Sử dụng PDFBox để bóc tách văn bản.
     */
    private String extractPdfText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            return textStripper.getText(document);
        } catch (IOException ex) {
            log.warn("Failed to parse PDF content for '{}': {}", file.getOriginalFilename(), ex.getMessage());
            return "";
        }
    }

    /**
     * Chuẩn hóa văn bản sau khi bóc tách (loại bỏ ký tự lạ, khoảng trắng thừa).
     */
    public String normalizeExtractedText(String text) {
        if (text == null) return "";
        
        String normalized = text.replaceAll("[^\\p{L}\\p{N}\\.\\+#\\-/ ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        
        // Giới hạn 12000 ký tự để tránh token limit quá lớn cho LLM
        if (normalized.length() > 12000) {
            return normalized.substring(0, 12000);
        }
        return normalized;
    }
}
