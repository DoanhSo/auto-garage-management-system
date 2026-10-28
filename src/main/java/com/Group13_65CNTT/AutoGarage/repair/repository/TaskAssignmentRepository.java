package com.Group13_65CNTT.AutoGarage.repair.repository;

import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentRole;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    @Query("SELECT a FROM TaskAssignment a JOIN RepairTask t ON t.id = a.taskId " +
            "WHERE t.repairOrderId = :orderId AND a.isActive = true AND a.assignmentRole = 'QC'")
    List<TaskAssignment> findActiveQcForOrder(@Param("orderId") Long orderId);

    List<TaskAssignment> findByTaskId(Long taskId);

    List<TaskAssignment> findByTaskIdAndIsActiveTrue(Long taskId);

    Optional<TaskAssignment> findByTaskIdAndAssignmentRoleAndIsActiveTrue(Long taskId, AssignmentRole assignmentRole);

    List<TaskAssignment> findByEmployeeIdAndIsActiveTrue(Long employeeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.id = :id")
    Optional<TaskAssignment> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.employeeId = :employeeId " +
           "AND ta.isActive = true " +
           "AND ta.startsAt < :endsAt AND ta.endsAt > :startsAt")
    List<TaskAssignment> findOverlappingAssignments(@Param("employeeId") Long employeeId,
                                                    @Param("startsAt") Instant startsAt,
                                                    @Param("endsAt") Instant endsAt);
}
