package com.pfe.predictive.machine.mapper;

import com.pfe.predictive.machine.dto.MachineFailureReportDTO;
import com.pfe.predictive.machine.dto.RequiredPartDTO;
import com.pfe.predictive.machine.entity.MachineFailureReport;
import com.pfe.predictive.machine.entity.MachineFailureReportPart;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MachineMapper {

    public MachineFailureReportDTO toReportDto(MachineFailureReport report) {
        MachineFailureReportDTO dto = new MachineFailureReportDTO();
        dto.setId(report.getId());
        dto.setMachineId(report.getMachineId());
        dto.setMachineName(report.getMachineName());
        dto.setCurrentSensorState(report.getCurrentSensorState());
        dto.setPredictedFailureDays(report.getPredictedFailureDays());
        dto.setRisk(report.getRisk());
        dto.setRecommendedAction(report.getRecommendedAction());
        dto.setEstimatedCost(report.getEstimatedCost());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setRequiredParts(toRequiredPartDtos(report.getRequiredParts()));
        return dto;
    }

    public List<RequiredPartDTO> toRequiredPartDtos(List<MachineFailureReportPart> parts) {
        return parts.stream().map(this::toRequiredPartDto).toList();
    }

    public RequiredPartDTO toRequiredPartDto(MachineFailureReportPart part) {
        RequiredPartDTO dto = new RequiredPartDTO();
        dto.setPartId(part.getPartId());
        dto.setPartName(part.getPartName());
        dto.setQuantityNeeded(part.getQuantityNeeded());
        dto.setCurrentStock(part.getCurrentStock());
        dto.setMinimumStock(part.getMinimumStock());
        return dto;
    }
}
