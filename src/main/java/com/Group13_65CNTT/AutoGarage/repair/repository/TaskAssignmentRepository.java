package com.Group13_65CNTT.AutoGarage.repair.repository;

import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTaskId(Long taskId);

    List<TaskAssignment> findByTaskIdAndIsActiveTrue(Long taskId);

    List<TaskAssignment> findByEmployeeIdAndIsActiveTrue(Long employeeId);
}
