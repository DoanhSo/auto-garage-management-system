package com.Group13_65CNTT.AutoGarage.inventory.repository;

import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryDocumentItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InventoryDocumentItemRepository extends JpaRepository<InventoryDocumentItem, Long> {
    List<InventoryDocumentItem> findByDocumentIdOrderByLineNoAsc(Long documentId);

    List<InventoryDocumentItem> findBySourceItemId(Long sourceItemId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryDocumentItem i " +
           "WHERE i.sourceItem.id = :sourceItemId AND i.document.status = 'POSTED'")
    BigDecimal getTotalReturnedQuantityForSourceItem(@Param("sourceItemId") Long sourceItemId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryDocumentItem i " +
           "WHERE i.document.repairOrderId = :orderId AND i.part.id = :partId AND i.document.documentType = 'OUT' AND i.document.status = 'POSTED'")
    BigDecimal getTotalIssuedQuantityForOrderAndPart(@Param("orderId") Long orderId, @Param("partId") Long partId);
}
