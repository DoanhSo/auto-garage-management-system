package com.Group13_65CNTT.AutoGarage.warranty.repository;

import com.Group13_65CNTT.AutoGarage.warranty.entity.Warranty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    Optional<Warranty> findByWarrantyCode(String warrantyCode);

    Optional<Warranty> findByInvoiceItemId(Long invoiceItemId);

    Page<Warranty> findByVehicleId(Long vehicleId, Pageable pageable);

    Page<Warranty> findByCustomerId(Long customerId, Pageable pageable);
}
