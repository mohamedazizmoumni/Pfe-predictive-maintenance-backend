package com.pfe.predictive.data.repository.portal;

import com.pfe.predictive.core.entity.portal.PortalMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortalMessageRepository extends JpaRepository<PortalMessage, Long> {

    List<PortalMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
