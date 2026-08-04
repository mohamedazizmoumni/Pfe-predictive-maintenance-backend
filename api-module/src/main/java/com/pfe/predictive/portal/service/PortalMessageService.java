package com.pfe.predictive.portal.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.core.entity.portal.PortalMessage;
import com.pfe.predictive.core.entity.portal.SupportTicket;
import com.pfe.predictive.data.repository.portal.PortalMessageRepository;
import com.pfe.predictive.data.repository.portal.SupportTicketRepository;
import com.pfe.predictive.portal.dto.PortalMessageRequest;
import com.pfe.predictive.portal.dto.PortalMessageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Message thread on a support ticket — "communicate with maintenance
 * managers" portal capability. Deliberately no separate PortalMessage
 * controller-level role split: authorization is entity-level (must be the
 * ticket's own customer, or an internal Manager/Admin) rather than a
 * blanket role check, since both sides post to the same thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PortalMessageService {

    private final PortalMessageRepository messageRepository;
    private final SupportTicketRepository ticketRepository;
    private final EmailService emailService;
    private final AuditEventService auditEventService;

    public PortalMessageResponse postMessage(Long ticketId, PortalMessageRequest request, String senderUsername, boolean isCustomer, Long callerCustomerUserId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found: " + ticketId));

        if (isCustomer && !ticket.getCustomerUserId().equals(callerCustomerUserId)) {
            throw new AccessDeniedException("This support ticket does not belong to the authenticated customer");
        }

        PortalMessage message = PortalMessage.builder()
                .ticketId(ticketId)
                .senderUsername(senderUsername)
                .fromCustomer(isCustomer)
                .body(request.getBody())
                .build();
        PortalMessage saved = messageRepository.save(message);

        auditEventService.record(senderUsername, "PORTAL_MESSAGE_POSTED", "SupportTicket", ticketId, null);
        notifyOtherParty(ticket, isCustomer);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PortalMessageResponse> getThread(Long ticketId, boolean isCustomer, Long callerCustomerUserId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found: " + ticketId));

        if (isCustomer && !ticket.getCustomerUserId().equals(callerCustomerUserId)) {
            throw new AccessDeniedException("This support ticket does not belong to the authenticated customer");
        }

        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void notifyOtherParty(SupportTicket ticket, boolean fromCustomer) {
        try {
            if (fromCustomer) {
                emailService.sendEmailToUsersByRoles(List.of("MANAGER", "ADMIN"),
                        "New reply on support ticket: " + ticket.getSubject(),
                        "<p>The customer replied on ticket #" + ticket.getId() + " (" + ticket.getSubject() + ").</p>");
            }
            // Notifying the customer of a staff reply would go through the
            // regular in-app notification system once the portal has one —
            // out of scope for this pass, see project memory.
        } catch (Exception ex) {
            log.error("Failed to notify on new portal message for ticket {}: {}", ticket.getId(), ex.getMessage(), ex);
        }
    }

    private PortalMessageResponse toResponse(PortalMessage message) {
        return PortalMessageResponse.builder()
                .id(message.getId())
                .ticketId(message.getTicketId())
                .senderUsername(message.getSenderUsername())
                .fromCustomer(message.isFromCustomer())
                .body(message.getBody())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
