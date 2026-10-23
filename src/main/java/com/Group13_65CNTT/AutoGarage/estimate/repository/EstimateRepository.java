package com.Group13_65CNTT.AutoGarage.estimate.repository;

import com.Group13_65CNTT.AutoGarage.estimate.entity.Estimate;
import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstimateRepository extends JpaRepository<Estimate, Long> {

    Optional<Estimate> findByEstimateCode(String estimateCode);

    List<Estimate> findByRepairOrderId(Long repairOrderId);

    Optional<Estimate> findByRepairOrderIdAndVersionNo(Long repairOrderId, Integer versionNo);

    Optional<Estimate> findTopByRepairOrderIdOrderByVersionNoDesc(Long repairOrderId);

    boolean existsByEstimateCode(String estimateCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Estimate e WHERE e.id = :id")
    Optional<Estimate> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT e FROM Estimate e WHERE " +
           "(:repairOrderId IS NULL OR e.repairOrderId = :repairOrderId) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Estimate> searchEstimates(@Param("repairOrderId") Long repairOrderId,
                                  @Param("status") EstimateStatus status,
                                  Pageable pageable);
}
