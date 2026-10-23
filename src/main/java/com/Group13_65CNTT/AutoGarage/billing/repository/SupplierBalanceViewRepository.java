package com.Group13_65CNTT.AutoGarage.billing.repository;

import com.Group13_65CNTT.AutoGarage.billing.entity.SupplierBalanceView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierBalanceViewRepository extends JpaRepository<SupplierBalanceView, Long> {
    Optional<SupplierBalanceView> findBySupplierId(Long supplierId);

    @Query("SELECT v FROM SupplierBalanceView v WHERE (:hasBalance IS NULL OR (:hasBalance = true AND v.balanceAmount > 0) OR (:hasBalance = false AND v.balanceAmount = 0)) " +
           "AND (:query IS NULL OR :query = '' OR LOWER(v.supplierCode) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(v.supplierName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<SupplierBalanceView> searchSupplierBalances(
            @Param("hasBalance") Boolean hasBalance,
            @Param("query") String query,
            Pageable pageable);
}
