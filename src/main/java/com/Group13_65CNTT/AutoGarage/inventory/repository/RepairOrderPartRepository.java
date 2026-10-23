package com.Group13_65CNTT.AutoGarage.inventory.repository;

import com.Group13_65CNTT.AutoGarage.inventory.entity.RepairOrderPart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepairOrderPartRepository extends JpaRepository<RepairOrderPart, Long> {
    List<RepairOrderPart> findByRepairOrderId(Long repairOrderId);

    Optional<RepairOrderPart> findByEstimateItemId(Long estimateItemId);

    Optional<RepairOrderPart> findByRepairOrderIdAndPartId(Long repairOrderId, Long partId);
}
