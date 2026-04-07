package com.careerai.builder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"jwt.secret=test-jwt-secret-key-32-bytes-minimum",
		"spring.ai.model.chat=none",
		"spring.ai.model.embedding=none",
		"spring.autoconfigure.exclude="
				+ "org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration,"
				+ "org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration,"
				+ "org.springframework.ai.autoconfigure.vertexai.embedding.VertexAiEmbeddingAutoConfiguration,"
				+ "org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration"
})
class CareerAiBuilderApplicationTests {

	@Test
	void contextLoads() {
	}

}
