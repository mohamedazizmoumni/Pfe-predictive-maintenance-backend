package com.yourpackage.business.service;

import com.pfe.predictive.maintenancecost.entity.MaintenancePart;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartAvailabilityService {

    public boolean checkPartsAvailable(List<MaintenancePart> parts) {
        if (parts == null || parts.isEmpty()) {
            return true;
        }
        return parts.stream().allMatch(part -> part.getStockQuantity() != null && part.getStockQuantity() > 0);
    }

    public List<MaintenancePart> getUnavailableParts(List<MaintenancePart> parts) {
        if (parts == null || parts.isEmpty()) {
            return List.of();
        }
        return parts.stream()
                .filter(part -> part.getStockQuantity() != null && part.getStockQuantity() == 0)
                .toList();
    }

    public boolean isUrgentOrder(MaintenancePart part, int daysUntilFailure) {
        if (part == null || part.getLeadTimeDays() == null) {
            return false;
        }
        return part.getLeadTimeDays() >= daysUntilFailure;
    }

    public List<String> getMissingPartNames(List<MaintenancePart> parts) {
        return getUnavailableParts(parts).stream()
                .map(MaintenancePart::getName)
                .toList();
    }
}
