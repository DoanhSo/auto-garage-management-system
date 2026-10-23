package com.Group13_65CNTT.AutoGarage.warranty.repository;

import com.Group13_65CNTT.AutoGarage.warranty.entity.ClaimStatus;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {

    Optional<WarrantyClaim> findByClaimCode(String claimCode);

    Optional<WarrantyClaim> findByHandlingOrderId(Long handlingOrderId);

    Page<WarrantyClaim> findByWarrantyId(Long warrantyId, Pageable pageable);

    Page<WarrantyClaim> findByStatus(ClaimStatus status, Pageable pageable);
}
