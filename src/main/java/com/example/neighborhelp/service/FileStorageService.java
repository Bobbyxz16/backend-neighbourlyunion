package com.example.neighborhelp.service;

import com.example.neighborhelp.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.base-url:http://localhost:8080}")
    private String baseUrl;

    public FileStorageService(@Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Store file and return the public URL
     */
    public String storeFile(MultipartFile file) {
        // Validate file
        validateFile(file);

        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + originalFileName);
            }

            // Generate unique filename to prevent collisions
            String fileExtension = getFileExtension(originalFileName);
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return public URL
            String fileUrl = baseUrl + "/uploads/" + uniqueFileName;

            log.info("File stored successfully: {}", fileUrl);
            return fileUrl;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    /**
     * Delete file by URL or filename
     */
    public void deleteFile(String fileUrl) {
        try {
            // Extract filename from URL
            String fileName = extractFileNameFromUrl(fileUrl);
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            log.info("File deleted successfully: {}", fileName);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", fileUrl, ex);
        }
    }

    /**
     * Validate file before upload
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        // Check file size (max 5MB)
        long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxFileSize) {
            throw new FileStorageException("File size exceeds maximum limit of 5MB");
        }

        // Check file type
        String contentType = file.getContentType();
        if (!isValidImageType(contentType)) {
            throw new FileStorageException("Invalid file type. Only JPEG, PNG, and GIF images are allowed");
        }
    }

    /**
     * Check if file type is valid image
     */
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif") ||
                        contentType.equals("image/webp")
        );
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex);
    }

    /**
     * Extract filename from full URL
     */
    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null) {
            return "";
        }
        int lastSlashIndex = fileUrl.lastIndexOf('/');
        return (lastSlashIndex == -1) ? fileUrl : fileUrl.substring(lastSlashIndex + 1);
    }
}