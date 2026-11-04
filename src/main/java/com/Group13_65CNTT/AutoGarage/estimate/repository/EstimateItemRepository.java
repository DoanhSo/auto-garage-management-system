package com.Group13_65CNTT.AutoGarage.estimate.repository;

import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstimateItemRepository extends JpaRepository<EstimateItem, Long> {

    List<EstimateItem> findByEstimateIdOrderByLineNoAsc(Long estimateId);

    List<EstimateItem> findByEstimateIdIn(Collection<Long> estimateIds);

    /** True once the customer has approved at least one line of any estimate on the order. */
    @Query("SELECT COUNT(ei) > 0 FROM EstimateItem ei, Estimate e "
            + "WHERE ei.estimateId = e.id AND e.repairOrderId = :repairOrderId AND ei.approvalStatus = :status")
    boolean existsLineByRepairOrderIdAndStatus(@Param("repairOrderId") Long repairOrderId,
            @Param("status") com.Group13_65CNTT.AutoGarage.estimate.entity.ApprovalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ei FROM EstimateItem ei WHERE ei.id = :id")
    Optional<EstimateItem> findByIdForUpdate(@Param("id") Long id);
}
