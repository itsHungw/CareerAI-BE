package com.careerai.builder.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AiIngestionServiceTest {

    @Test
    void ingestCvToVectorStoreSplitsTextAndPushesDocumentsToVectorStore() {
        VectorStore vectorStore = mock(VectorStore.class);
        AiIngestionService aiIngestionService = new AiIngestionService(vectorStore);

        String mockCvRawText = "Tran Van A, Java Backend Developer. "
                + "Kinh nghiem 5 nam lam viec voi Spring Boot, Hibernate, Microservices. "
                + "Tung dam nhiem cac he thong chiu tai cao va tich hop Redis, Kafka. "
                + "Muc tieu nghe nghiep: Tro thanh System Architect trong vong 3 nam toi.";

        Long mockCvId = 9999L;
        Long mockUserId = 1L;
        aiIngestionService.ingestCvToVectorStore(mockCvRawText, mockCvId, mockUserId);

        @SuppressWarnings("unchecked")
        var documentsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).accept(documentsCaptor.capture());

        Object capturedValue = documentsCaptor.getValue();
        assertInstanceOf(List.class, capturedValue);

        List<?> rawDocuments = documentsCaptor.getValue();
        assertFalse(rawDocuments.isEmpty());

        Document firstDocument = (Document) rawDocuments.getFirst();
        assertTrue(firstDocument.getText().contains("Java Backend Developer"));
        assertEquals(mockCvId, firstDocument.getMetadata().get("cvId"));
        assertEquals(mockUserId, firstDocument.getMetadata().get("userId"));
        assertEquals("CV", firstDocument.getMetadata().get("docType"));
    }
}
