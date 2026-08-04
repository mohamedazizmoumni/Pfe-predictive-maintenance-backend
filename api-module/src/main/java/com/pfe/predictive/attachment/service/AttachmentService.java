package com.pfe.predictive.attachment.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.core.entity.Attachment;
import com.pfe.predictive.data.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final RepairEvidenceStorage storage;
    private final AuditEventService auditEventService;

    public Attachment upload(MultipartFile file, String entityType, Long entityId, String uploadedBy) throws IOException {
        RepairEvidenceStorage.StoredFile stored = storage.save(file);

        Attachment attachment = Attachment.builder()
                .entityType(entityType)
                .entityId(entityId)
                .fileName(file.getOriginalFilename())
                .storedFileName(stored.storedFileName())
                .contentType(stored.contentType())
                .fileSize(stored.fileSize())
                .uploadedBy(uploadedBy)
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        auditEventService.record(uploadedBy, "ATTACHMENT_UPLOADED", entityType, entityId,
                "fileName=" + saved.getFileName());
        log.info("Attachment {} uploaded for {}#{} by {}", saved.getId(), entityType, entityId, uploadedBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Attachment> list(String entityType, Long entityId) {
        return attachmentRepository.findByEntityTypeAndEntityIdOrderByUploadedAtDesc(entityType, entityId);
    }

    @Transactional(readOnly = true)
    public Attachment findById(Long id) {
        return attachmentRepository.findById(id).orElse(null);
    }
}
