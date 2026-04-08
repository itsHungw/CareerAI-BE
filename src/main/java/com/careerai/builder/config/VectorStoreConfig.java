package com.careerai.builder.config;

import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình này chỉ đóng vai trò đánh dấu (Marker).
 * Mặc định Spring Boot (thông qua spring-ai-pgvector-store-spring-boot-starter) 
 * đã tự động tạo đối tượng `VectorStore` (chính xác là `PgVectorStore`)
 * miễn là JdbcTemplate và EmbeddingModel tồn tại trong Context.
 *
 * Để ý application.yml: spring.ai.vectorstore.pgvector.initialize-schema=true
 * sẽ tự chạy lệnh CREATE EXTENSION vector và dọn bảng.
 */
@Configuration
public class VectorStoreConfig {
    // Không cần định nghĩa Bean thủ công nữa vì Starter 1.0.0-M6 tự động nhận diện PGVector!
}
