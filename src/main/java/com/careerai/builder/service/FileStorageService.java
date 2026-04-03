package com.careerai.builder.service;

import com.careerai.builder.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${app.upload-dir:cv-uploads/}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new ApiException("Could not create the directory where the uploaded files will be stored.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException("Failed to store empty file.", HttpStatus.BAD_REQUEST);
        }
        
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.pdf");
        
        if (!originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new ApiException("Sorry! Only PDF files are allowed.", HttpStatus.BAD_REQUEST);
        }
        
        try {
            // Check for invalid characters
            if (originalFileName.contains("..")) {
                throw new ApiException("Sorry! Filename contains invalid path sequence " + originalFileName, HttpStatus.BAD_REQUEST);
            }

            // Generate unique filename
            String fileExtension = ".pdf";
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return newFileName;
        } catch (IOException ex) {
            throw new ApiException("Could not store file " + originalFileName + ". Please try again!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new ApiException("Invalid file path.", HttpStatus.BAD_REQUEST);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }

            throw new ApiException("File not found.", HttpStatus.NOT_FOUND);
        } catch (IOException ex) {
            throw new ApiException("File not found.", HttpStatus.NOT_FOUND);
        }
    }
}
