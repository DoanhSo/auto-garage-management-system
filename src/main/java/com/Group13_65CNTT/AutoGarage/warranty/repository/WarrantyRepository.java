package com.Group13_65CNTT.AutoGarage.warranty.repository;

import com.Group13_65CNTT.AutoGarage.warranty.entity.Warranty;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    Optional<Warranty> findByWarrantyCode(String warrantyCode);

    Optional<Warranty> findByInvoiceItemId(Long invoiceItemId);

    Page<Warranty> findByVehicleId(Long vehicleId, Pageable pageable);

    Page<Warranty> findByCustomerId(Long customerId, Pageable pageable);

    @Query("""
            select w from Warranty w
            where (:vehicleId is null or w.vehicleId = :vehicleId)
              and (:customerId is null or w.customerId = :customerId)
              and (:status is null or w.status = :status)
            """)
    Page<Warranty> searchWarranties(
            @Param("vehicleId") Long vehicleId,
            @Param("customerId") Long customerId,
            @Param("status") WarrantyStatus status,
            Pageable pageable);
}
