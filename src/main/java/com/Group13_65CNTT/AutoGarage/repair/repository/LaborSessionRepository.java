package com.Group13_65CNTT.AutoGarage.repair.repository;

import com.Group13_65CNTT.AutoGarage.repair.entity.LaborSession;
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
public interface LaborSessionRepository extends JpaRepository<LaborSession, Long> {

    List<LaborSession> findByEmployeeIdAndEndsAtIsNull(Long employeeId);

    Optional<LaborSession> findTopByEmployeeIdAndEndsAtIsNull(Long employeeId);

    Optional<LaborSession> findTopByAssignmentIdAndEndsAtIsNull(Long assignmentId);

    List<LaborSession> findByAssignmentId(Long assignmentId);

    List<LaborSession> findByAssignmentIdIn(Collection<Long> assignmentIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ls FROM LaborSession ls WHERE ls.id = :id")
    Optional<LaborSession> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COUNT(ls) FROM LaborSession ls " +
           "JOIN TaskAssignment ta ON ls.assignmentId = ta.id " +
           "JOIN RepairTask rt ON ta.taskId = rt.id " +
           "WHERE rt.repairOrderId = :repairOrderId AND ls.endsAt IS NULL")
    long countOpenSessionsByRepairOrderId(@Param("repairOrderId") Long repairOrderId);

    @Query("SELECT ls FROM LaborSession ls " +
           "JOIN TaskAssignment ta ON ls.assignmentId = ta.id " +
           "WHERE ta.taskId = :taskId AND ls.endsAt IS NULL")
    List<LaborSession> findOpenSessionsByTaskId(@Param("taskId") Long taskId);
}
