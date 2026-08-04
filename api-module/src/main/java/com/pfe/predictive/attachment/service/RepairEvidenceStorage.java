package com.pfe.predictive.attachment.service;

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

/**
 * Local-disk file storage for attachment uploads (repair evidence photos
 * today). Mirrors MachinePhotoStorage's convention exactly — same
 * UUID-suffixed naming, same file:// resource-loading pattern — so it plugs
 * into the existing StaticResourceConfig/uploads serving approach.
 */
@Component
public class RepairEvidenceStorage {

    private final Path baseDir;

    public RepairEvidenceStorage(@Value("${app.attachment.dir:uploads/repair-evidence}") String baseDir) {
        this.baseDir = Path.of(baseDir);
    }

    public StoredFile save(MultipartFile file) throws IOException {
        Files.createDirectories(baseDir);

        String extension = resolveExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + extension;

        Path target = baseDir.resolve(storedFileName).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return new StoredFile(storedFileName, file.getContentType(), file.getSize());
    }

    public Resource loadAsResource(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return null;
        }
        Path target = baseDir.resolve(storedFileName).normalize();
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
        return ext.length() > 10 ? "" : ext;
    }

    public record StoredFile(String storedFileName, String contentType, long fileSize) {
    }
}
