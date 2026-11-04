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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM WarrantyClaim c WHERE c.id = :id")
    Optional<WarrantyClaim> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<WarrantyClaim> findByClaimCode(String claimCode);

    Optional<WarrantyClaim> findByHandlingOrderId(Long handlingOrderId);

    Page<WarrantyClaim> findByWarrantyId(Long warrantyId, Pageable pageable);

    Page<WarrantyClaim> findByStatus(ClaimStatus status, Pageable pageable);

    boolean existsByWarrantyIdAndStatusIn(Long warrantyId, java.util.Collection<ClaimStatus> statuses);

    @org.springframework.data.jpa.repository.Query("""
            select c from WarrantyClaim c
            where (:status is null or c.status = :status)
              and exists (
                  select w.id from Warranty w
                  where w.id = c.warrantyId
                    and (:vehicleId is null or w.vehicleId = :vehicleId)
                    and (:customerId is null or w.customerId = :customerId))
            """)
    Page<WarrantyClaim> searchClaims(
            @org.springframework.data.repository.query.Param("vehicleId") Long vehicleId,
            @org.springframework.data.repository.query.Param("customerId") Long customerId,
            @org.springframework.data.repository.query.Param("status") ClaimStatus status,
            Pageable pageable);
}
