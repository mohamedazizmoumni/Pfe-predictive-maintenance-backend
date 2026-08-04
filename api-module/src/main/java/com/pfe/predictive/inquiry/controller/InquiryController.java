package com.pfe.predictive.inquiry.controller;

import com.pfe.predictive.inquiry.dto.ContactInquiryResponse;
import com.pfe.predictive.inquiry.dto.InquiryStatusUpdateRequest;
import com.pfe.predictive.inquiry.entity.InquiryStatus;
import com.pfe.predictive.inquiry.service.ContactInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-facing inbox for inquiries submitted through the public marketing
 * site's Contact / Request-a-Demo forms.
 */
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class InquiryController {

    private final ContactInquiryService inquiryService;

    @GetMapping
    public Page<ContactInquiryResponse> list(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inquiryService.list(status, PageRequest.of(page, size));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", inquiryService.countNew());
    }

    @PatchMapping("/{id}/status")
    public ContactInquiryResponse updateStatus(@PathVariable Long id,
                                                @Valid @RequestBody InquiryStatusUpdateRequest request,
                                                Authentication authentication) {
        return inquiryService.updateStatus(id, request.getStatus(), authentication.getName());
    }
}
