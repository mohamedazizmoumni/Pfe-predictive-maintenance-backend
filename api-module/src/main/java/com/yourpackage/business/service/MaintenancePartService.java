package com.yourpackage.business.service;

import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import com.pfe.predictive.maintenancecost.repository.MaintenancePartRepository;
import com.yourpackage.business.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaintenancePartService {

    private final MaintenancePartRepository maintenancePartRepository;

    public MaintenancePartService(MaintenancePartRepository maintenancePartRepository) {
        this.maintenancePartRepository = maintenancePartRepository;
    }

    public List<MaintenancePart> findAll() {
        return maintenancePartRepository.findAll();
    }

    public MaintenancePart findById(Long id) {
        return maintenancePartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance part not found with id: " + id));
    }

    public MaintenancePart create(MaintenancePart part) {
        return maintenancePartRepository.save(part);
    }

    public void delete(Long id) {
        MaintenancePart existing = findById(id);
        maintenancePartRepository.delete(existing);
    }
}
