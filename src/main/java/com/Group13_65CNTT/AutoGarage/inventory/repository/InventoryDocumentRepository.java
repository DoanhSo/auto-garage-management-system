package com.Group13_65CNTT.AutoGarage.inventory.repository;

import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentStatus;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryDocument;

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
public interface InventoryDocumentRepository extends JpaRepository<InventoryDocument, Long> {
    Optional<InventoryDocument> findByDocumentCode(String documentCode);

    boolean existsByDocumentCode(String documentCode);

    boolean existsBySupplierIdAndSupplierInvoiceNumber(Long supplierId, String supplierInvoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM InventoryDocument d WHERE d.id = :id")
    Optional<InventoryDocument> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT d FROM InventoryDocument d WHERE (:type IS NULL OR d.documentType = :type) " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:supplierId IS NULL OR d.supplier.id = :supplierId) " +
           "AND (:orderId IS NULL OR d.repairOrderId = :orderId) " +
           "AND (:query IS NULL OR :query = '' OR LOWER(d.documentCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<InventoryDocument> searchDocuments(
            @Param("type") DocumentType type,
            @Param("status") DocumentStatus status,
            @Param("supplierId") Long supplierId,
            @Param("orderId") Long orderId,
            @Param("query") String query,
            Pageable pageable);
}
