package com.Group13_65CNTT.AutoGarage.billing.repository;

import com.Group13_65CNTT.AutoGarage.billing.entity.Payment;
import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionCode(String transactionCode);

    boolean existsByTransactionCode(String transactionCode);

    boolean existsByProviderReference(String providerReference);

    Optional<Payment> findByProviderReference(String providerReference);

    List<Payment> findByInvoiceIdOrderByPaidAtAsc(Long invoiceId);

    List<Payment> findByInventoryDocumentIdOrderByPaidAtAsc(Long inventoryDocumentId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId AND p.direction = 'IN'")
    BigDecimal calculatePaidAmountForInvoice(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.inventoryDocumentId = :documentId AND p.direction = 'OUT'")
    BigDecimal calculatePaidAmountForDocument(@Param("documentId") Long documentId);

    @Query("SELECT p FROM Payment p WHERE (:direction IS NULL OR p.direction = :direction) " +
           "AND (:invoiceId IS NULL OR p.invoice.id = :invoiceId) " +
           "AND (:documentId IS NULL OR p.inventoryDocumentId = :documentId) " +
           "AND (:query IS NULL OR :query = '' OR LOWER(p.transactionCode) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.providerReference) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Payment> searchPayments(
            @Param("direction") PaymentDirection direction,
            @Param("invoiceId") Long invoiceId,
            @Param("documentId") Long documentId,
            @Param("query") String query,
            Pageable pageable);
}
