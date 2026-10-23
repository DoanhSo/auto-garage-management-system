package com.Group13_65CNTT.AutoGarage.billing.repository;

import com.Group13_65CNTT.AutoGarage.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    Optional<Invoice> findByRepairOrderId(Long repairOrderId);
}
