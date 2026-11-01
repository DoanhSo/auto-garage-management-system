package com.Group13_65CNTT.AutoGarage.repair.repository;

import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentStage;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepairOrderAssignmentRepository extends JpaRepository<RepairOrderAssignment, Long> {

    List<RepairOrderAssignment> findByVehicleIdOrderByStartedAtDesc(Long vehicleId);

    List<RepairOrderAssignment> findByRepairOrderIdOrderByStartedAtAsc(Long repairOrderId);

    List<RepairOrderAssignment> findByEmployeeIdOrderByStartedAtDesc(Long employeeId);

    List<RepairOrderAssignment> findByEmployeeIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
            Long employeeId, java.time.Instant from, java.time.Instant to);

    Optional<RepairOrderAssignment> findFirstByTaskIdAndEmployeeIdAndAssignmentStageOrderByIdDesc(
            Long taskId, Long employeeId, AssignmentStage stage);

    List<RepairOrderAssignment> findByRepairOrderIdAndAssignmentStage(Long repairOrderId, AssignmentStage stage);

    boolean existsByRepairOrderIdAndAssignmentStage(Long repairOrderId, AssignmentStage stage);
}
