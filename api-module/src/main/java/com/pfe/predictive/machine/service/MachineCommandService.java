package com.pfe.predictive.machine.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.MachineStatus;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.machine.dto.CreateMachineRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class MachineCommandService {

    private final MachineRepository machineRepository;
    private final MachinePhotoStorage photoStorage;

    public MachineCommandService(MachineRepository machineRepository, MachinePhotoStorage photoStorage) {
        this.machineRepository = machineRepository;
        this.photoStorage = photoStorage;
    }

    public Machine createMachine(CreateMachineRequest request, MultipartFile photo) {
        if (machineRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new IllegalArgumentException("Machine with serial number already exists: " + request.getSerialNumber());
        }

        Machine machine = new Machine();
        machine.setSerialNumber(request.getSerialNumber());
        machine.setName(request.getName());
        machine.setDescription(request.getDescription());
        machine.setModel(request.getModel());
        machine.setManufacturer(request.getManufacturer());
        machine.setLocation(request.getLocation());
        machine.setCategory(request.getCategory());
        machine.setSubCategory(request.getSubCategory());
        machine.setStatus(resolveStatus(request.getStatus()));

        if (request.getInstallationYear() != null) {
            LocalDateTime installedAt = LocalDate.of(request.getInstallationYear(), 1, 1).atStartOfDay();
            machine.setInstallationDate(installedAt);
        }

        if (photo != null && !photo.isEmpty()) {
            MachinePhotoStorage.StoredPhoto stored = savePhoto(photo, request.getSerialNumber());
            machine.setPhotoPath(stored.getRelativePath());
            machine.setPhotoContentType(stored.getContentType());
        }

        return machineRepository.save(machine);
    }

    public Machine getMachine(Long id) {
        return machineRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Machine not found: " + id));
    }

    public PhotoDownload loadPhoto(Long id) {
        Machine machine = getMachine(id);
        Resource resource = photoStorage.loadAsResource(machine.getPhotoPath());
        if (resource == null || !resource.exists()) {
            return null;
        }
        return new PhotoDownload(resource, machine.getPhotoContentType());
    }

    private MachinePhotoStorage.StoredPhoto savePhoto(MultipartFile photo, String serialNumber) {
        try {
            return photoStorage.save(photo, serialNumber);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to store uploaded photo", ex);
        }
    }

    private String resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return MachineStatus.OPERATIONAL.name();
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        for (MachineStatus value : MachineStatus.values()) {
            if (value.name().equals(normalized)) {
                return value.name();
            }
        }
        throw new IllegalArgumentException("Unsupported machine status: " + status);
    }

    public static class PhotoDownload {
        private final Resource resource;
        private final String contentType;

        public PhotoDownload(Resource resource, String contentType) {
            this.resource = resource;
            this.contentType = contentType;
        }

        public Resource getResource() {
            return resource;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
