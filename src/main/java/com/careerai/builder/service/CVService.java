package com.careerai.builder.service;

import com.careerai.builder.domain.entity.CV;
import com.careerai.builder.domain.entity.User;
import com.careerai.builder.dto.CVResponse;
import com.careerai.builder.repository.CVRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CVService {

    private final FileStorageService fileStorageService;
    private final CVRepository cvRepository;

    public CVResponse uploadAndParseCV(MultipartFile file, User user) {
        // 1. Process Upload
        String storedFileName = fileStorageService.storeFile(file);
        
        // 2. Tạm thời Mock AI Parsing (sẽ gọi AI LLM thực trong tuơng lai)
        String mockParsedData = "[MOCK] Phân tích thấy kĩ năng: Java, Spring Boot, React...";

        // 3. Save to database
        CV newCv = CV.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .fileUrl("/api/cv/download/" + storedFileName) // Đường dẫn mock public url
                .parsedContent(mockParsedData)
                .build();
                
        CV savedCv = cvRepository.save(newCv);

        // 4. Return DTO
        return CVResponse.builder()
                .id(savedCv.getId())
                .fileName(savedCv.getFileName())
                .fileUrl(savedCv.getFileUrl())
                .parsedContent(savedCv.getParsedContent())
                .build();
    }
}
