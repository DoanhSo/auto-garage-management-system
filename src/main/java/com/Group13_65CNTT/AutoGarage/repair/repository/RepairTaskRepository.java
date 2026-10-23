package com.Group13_65CNTT.AutoGarage.repair.repository;

import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
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
public interface RepairTaskRepository extends JpaRepository<RepairTask, Long> {

    List<RepairTask> findByRepairOrderId(Long repairOrderId);

    List<RepairTask> findByRepairOrderIdAndStatusIn(Long repairOrderId, Collection<TaskStatus> statuses);

    long countByRepairOrderIdAndStatusNot(Long repairOrderId, TaskStatus status);

    Optional<RepairTask> findByEstimateItemId(Long estimateItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RepairTask rt WHERE rt.id = :id")
    Optional<RepairTask> findByIdForUpdate(@Param("id") Long id);

    boolean existsByRepairOrderIdAndStatusNot(Long repairOrderId, TaskStatus status);
}
