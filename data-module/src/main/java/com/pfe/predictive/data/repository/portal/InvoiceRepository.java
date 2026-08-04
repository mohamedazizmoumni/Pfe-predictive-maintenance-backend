package com.pfe.predictive.data.repository.portal;

import com.pfe.predictive.core.entity.portal.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByCustomerUserIdOrderByIssueDateDesc(Long customerUserId);
}
