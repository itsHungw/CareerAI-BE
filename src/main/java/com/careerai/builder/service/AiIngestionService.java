package com.careerai.builder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIngestionService {

    private final VectorStore vectorStore;

    /**
     * Nhận văn bản cực lớn (VD: CV), cắt nhỏ thành đoạn (Chunking)
     * và nhúng vào DB thông qua VectorStore xử lý ngầm EmbeddingModel.
     *
     * @param rawText Toàn bộ nội dung CV
     * @param cvId    Mã định danh của CV để đánh dấu Metadata
     * @param userId  ID người dùng
     */
    public void ingestCvToVectorStore(String rawText, Long cvId, Long userId) {
        log.info("Bắt đầu xử lý Nhúng (Embedding) cho CV ID: {}", cvId);

        // 1. Tạo chiến thuật cắt văn bản TokenTextSplitter (như LangChain)
        // Cắt mỗi đoạn dài 800 tokens, phần giao nhau (overlap) là 100 tokens để giữ mạch văn 
        TokenTextSplitter splitter = new TokenTextSplitter(800, 100, 5, 10000, true);

        // 2. Định dạng đầu vào
        Document document = new Document(rawText);
        document.getMetadata().put("cvId", cvId);
        document.getMetadata().put("userId", userId);
        document.getMetadata().put("docType", "CV");

        log.info("Chuẩn bị bẻ gãy văn bản CV ID: {}", cvId);
        // 3. Tiến hành cắt (Chunking)
        List<Document> chunks = splitter.apply(List.of(document));
        
        log.info("CV ID: {} được cắt thành {} chunks. Bắt đầu đẩy lên VectorStore...", cvId, chunks.size());

        // 4. Nhúng & Đẩy lên PgVector (Sẽ gọi API OpenAI/Gemini ẩn giấu bên trong)
        vectorStore.accept(chunks);

        log.info("Đã hoàn tất nhúng & lưu toàn bộ chuỗi Vector vào PostgreSQL thành công cho CV ID: {}", cvId);
    }
}
