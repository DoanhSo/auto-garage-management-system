package com.Group13_65CNTT.AutoGarage.payroll.repository;

import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionStatus;
import com.Group13_65CNTT.AutoGarage.payroll.entity.OrderCommission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderCommissionRepository extends JpaRepository<OrderCommission, Long> {

    boolean existsByRepairOrderId(Long repairOrderId);

    List<OrderCommission> findByPayrollItemId(Long payrollItemId);

    @Query("SELECT c FROM OrderCommission c WHERE (:employeeId IS NULL OR c.employeeId = :employeeId) "
            + "AND (:repairOrderId IS NULL OR c.repairOrderId = :repairOrderId) "
            + "AND (:status IS NULL OR c.status = :status) "
            + "AND (:fromEarnedAt IS NULL OR c.earnedAt >= :fromEarnedAt) "
            + "AND (:toEarnedAt IS NULL OR c.earnedAt < :toEarnedAt)")
    Page<OrderCommission> search(
            @Param("employeeId") Long employeeId,
            @Param("repairOrderId") Long repairOrderId,
            @Param("status") CommissionStatus status,
            @Param("fromEarnedAt") Instant fromEarnedAt,
            @Param("toEarnedAt") Instant toEarnedAt,
            Pageable pageable);

    @Query("SELECT c FROM OrderCommission c WHERE c.status = :status AND c.payrollItemId IS NULL "
            + "AND c.earnedAt >= :from AND c.earnedAt < :to")
    List<OrderCommission> findUnlinked(
            @Param("status") CommissionStatus status, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(DISTINCT c.repairOrderId) FROM OrderCommission c WHERE c.payrollItemId IN "
            + "(SELECT i.id FROM PayrollItem i WHERE i.payrollPeriodId = :periodId)")
    long countDistinctOrdersByPeriod(@Param("periodId") Long periodId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE OrderCommission c SET c.payrollItemId = NULL WHERE c.status <> :paid AND c.payrollItemId IN "
            + "(SELECT i.id FROM PayrollItem i WHERE i.payrollPeriodId = :periodId)")
    int unlinkFromPeriod(@Param("periodId") Long periodId, @Param("paid") CommissionStatus paid);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE OrderCommission c SET c.status = :paid WHERE c.status = :approved AND c.payrollItemId IN "
            + "(SELECT i.id FROM PayrollItem i WHERE i.payrollPeriodId = :periodId)")
    int markPaidForPeriod(
            @Param("periodId") Long periodId,
            @Param("approved") CommissionStatus approved,
            @Param("paid") CommissionStatus paid);
}
