package com.Group13_65CNTT.AutoGarage.reception.repository;

import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleCheckRepository extends JpaRepository<VehicleCheck, Long> {

    List<VehicleCheck> findAllByRepairOrderIdOrderByCheckedAtDesc(Long repairOrderId);

    List<VehicleCheck> findAllByRepairOrderIdAndCheckTypeOrderByAttemptNoAsc(Long repairOrderId, CheckType checkType);

    Optional<VehicleCheck> findByRepairOrderIdAndCheckTypeAndAttemptNo(Long repairOrderId, CheckType checkType, Integer attemptNo);

    @Query("SELECT COALESCE(MAX(vc.attemptNo), 0) FROM VehicleCheck vc WHERE vc.repairOrderId = :repairOrderId AND vc.checkType = :checkType")
    int findMaxAttemptNo(@Param("repairOrderId") Long repairOrderId, @Param("checkType") CheckType checkType);
}
