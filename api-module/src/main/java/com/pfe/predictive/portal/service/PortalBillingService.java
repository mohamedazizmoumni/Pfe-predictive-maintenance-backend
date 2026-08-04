package com.pfe.predictive.portal.service;

import com.pfe.predictive.core.entity.Machine;
import com.pfe.predictive.core.entity.portal.CustomerMachineLink;
import com.pfe.predictive.core.entity.portal.Invoice;
import com.pfe.predictive.core.entity.portal.InvoiceStatus;
import com.pfe.predictive.core.entity.portal.Warranty;
import com.pfe.predictive.data.repository.MachineRepository;
import com.pfe.predictive.data.repository.portal.CustomerMachineLinkRepository;
import com.pfe.predictive.data.repository.portal.InvoiceRepository;
import com.pfe.predictive.data.repository.portal.WarrantyRepository;
import com.pfe.predictive.portal.dto.InvoiceRequest;
import com.pfe.predictive.portal.dto.InvoiceResponse;
import com.pfe.predictive.portal.dto.WarrantyRequest;
import com.pfe.predictive.portal.dto.WarrantyResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PortalBillingService {

    private final WarrantyRepository warrantyRepository;
    private final InvoiceRepository invoiceRepository;
    private final MachineRepository machineRepository;
    private final CustomerMachineLinkRepository linkRepository;

    // ── Warranties ──────────────────────────────────────────────────────

    public WarrantyResponse createWarranty(WarrantyRequest request) {
        Warranty warranty = Warranty.builder()
                .machineId(request.getMachineId())
                .provider(request.getProvider())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .terms(request.getTerms())
                .build();
        return toResponse(warrantyRepository.save(warranty));
    }

    public void deleteWarranty(Long id) {
        if (!warrantyRepository.existsById(id)) {
            throw new EntityNotFoundException("Warranty not found: " + id);
        }
        warrantyRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<WarrantyResponse> getWarrantiesForCustomer(Long customerUserId) {
        List<Long> machineIds = linkRepository.findByUserId(customerUserId).stream()
                .map(CustomerMachineLink::getMachineId)
                .toList();
        return warrantyRepository.findByMachineIdInOrderByEndDateDesc(machineIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarrantyResponse> getWarrantiesForMachine(Long machineId) {
        return warrantyRepository.findByMachineIdOrderByEndDateDesc(machineId).stream()
                .map(this::toResponse)
                .toList();
    }

    private WarrantyResponse toResponse(Warranty warranty) {
        Machine machine = machineRepository.findById(warranty.getMachineId()).orElse(null);
        return WarrantyResponse.builder()
                .id(warranty.getId())
                .machineId(warranty.getMachineId())
                .machineName(machine != null ? machine.getName() : null)
                .provider(warranty.getProvider())
                .startDate(warranty.getStartDate())
                .endDate(warranty.getEndDate())
                .terms(warranty.getTerms())
                .active(warranty.isActive())
                .build();
    }

    // ── Invoices ─────────────────────────────────────────────────────────

    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Invoice invoice = Invoice.builder()
                .customerUserId(request.getCustomerUserId())
                .machineId(request.getMachineId())
                .invoiceNumber(request.getInvoiceNumber())
                .amount(request.getAmount())
                .status(request.getStatus() != null ? request.getStatus() : InvoiceStatus.UNPAID)
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .description(request.getDescription())
                .build();
        return toResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse updateInvoiceStatus(Long id, InvoiceStatus status) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        invoice.setStatus(status);
        return toResponse(invoiceRepository.save(invoice));
    }

    public void deleteInvoice(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new EntityNotFoundException("Invoice not found: " + id);
        }
        invoiceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesForCustomer(Long customerUserId) {
        return invoiceRepository.findByCustomerUserIdOrderByIssueDateDesc(customerUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream().map(this::toResponse).toList();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        Machine machine = invoice.getMachineId() != null ? machineRepository.findById(invoice.getMachineId()).orElse(null) : null;
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .customerUserId(invoice.getCustomerUserId())
                .machineId(invoice.getMachineId())
                .machineName(machine != null ? machine.getName() : null)
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(invoice.getAmount())
                .status(invoice.getStatus().name())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .description(invoice.getDescription())
                .build();
    }
}
