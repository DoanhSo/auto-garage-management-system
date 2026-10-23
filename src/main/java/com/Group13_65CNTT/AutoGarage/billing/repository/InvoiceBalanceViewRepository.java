package com.Group13_65CNTT.AutoGarage.billing.repository;

import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceBalanceView;

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
public interface InvoiceBalanceViewRepository extends JpaRepository<InvoiceBalanceView, Long> {
    Optional<InvoiceBalanceView> findByInvoiceId(Long invoiceId);

    List<InvoiceBalanceView> findByCustomerId(Long customerId);

    @Query("SELECT v FROM InvoiceBalanceView v WHERE (:customerId IS NULL OR v.customerId = :customerId) " +
           "AND (:hasBalance IS NULL OR (:hasBalance = true AND v.balanceAmount > 0) OR (:hasBalance = false AND v.balanceAmount = 0)) " +
           "AND (:query IS NULL OR :query = '' OR LOWER(v.invoiceCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<InvoiceBalanceView> searchInvoiceBalances(
            @Param("customerId") Long customerId,
            @Param("hasBalance") Boolean hasBalance,
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.balanceAmount), 0) FROM InvoiceBalanceView v WHERE v.customerId = :customerId")
    BigDecimal calculateTotalOutstandingBalanceForCustomer(@Param("customerId") Long customerId);
}
