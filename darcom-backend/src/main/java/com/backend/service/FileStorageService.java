package com.backend.service;

import com.backend.config.AppConfig;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Singleton
public class FileStorageService {

    private final String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));

    public FileStorageService(AppConfig config) {
        this.uploadDir = config.getUploadDir();
    }

    public String storeImage(InputStream content, String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ApiException(400, ErrorCode.INVALID_FILE_TYPE, "Filename is required");
        }

        String extension = getExtension(filename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(400, ErrorCode.INVALID_FILE_TYPE, "Invalid file type. Allowed: jpg, jpeg, png, webp");
        }

        try {
            // Create directory if not exists
            Path uploadPath = Path.of(uploadDir);
            Files.createDirectories(uploadPath);

            // Generate secure unique filename
            String uniqueFilename = UUID.randomUUID() + "." + extension;
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Copy file
            try (InputStream in = content) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public void deleteImage(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Path filePath = Path.of(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail
            System.err.println("Failed to delete file: " + filename);
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}