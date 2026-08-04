package com.pfe.predictive.portal.service;

import com.pfe.predictive.audit.service.AuditEventService;
import com.pfe.predictive.common.service.EmailService;
import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.core.entity.portal.SupportTicket;
import com.pfe.predictive.core.entity.portal.SupportTicketStatus;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.data.repository.portal.SupportTicketRepository;
import com.pfe.predictive.portal.dto.SupportTicketRequest;
import com.pfe.predictive.portal.dto.SupportTicketResponse;
import com.pfe.predictive.portal.dto.SupportTicketUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final PortalAccessService portalAccessService;
    private final EmailService emailService;
    private final AuditEventService auditEventService;

    public SupportTicketResponse createTicket(Long customerUserId, SupportTicketRequest request) {
        portalAccessService.requireLinkedMachine(customerUserId, request.getMachineId());

        SupportTicket ticket = SupportTicket.builder()
                .customerUserId(customerUserId)
                .machineId(request.getMachineId())
                .subject(request.getSubject())
                .description(request.getDescription())
                .status(SupportTicketStatus.OPEN)
                .build();

        SupportTicket saved = ticketRepository.save(ticket);

        auditEventService.record("SUPPORT_TICKET_CREATED", "SupportTicket", saved.getId(),
                "machineId=" + saved.getMachineId());

        notifyManagersOfNewTicket(saved);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getMyTickets(Long customerUserId, Pageable pageable) {
        return ticketRepository.findByCustomerUserIdOrderByCreatedAtDesc(customerUserId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getAllTickets(Pageable pageable) {
        return ticketRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    public SupportTicketResponse updateTicket(Long id, SupportTicketUpdateRequest request, String updatedBy) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found: " + id));

        ticket.setStatus(request.getStatus());
        ticket.setAssignedTo(updatedBy);
        if (request.getResolutionNotes() != null) {
            ticket.setResolutionNotes(request.getResolutionNotes());
        }
        if (request.getStatus() == SupportTicketStatus.RESOLVED || request.getStatus() == SupportTicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        SupportTicket saved = ticketRepository.save(ticket);
        auditEventService.record(updatedBy, "SUPPORT_TICKET_UPDATED", "SupportTicket", saved.getId(),
                "status=" + saved.getStatus());
        return toResponse(saved);
    }

    private void notifyManagersOfNewTicket(SupportTicket ticket) {
        try {
            Machine machine = machineRepository.findById(ticket.getMachineId()).orElse(null);
            String machineName = machine != null ? machine.getName() : ("Machine #" + ticket.getMachineId());
            String subject = "New customer support ticket: " + ticket.getSubject();
            String body = "<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#111827;\">"
                    + "<h2 style=\"margin:0 0 12px 0;\">New Support Ticket</h2>"
                    + "<p><strong>Machine:</strong> " + machineName + "</p>"
                    + "<p><strong>Subject:</strong> " + ticket.getSubject() + "</p>"
                    + "<p><strong>Description:</strong> " + (ticket.getDescription() != null ? ticket.getDescription() : "—") + "</p>"
                    + "</div>";
            emailService.sendEmailToUsersByRoles(List.of("MANAGER", "ADMIN"), subject, body);
        } catch (Exception ex) {
            log.error("Failed to notify managers of new support ticket {}: {}", ticket.getId(), ex.getMessage(), ex);
        }
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        Machine machine = machineRepository.findById(ticket.getMachineId()).orElse(null);
        User customer = userRepository.findById(ticket.getCustomerUserId()).orElse(null);
        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .machineId(ticket.getMachineId())
                .machineName(machine != null ? machine.getName() : null)
                .customerName(customer != null ? customer.getDisplayName() : null)
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .status(ticket.getStatus().name())
                .assignedTo(ticket.getAssignedTo())
                .resolutionNotes(ticket.getResolutionNotes())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }
}
