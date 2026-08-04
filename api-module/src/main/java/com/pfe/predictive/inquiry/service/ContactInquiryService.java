package com.pfe.predictive.inquiry.service;

import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.inquiry.dto.ContactInquiryRequest;
import com.pfe.predictive.inquiry.dto.ContactInquiryResponse;
import com.pfe.predictive.inquiry.entity.ContactInquiry;
import com.pfe.predictive.inquiry.entity.InquiryStatus;
import com.pfe.predictive.inquiry.entity.InquiryType;
import com.pfe.predictive.inquiry.repository.ContactInquiryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContactInquiryService {

    private final ContactInquiryRepository inquiryRepository;
    private final EmailService emailService;

    /**
     * Persists the inquiry first, then best-effort notifies admins by email
     * — a slow/broken SMTP relay must never fail the visitor's form
     * submission, since the record itself is already saved and visible in
     * the admin inbox either way.
     */
    public ContactInquiryResponse submit(ContactInquiryRequest request, InquiryType type) {
        ContactInquiry inquiry = ContactInquiry.builder()
                .inquiryType(type)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .company(request.getCompany())
                .phone(request.getPhone())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(InquiryStatus.NEW)
                .build();

        ContactInquiry saved = inquiryRepository.save(inquiry);
        log.info("New {} inquiry #{} from {}", type, saved.getId(), saved.getEmail());

        try {
            String typeLabel = type == InquiryType.DEMO_REQUEST ? "Demo Request" : "Contact Message";
            emailService.sendInquiryNotification(
                    typeLabel, saved.getFullName(), saved.getEmail(),
                    saved.getCompany(), saved.getPhone(), saved.getSubject(), saved.getMessage());
        } catch (Exception ex) {
            log.warn("Inquiry #{} saved but admin notification email failed to queue: {}", saved.getId(), ex.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ContactInquiryResponse> list(InquiryStatus status, Pageable pageable) {
        Page<ContactInquiry> page = status != null
                ? inquiryRepository.findByStatusOrderByCreatedDateDesc(status, pageable)
                : inquiryRepository.findAllByOrderByCreatedDateDesc(pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long countNew() {
        return inquiryRepository.countByStatus(InquiryStatus.NEW);
    }

    public ContactInquiryResponse updateStatus(Long id, InquiryStatus status, String reviewedBy) {
        ContactInquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inquiry not found: " + id));

        inquiry.setStatus(status);
        inquiry.setReviewedBy(reviewedBy);
        inquiry.setReviewedDate(LocalDateTime.now());

        return toResponse(inquiryRepository.save(inquiry));
    }

    private ContactInquiryResponse toResponse(ContactInquiry inquiry) {
        return ContactInquiryResponse.builder()
                .id(inquiry.getId())
                .inquiryType(inquiry.getInquiryType())
                .fullName(inquiry.getFullName())
                .email(inquiry.getEmail())
                .company(inquiry.getCompany())
                .phone(inquiry.getPhone())
                .subject(inquiry.getSubject())
                .message(inquiry.getMessage())
                .status(inquiry.getStatus())
                .reviewedBy(inquiry.getReviewedBy())
                .reviewedDate(inquiry.getReviewedDate())
                .createdDate(inquiry.getCreatedDate())
                .build();
    }
}
