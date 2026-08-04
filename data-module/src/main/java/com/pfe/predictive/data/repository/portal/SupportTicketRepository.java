package com.pfe.predictive.data.repository.portal;

import com.pfe.predictive.core.entity.portal.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Page<SupportTicket> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId, Pageable pageable);

    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByCustomerUserIdAndStatusNot(Long customerUserId, com.pfe.predictive.core.entity.portal.SupportTicketStatus status);

    long countByStatusIn(java.util.List<com.pfe.predictive.core.entity.portal.SupportTicketStatus> statuses);
}
