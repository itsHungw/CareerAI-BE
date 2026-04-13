package com.careerai.builder.service;

import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Module 2 — Job Processing (Offline Ingestion)
 *
 * Chịu trách nhiệm:
 * 1. Gộp nội dung JD thành văn bản tìm kiếm được
 * 2. Chunking JD thành các đoạn nhỏ
 * 3. Embedding + lưu vào pgvector thông qua Spring AI VectorStore
 * 4. Cập nhật trạng thái ingestion trên Job entity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobIngestionService {

    private final VectorStore vectorStore;
    private final JobRepository jobRepository;

    /**
     * Ingest một Job đơn lẻ vào Vector Store.
     *
     * Chiến lược chunking:
     * - Sử dụng TokenTextSplitter với chunk size 500 tokens (nhỏ hơn CV vì JD thường ngắn và tập trung)
     * - Overlap 80 tokens để giữ mạch ngữ nghĩa giữa các chunks
     * - Metadata bao gồm: jobId, role, level, location, company, docType=JD
     */
    @Transactional
    public void ingestJob(Job job) {
        log.info("🔄 Bắt đầu Ingestion cho Job ID: {} - '{}'", job.getId(), job.getTitle());

        // 1. Gộp nội dung JD thành searchable text
        String searchableText = buildSearchableText(job);

        if (searchableText.isBlank()) {
            log.warn("⚠️ Job {} có nội dung rỗng, bỏ qua ingestion", job.getId());
            job.setIngestionStatus(Job.IngestionStatus.FAILED);
            jobRepository.save(job);
            return;
        }

        // 2. Chunking
        TokenTextSplitter splitter = new TokenTextSplitter(500, 80, 5, 8000, true);

        Document document = new Document(searchableText);
        document.getMetadata().putAll(Map.of(
                "jobId", job.getId().toString(),
                "role", nullSafe(job.getRole()),
                "level", nullSafe(job.getLevel()),
                "location", nullSafe(job.getLocation()),
                "company", nullSafe(job.getCompany()),
                "docType", "JD"
        ));

        List<Document> chunks = splitter.apply(List.of(document));
        log.info("📄 Job '{}' được cắt thành {} chunks", job.getTitle(), chunks.size());

        // 3. Embedding & Store vào pgvector (Spring AI xử lý ngầm)
        vectorStore.accept(chunks);

        // 4. Cập nhật trạng thái
        job.setIngestionStatus(Job.IngestionStatus.INGESTED);
        jobRepository.save(job);

        log.info("✅ Hoàn tất Ingestion cho Job: '{}'", job.getTitle());
    }

    /**
     * Batch ingest tất cả Job có trạng thái PENDING hoặc FAILED (retry).
     *
     * @return số lượng Job được ingest thành công
     */
    public int ingestPendingJobs() {
        List<Job> jobsToIngest = new ArrayList<>();
        jobsToIngest.addAll(jobRepository.findByIngestionStatus(Job.IngestionStatus.PENDING));
        jobsToIngest.addAll(jobRepository.findByIngestionStatus(Job.IngestionStatus.FAILED));

        log.info("🔍 Tìm thấy {} jobs cần ingest/retry (PENDING + FAILED)", jobsToIngest.size());

        int success = 0;
        for (Job job : jobsToIngest) {
            try {
                ingestJob(job);
                success++;
            } catch (Exception e) {
                log.error("❌ Failed to ingest Job '{}' (ID: {})", job.getTitle(), job.getId(), e);
                job.setIngestionStatus(Job.IngestionStatus.FAILED);
                jobRepository.save(job);
            }
        }

        log.info("📊 Ingestion batch hoàn tất: {}/{} thành công", success, jobsToIngest.size());
        return success;
    }

    /**
     * Xây dựng văn bản tìm kiếm từ các trường của Job.
     * Ưu tiên rawDescription (plain text), fallback sang descriptionHtml (strip tags).
     */
    private String buildSearchableText(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("Job Title: ").append(nullSafe(job.getTitle())).append("\n");
        sb.append("Company: ").append(nullSafe(job.getCompany())).append("\n");
        sb.append("Role: ").append(nullSafe(job.getRole())).append("\n");
        sb.append("Level: ").append(nullSafe(job.getLevel())).append("\n");
        sb.append("Location: ").append(nullSafe(job.getLocation())).append("\n\n");

        if (job.getRawDescription() != null && !job.getRawDescription().isBlank()) {
            sb.append(job.getRawDescription());
        } else if (job.getDescriptionHtml() != null && !job.getDescriptionHtml().isBlank()) {
            // Strip HTML tags for embedding
            sb.append(job.getDescriptionHtml().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim());
        }

        return sb.toString().trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
