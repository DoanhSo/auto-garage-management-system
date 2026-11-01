package com.Group13_65CNTT.AutoGarage.payroll.repository;

import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriod;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    boolean existsByPeriodMonth(String periodMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PayrollPeriod p WHERE p.id = :id")
    Optional<PayrollPeriod> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM PayrollPeriod p WHERE :yearPrefix IS NULL OR p.periodMonth LIKE CONCAT(:yearPrefix, '%')")
    Page<PayrollPeriod> search(@Param("yearPrefix") String yearPrefix, Pageable pageable);
}
