package com.careerai.builder.service;

import com.careerai.builder.domain.entity.Job;
import com.careerai.builder.dto.MatchQuery;
import com.careerai.builder.dto.MatchedJobResponse;
import com.careerai.builder.repository.JobRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module 3 + Module 4 — Vector Search Engine + Matching Engine
 *
 * Trái tim của hệ thống RAG Job Matching.
 *
 * Flow:
 * 1. CV Text + User Query → Enhanced Search Query
 * 2. Metadata Filter (role, level) → Thu hẹp không gian tìm kiếm
 * 3. Vector Similarity Search → Top N chunks gần nhất
 * 4. Group by Job → Tính Match Score → Rank → Return Top 10
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final VectorStore vectorStore;
    private final JobRepository jobRepository;

    /**
     * Tìm kiếm và xếp hạng Jobs dựa trên CV text + user filters.
     *
     * @param cvText   Nội dung CV đã extract (rawText)
     * @param query    Bộ lọc từ user (role, level, location)
     * @return Danh sách Jobs được xếp hạng theo mức độ phù hợp
     */
    public List<MatchedJobResponse> searchAndMatch(String cvText, MatchQuery query) {
        String effectiveRole = normalizeRoleFilter(normalizeFilterInput(query.getRole()));
        String effectiveLevel = normalizeLevelFilter(normalizeFilterInput(query.getLevel()));

        log.info("🔍 Bắt đầu Vector Search — role: {}, level: {}", effectiveRole, effectiveLevel);

        // 1. Xây dựng search query (kết hợp CV context + user filter)
        String searchQuery = buildEnhancedQuery(cvText, query);

        // 2. Cấu hình search request với metadata filter
        SearchRequest.Builder searchBuilder = SearchRequest.builder()
                .query(searchQuery)
                .topK(30);  // Lấy 30 chunks gần nhất

        // 3. Áp dụng metadata filter (lọc cứng: chỉ tìm trong JD docs)
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filterOp = filterBuilder.eq("docType", "JD");

        if (!effectiveRole.isBlank()) {
            filterOp = filterBuilder.and(
                filterOp,
                filterBuilder.eq("role", effectiveRole)
            );
        }
        if (!effectiveLevel.isBlank()) {
            filterOp = filterBuilder.and(
                filterOp,
                filterBuilder.eq("level", effectiveLevel)
            );
        }

        searchBuilder.filterExpression(filterOp.build());

        // 4. Thực hiện Vector Search
        List<Document> results;
        try {
            results = vectorStore.similaritySearch(searchBuilder.build());
        } catch (Exception e) {
            log.error("❌ Vector search failed: {}", e.getMessage(), e);
            return List.of();
        }

        if (results == null || results.isEmpty()) {
            log.info("📊 Vector Search trả về 0 chunks");
            return List.of();
        }

        log.info("📊 Vector Search trả về {} chunks", results.size());

        // 5. Group chunks by Job ID
        Map<String, List<Document>> groupedByJob = results.stream()
                .filter(doc -> doc.getMetadata().containsKey("jobId"))
                .collect(Collectors.groupingBy(doc ->
                        doc.getMetadata().get("jobId").toString()));

        // 6. Tính match score cho mỗi job
        List<RawMatchResult> rawResults = new ArrayList<>();
        for (Map.Entry<String, List<Document>> entry : groupedByJob.entrySet()) {
            String jobIdStr = entry.getKey();
            List<Document> jobChunks = entry.getValue();

            // Score = trung bình similarity score của các chunks (0.0 - 1.0)
            // Nhân 100 để thành phần trăm
            double avgSimilarity = jobChunks.stream()
                    .mapToDouble(doc -> {
                        // Spring AI Document có thể trả score qua metadata hoặc trực tiếp
                        Object scoreObj = doc.getMetadata().get("distance");
                        if (scoreObj instanceof Number) {
                            // Cosine distance -> similarity = 1 - distance
                            return 1.0 - ((Number) scoreObj).doubleValue();
                        }
                        // Fallback: dùng vị trí trong kết quả để ước lượng
                        return 0.5;
                    })
                    .average().orElse(0.0);

            double matchScore = avgSimilarity * 100;
            rawResults.add(new RawMatchResult(jobIdStr, matchScore, jobChunks.size()));
        }

        // 7. Sort theo score giảm dần, lấy top 10
        rawResults.sort(Comparator.comparing(RawMatchResult::getMatchScore).reversed());
        List<RawMatchResult> topResults = rawResults.stream().limit(10).toList();

        // 8. Enrich với Job entity data
        List<UUID> jobIds = topResults.stream()
                .map(r -> UUID.fromString(r.getJobId()))
                .toList();

        Map<UUID, Job> jobMap = jobRepository.findByIdIn(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        List<MatchedJobResponse> response = new ArrayList<>();
        for (RawMatchResult raw : topResults) {
            UUID jobId = UUID.fromString(raw.getJobId());
            Job job = jobMap.get(jobId);
            if (job != null) {
                response.add(MatchedJobResponse.fromJob(job, raw.getMatchScore(), raw.getChunkMatches()));
            }
        }

        log.info("✅ Trả về {} matched jobs", response.size());
        return response;
    }

    /**
     * Xây dựng enhanced search query bằng cách kết hợp CV context + user filters.
     * Giới hạn CV text ở 2000 chars để không overwhelming embedding model.
     */
    private String buildEnhancedQuery(String cvText, MatchQuery query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Find job descriptions matching this candidate profile:\n\n");

        String effectiveRole = normalizeRoleFilter(normalizeFilterInput(query.getRole()));
        String effectiveLevel = normalizeLevelFilter(normalizeFilterInput(query.getLevel()));
        String effectiveLocation = normalizeFilterInput(query.getLocation());

        if (!effectiveRole.isBlank()) {
            sb.append("Target Role: ").append(effectiveRole).append("\n");
        }
        if (!effectiveLevel.isBlank()) {
            sb.append("Experience Level: ").append(effectiveLevel).append("\n");
        }
        if (!effectiveLocation.isBlank()) {
            sb.append("Preferred Location: ").append(effectiveLocation).append("\n");
        }

        sb.append("\nCandidate Skills & Experience:\n");

        // Giới hạn CV text để tránh token overflow
        String cvSnippet = cvText;
        if (cvText.length() > 2000) {
            cvSnippet = cvText.substring(0, 2000) + "...";
        }
        sb.append(cvSnippet);

        return sb.toString();
    }

    /**
     * Internal result class trước khi enrich với Job entity.
     */
    @Data
    @AllArgsConstructor
    private static class RawMatchResult {
        private String jobId;
        private Double matchScore;
        private Integer chunkMatches;
    }

    private String sanitizeFilterValue(String value) {
        if (value == null) {
            return "";
        }
        // Allow alphanumeric, spaces, hyphens, dots, and plus signs (for skills like C++)
        return value.replaceAll("[^a-zA-Z0-9\\s\\-+.]", "").toLowerCase().trim();
    }

    private String normalizeFilterInput(String value) {
        String sanitized = sanitizeFilterValue(value);
        if (sanitized.isBlank()) {
            return "";
        }

        // Ignore placeholder values often sent by Swagger/manual tests.
        Set<String> invalidPlaceholders = Set.of("string", "null", "undefined", "n/a", "none");
        return invalidPlaceholders.contains(sanitized) ? "" : sanitized;
    }

    private String normalizeRoleFilter(String role) {
        return switch (role) {
            case "ai", "ml", "machine learning" -> "data";
            default -> role;
        };
    }

    private String normalizeLevelFilter(String level) {
        return switch (level) {
            case "middle" -> "mid";
            case "manager" -> "lead";
            default -> level;
        };
    }
}
