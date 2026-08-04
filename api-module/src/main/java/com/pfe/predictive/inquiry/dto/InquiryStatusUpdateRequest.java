package com.pfe.predictive.inquiry.dto;

import com.pfe.predictive.inquiry.entity.InquiryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InquiryStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private InquiryStatus status;
}
