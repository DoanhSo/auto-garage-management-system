package com.Group13_65CNTT.AutoGarage.repair.repository;

import com.Group13_65CNTT.AutoGarage.repair.entity.LaborSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaborSessionRepository extends JpaRepository<LaborSession, Long> {

    List<LaborSession> findByEmployeeIdAndEndsAtIsNull(Long employeeId);

    List<LaborSession> findByAssignmentId(Long assignmentId);

    @Query("SELECT COUNT(ls) FROM LaborSession ls " +
           "JOIN TaskAssignment ta ON ls.assignmentId = ta.id " +
           "JOIN RepairTask rt ON ta.taskId = rt.id " +
           "WHERE rt.repairOrderId = :repairOrderId AND ls.endsAt IS NULL")
    long countOpenSessionsByRepairOrderId(@Param("repairOrderId") Long repairOrderId);
}
