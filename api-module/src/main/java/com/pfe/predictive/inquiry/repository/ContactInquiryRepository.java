package com.pfe.predictive.inquiry.repository;

import com.pfe.predictive.inquiry.entity.ContactInquiry;
import com.pfe.predictive.inquiry.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {

    Page<ContactInquiry> findAllByOrderByCreatedDateDesc(Pageable pageable);

    Page<ContactInquiry> findByStatusOrderByCreatedDateDesc(InquiryStatus status, Pageable pageable);

    long countByStatus(InquiryStatus status);
}
