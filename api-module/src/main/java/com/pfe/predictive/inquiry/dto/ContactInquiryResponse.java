package com.pfe.predictive.inquiry.dto;

import com.pfe.predictive.inquiry.entity.InquiryStatus;
import com.pfe.predictive.inquiry.entity.InquiryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInquiryResponse {
    private Long id;
    private InquiryType inquiryType;
    private String fullName;
    private String email;
    private String company;
    private String phone;
    private String subject;
    private String message;
    private InquiryStatus status;
    private String reviewedBy;
    private LocalDateTime reviewedDate;
    private LocalDateTime createdDate;
}
