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

    @Query("""
            SELECT v FROM InvoiceBalanceView v WHERE (:customerId IS NULL OR v.customerId = :customerId)
            AND (:hasBalance IS NULL OR (:hasBalance = true AND v.balanceAmount > 0) OR (:hasBalance = false AND v.balanceAmount = 0))
            AND (:query IS NULL OR :query = ''
                 OR LOWER(v.invoiceCode) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR EXISTS (
                     SELECT 1 FROM Customer c
                     WHERE c.id = v.customerId
                       AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR (c.phone IS NOT NULL AND c.phone LIKE CONCAT('%', :query, '%'))
                            OR (c.companyName IS NOT NULL AND LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%'))))
                 )
                 OR EXISTS (
                     SELECT 1 FROM RepairOrder ro
                     WHERE ro.id = v.repairOrderId
                       AND LOWER(ro.orderCode) LIKE LOWER(CONCAT('%', :query, '%'))
                 )
                 OR EXISTS (
                     SELECT 1 FROM RepairOrder ro, Vehicle veh
                     WHERE ro.id = v.repairOrderId AND veh.id = ro.vehicleId
                       AND ((veh.licensePlate IS NOT NULL AND LOWER(veh.licensePlate) LIKE LOWER(CONCAT('%', :query, '%')))
                            OR (veh.vin IS NOT NULL AND LOWER(veh.vin) LIKE LOWER(CONCAT('%', :query, '%')))
                            OR (veh.brand IS NOT NULL AND LOWER(veh.brand) LIKE LOWER(CONCAT('%', :query, '%')))
                            OR (veh.model IS NOT NULL AND LOWER(veh.model) LIKE LOWER(CONCAT('%', :query, '%'))))
                 ))
            """)
    Page<InvoiceBalanceView> searchInvoiceBalances(
            @Param("customerId") Long customerId,
            @Param("hasBalance") Boolean hasBalance,
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.balanceAmount), 0) FROM InvoiceBalanceView v WHERE v.customerId = :customerId")
    BigDecimal calculateTotalOutstandingBalanceForCustomer(@Param("customerId") Long customerId);
}
