package com.Group13_65CNTT.AutoGarage.payroll.repository;

import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long> {

    List<PayrollItem> findByPayrollPeriodIdOrderByEmployeeIdAsc(Long payrollPeriodId);

    Optional<PayrollItem> findByPayrollPeriodIdAndEmployeeId(Long payrollPeriodId, Long employeeId);

    Optional<PayrollItem> findByIdAndPayrollPeriodId(Long id, Long payrollPeriodId);

    @Query("SELECT i FROM PayrollItem i JOIN PayrollPeriod p ON p.id = i.payrollPeriodId "
            + "WHERE i.employeeId = :employeeId AND p.status IN "
            + "(com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriodStatus.APPROVED, "
            + "com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriodStatus.PAID)")
    Page<PayrollItem> findPublishedByEmployee(@Param("employeeId") Long employeeId, Pageable pageable);
}
