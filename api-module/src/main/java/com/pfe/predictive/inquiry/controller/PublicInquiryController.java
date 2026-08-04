package com.pfe.predictive.inquiry.controller;

import com.pfe.predictive.inquiry.dto.ContactInquiryRequest;
import com.pfe.predictive.inquiry.dto.ContactInquiryResponse;
import com.pfe.predictive.inquiry.entity.InquiryType;
import com.pfe.predictive.inquiry.service.ContactInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated endpoints hit by the public marketing site's Contact and
 * Request-a-Demo forms. No JWT is available here — this is the one place in
 * the API a stranger on the internet can write data, so it only ever
 * creates ContactInquiry rows, never touches anything else.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicInquiryController {

    private final ContactInquiryService inquiryService;

    @PostMapping("/contact")
    public ResponseEntity<ContactInquiryResponse> submitContact(@Valid @RequestBody ContactInquiryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inquiryService.submit(request, InquiryType.CONTACT));
    }

    @PostMapping("/demo-request")
    public ResponseEntity<ContactInquiryResponse> submitDemoRequest(@Valid @RequestBody ContactInquiryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inquiryService.submit(request, InquiryType.DEMO_REQUEST));
    }
}
