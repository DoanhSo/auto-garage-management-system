package com.Group13_65CNTT.AutoGarage.inventory.repository;

import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryTransaction;

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
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    Optional<InventoryTransaction> findByDocumentItemId(Long documentItemId);

    List<InventoryTransaction> findByPartIdOrderByCreatedAtAsc(Long partId);

    Page<InventoryTransaction> findByPartIdOrderByCreatedAtDesc(Long partId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.quantityChange), 0) FROM InventoryTransaction t WHERE t.part.id = :partId")
    BigDecimal calculateLedgerQuantityForPart(@Param("partId") Long partId);
}
