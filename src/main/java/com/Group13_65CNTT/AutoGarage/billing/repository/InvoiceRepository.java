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

    /** The live invoice of an order; voided ones stay as records but never count. */
    @Query("SELECT i FROM Invoice i WHERE i.repairOrderId = :repairOrderId "
            + "AND i.status <> com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceStatus.VOID")
    Optional<Invoice> findActiveByRepairOrderId(@Param("repairOrderId") Long repairOrderId);

    /** Most recent invoice of an order, voided or not, for display when no live one exists. */
    Optional<Invoice> findFirstByRepairOrderIdOrderByIdDesc(Long repairOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.repairOrderId = :repairOrderId "
            + "AND i.status <> com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceStatus.VOID")
    Optional<Invoice> findActiveByRepairOrderIdWithLock(@Param("repairOrderId") Long repairOrderId);

    boolean existsByRepairOrderIdAndStatusNot(Long repairOrderId, InvoiceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdWithLock(@Param("id") Long id);

    @Query("""
            SELECT i FROM Invoice i WHERE (:status IS NULL OR i.status = :status)
            AND (:customerId IS NULL OR i.customerId = :customerId)
            AND (:orderId IS NULL OR i.repairOrderId = :orderId)
            AND (:query IS NULL OR :query = ''
                 OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(CAST(i.customerSnapshot AS string)) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(CAST(i.vehicleSnapshot AS string)) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR EXISTS (
                     SELECT 1 FROM Customer c
                     WHERE c.id = i.customerId
                       AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR (c.phone IS NOT NULL AND c.phone LIKE CONCAT('%', :query, '%'))
                            OR (c.companyName IS NOT NULL AND LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%'))))
                 )
                 OR EXISTS (
                     SELECT 1 FROM RepairOrder ro
                     WHERE ro.id = i.repairOrderId
                       AND LOWER(ro.orderCode) LIKE LOWER(CONCAT('%', :query, '%'))
                 )
                 OR EXISTS (
                     SELECT 1 FROM RepairOrder ro, Vehicle v
                     WHERE ro.id = i.repairOrderId AND v.id = ro.vehicleId
                       AND ((v.licensePlate IS NOT NULL AND LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :query, '%')))
                            OR (v.vin IS NOT NULL AND LOWER(v.vin) LIKE LOWER(CONCAT('%', :query, '%')))
                            OR (v.brand IS NOT NULL AND LOWER(v.brand) LIKE LOWER(CONCAT('%', :query, '%')))
                            OR (v.model IS NOT NULL AND LOWER(v.model) LIKE LOWER(CONCAT('%', :query, '%'))))
                 ))
            """)
    Page<Invoice> searchInvoices(
            @Param("status") InvoiceStatus status,
            @Param("customerId") Long customerId,
            @Param("orderId") Long orderId,
            @Param("query") String query,
            Pageable pageable);
}
