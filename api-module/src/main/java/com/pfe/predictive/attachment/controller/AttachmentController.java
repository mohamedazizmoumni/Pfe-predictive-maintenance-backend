package com.pfe.predictive.attachment.controller;

import com.pfe.predictive.attachment.service.AttachmentService;
import com.pfe.predictive.attachment.service.RepairEvidenceStorage;
import com.pfe.predictive.core.entity.Attachment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Repair-evidence photo attachments. Deliberately not served through the
 * public /uploads/** static-resource mapping (see StaticResourceConfig) —
 * these are internal maintenance records, not the public part-catalog
 * images that mapping was built for, so downloads go through an
 * authenticated endpoint instead.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Polymorphic file attachments (repair evidence photos)")
public class AttachmentController {

    private static final List<String> ALLOWED_ENTITY_TYPES = List.of("MaintenanceRapport", "Machine");

    private final AttachmentService attachmentService;
    private final RepairEvidenceStorage storage;

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Upload a repair-evidence attachment")
    public ResponseEntity<?> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String entityType,
            @RequestParam Long entityId,
            Authentication authentication) {

        if (!ALLOWED_ENTITY_TYPES.contains(entityType)) {
            return ResponseEntity.badRequest().body("Unsupported entityType: " + entityType);
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            Attachment saved = attachmentService.upload(file, entityType, entityId, authentication.getName());
            return ResponseEntity.status(201).body(saved);
        } catch (IOException ex) {
            log.error("Failed to store attachment for {}#{}: {}", entityType, entityId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("Failed to store attachment");
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List attachments for an entity")
    public ResponseEntity<List<Attachment>> list(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(attachmentService.list(entityType, entityId));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'MANAGER', 'FINANCE_MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Download an attachment's file content")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment attachment = attachmentService.findById(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = storage.loadAsResource(attachment.getStoredFileName());
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = attachment.getContentType() != null
                ? MediaType.parseMediaType(attachment.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }
}
