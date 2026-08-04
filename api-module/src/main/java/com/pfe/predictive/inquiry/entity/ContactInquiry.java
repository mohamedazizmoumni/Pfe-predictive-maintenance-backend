package com.pfe.predictive.inquiry.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A visitor-submitted inquiry from the public marketing site — either the
 * "Contact Us" form or the "Request a Demo" form. Both land here so admins
 * have one inbox to work from regardless of which form was used.
 */
@Entity
@Table(name = "contact_inquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false, length = 30)
    private InquiryType inquiryType;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 150)
    private String company;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;

    @Column(name = "created_date", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();
}
