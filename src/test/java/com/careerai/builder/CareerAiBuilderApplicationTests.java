package com.careerai.builder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.vectorstore.VectorStore;

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

	@MockBean
	private VectorStore vectorStore;

	@Test
	void contextLoads() {
	}

}
