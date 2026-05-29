package com.pfe.predictive.machine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Component
public class MachinePhotoStorage {

    private final Path baseDir;

    public MachinePhotoStorage(@Value("${app.machine.photo-dir:uploads/machines}") String baseDir) {
        this.baseDir = Path.of(baseDir);
    }

    public StoredPhoto save(MultipartFile file, String machineSerial) throws IOException {
        Files.createDirectories(baseDir);

        String originalName = file.getOriginalFilename();
        String extension = resolveExtension(originalName);
        String safeSerial = machineSerial == null ? "machine" : machineSerial.replaceAll("[^a-zA-Z0-9_-]", "_");
        String fileName = safeSerial + "-" + UUID.randomUUID() + extension;

        Path target = baseDir.resolve(fileName).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String contentType = file.getContentType();
        return new StoredPhoto(fileName, contentType);
    }

    public Resource loadAsResource(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        Path target = baseDir.resolve(relativePath).normalize();
        return new FileSystemResource(target);
    }

    private String resolveExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(dot).toLowerCase(Locale.ROOT);
        if (ext.length() > 10) {
            return "";
        }
        return ext;
    }

    public static class StoredPhoto {
        private final String relativePath;
        private final String contentType;

        public StoredPhoto(String relativePath, String contentType) {
            this.relativePath = relativePath;
            this.contentType = contentType;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
