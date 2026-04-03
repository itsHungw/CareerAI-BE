package com.careerai.builder.controller;

import com.careerai.builder.domain.entity.User;
import com.careerai.builder.dto.ApiResponse;
import com.careerai.builder.dto.CVResponse;
import com.careerai.builder.exception.ApiException;
import com.careerai.builder.repository.UserRepository;
import com.careerai.builder.service.CVService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CVController {

    private final CVService cvService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CVResponse>> uploadCV(@RequestParam("file") MultipartFile file) {
        
        // Trích xuất User từ Token Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ApiException("User details could not be found via Token", HttpStatus.UNAUTHORIZED));

        // Tiến hành xử lý
        CVResponse result = cvService.uploadAndParseCV(file, user);
        
        return ResponseEntity.ok(ApiResponse.success("CV uploaded and parsed successfully", result));
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadCV(@PathVariable String fileName) {
        Resource resource = cvService.loadCvFile(fileName);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
