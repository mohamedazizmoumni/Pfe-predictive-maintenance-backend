package com.pfe.predictive.template.service;

import com.pfe.predictive.core.entity.template.WorkOrderTemplate;
import com.pfe.predictive.data.repository.template.WorkOrderTemplateRepository;
import com.pfe.predictive.template.dto.WorkOrderTemplateRequest;
import com.pfe.predictive.template.dto.WorkOrderTemplateResponse;
import com.pfe.predictive.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkOrderTemplateService {

    private final WorkOrderTemplateRepository repository;

    public WorkOrderTemplateResponse create(WorkOrderTemplateRequest request, String createdBy) {
        WorkOrderTemplate template = WorkOrderTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .priority(request.getPriority())
                .estimatedDuration(request.getEstimatedDuration())
                .defaultNotes(request.getDefaultNotes())
                .active(request.getActive() == null || request.getActive())
                .createdBy(createdBy)
                .build();
        return toResponse(repository.save(template));
    }

    public WorkOrderTemplateResponse update(Long id, WorkOrderTemplateRequest request) {
        WorkOrderTemplate template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getType() != null) template.setType(request.getType());
        if (request.getPriority() != null) template.setPriority(request.getPriority());
        if (request.getEstimatedDuration() != null) template.setEstimatedDuration(request.getEstimatedDuration());
        if (request.getDefaultNotes() != null) template.setDefaultNotes(request.getDefaultNotes());
        if (request.getActive() != null) template.setActive(request.getActive());
        return toResponse(repository.save(template));
    }

    public void deactivate(Long id) {
        WorkOrderTemplate template = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        template.setActive(false);
        repository.save(template);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderTemplateResponse> getAll(boolean activeOnly) {
        List<WorkOrderTemplate> templates = activeOnly ? repository.findByActiveTrue() : repository.findAll();
        return templates.stream().map(this::toResponse).toList();
    }

    private WorkOrderTemplateResponse toResponse(WorkOrderTemplate template) {
        return WorkOrderTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .type(template.getType())
                .priority(template.getPriority())
                .estimatedDuration(template.getEstimatedDuration())
                .defaultNotes(template.getDefaultNotes())
                .active(template.isActive())
                .createdDate(template.getCreatedDate())
                .build();
    }
}
