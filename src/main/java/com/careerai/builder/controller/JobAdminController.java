package com.careerai.builder.controller;

import com.careerai.builder.domain.entity.Company;
import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.dto.ApiResponse;
import com.careerai.builder.dto.JobImportRequest;
import com.careerai.builder.repository.CompanyRepository;
import com.careerai.builder.repository.JobRepository;
import com.careerai.builder.service.JobIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin API for managing Job data and triggering the RAG ingestion pipeline.
 *
 * Endpoints:
 * - POST /api/admin/jobs/import  — Import batch JDs into database
 * - POST /api/admin/jobs/ingest  — Trigger embedding for all pending jobs
 * - GET  /api/admin/jobs/status  — Check ingestion status summary
 */
@RestController
@RequestMapping("/api/admin/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobAdminController {

    private final JobIngestionService jobIngestionService;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    /**
     * Import một batch Job Descriptions vào database.
     * Mỗi JD sẽ được tạo tương ứng Company (nếu chưa tồn tại) và Job entity.
     * Sau khi import, trạng thái ingestion là PENDING — cần gọi /ingest để embedding.
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importJobs(
            @Valid @RequestBody List<JobImportRequest> requests) {

        log.info("📥 Nhận yêu cầu import {} jobs", requests.size());

        List<Job> importedJobs = new ArrayList<>();

        for (JobImportRequest req : requests) {
            // Upsert Company
            Company company = companyRepository.findByNameIgnoreCase(req.getCompany())
                    .orElseGet(() -> companyRepository.save(Company.builder()
                            .name(req.getCompany())
                            .industry(req.getIndustry())
                            .build()));

            // Create Job
            Job job = Job.builder()
                    .title(req.getTitle())
                    .company(req.getCompany())
                    .companyEntity(company)
                    .location(req.getLocation())
                    .role(req.getRole().toLowerCase())
                    .level(req.getLevel().toLowerCase())
                    .salaryRange(req.getSalaryRange())
                    .rawDescription(req.getDescription())
                    .descriptionHtml(req.getDescription()) // Dùng tạm plain text
                    .sourceUrl(req.getSourceUrl())
                    .ingestionStatus(Job.IngestionStatus.PENDING)
                    .build();

            importedJobs.add(jobRepository.save(job));
        }

        log.info("✅ Imported {} jobs thành công", importedJobs.size());

        return ResponseEntity.ok(ApiResponse.success("Jobs imported successfully", Map.of(
                "imported", importedJobs.size(),
                "status", "PENDING — call POST /api/admin/jobs/ingest to embed into vector store"
        )));
    }

    /**
     * Trigger embedding pipeline cho tất cả Job có trạng thái PENDING + FAILED (retry).
     * Sẽ chunk JD → gọi Embedding API → lưu vào pgvector.
     */
    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerIngestion() {
        log.info("🚀 Triggering ingestion for pending + failed jobs");

        int successCount = jobIngestionService.ingestPendingJobs();
        long totalPending = jobRepository.findByIngestionStatus(Job.IngestionStatus.PENDING).size();
        long totalFailed = jobRepository.findByIngestionStatus(Job.IngestionStatus.FAILED).size();

        return ResponseEntity.ok(ApiResponse.success("Ingestion completed", Map.of(
                "ingested", successCount,
                "remaining_pending", totalPending,
                "remaining_failed", totalFailed
        )));
    }

    /**
     * Lấy tổng quan trạng thái ingestion.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIngestionStatus() {
        long total = jobRepository.count();
        long pending = jobRepository.findByIngestionStatus(Job.IngestionStatus.PENDING).size();
        long ingested = jobRepository.findByIngestionStatus(Job.IngestionStatus.INGESTED).size();
        long failed = jobRepository.findByIngestionStatus(Job.IngestionStatus.FAILED).size();

        return ResponseEntity.ok(ApiResponse.success("Ingestion status", Map.of(
                "total_jobs", total,
                "pending", pending,
                "ingested", ingested,
                "failed", failed
        )));
    }
}
