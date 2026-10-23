package com.Group13_65CNTT.AutoGarage.billing.repository;

import com.Group13_65CNTT.AutoGarage.billing.entity.Invoice;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    Optional<Invoice> findByRepairOrderId(Long repairOrderId);

    boolean existsByRepairOrderId(Long repairOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT i FROM Invoice i WHERE (:status IS NULL OR i.status = :status) " +
           "AND (:customerId IS NULL OR i.customerId = :customerId) " +
           "AND (:orderId IS NULL OR i.repairOrderId = :orderId) " +
           "AND (:query IS NULL OR :query = '' OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Invoice> searchInvoices(
            @Param("status") InvoiceStatus status,
            @Param("customerId") Long customerId,
            @Param("orderId") Long orderId,
            @Param("query") String query,
            Pageable pageable);
}
