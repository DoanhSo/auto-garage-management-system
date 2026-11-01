package com.Group13_65CNTT.AutoGarage.repair.service;

import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentStage;
import com.Group13_65CNTT.AutoGarage.repair.entity.LaborSession;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderAssignment;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import com.Group13_65CNTT.AutoGarage.repair.entity.WorkResultStatus;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Writes the vehicle-technician-shift audit trail; always joins the caller's transaction. */
@Component
@RequiredArgsConstructor
public class WorkLogRecorder {

    private final RepairOrderAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeShiftRepository shiftRepository;
    private final LaborSessionRepository laborSessionRepository;

    public Optional<EmployeeShift> activeShift(Long employeeId, Instant at) {
        List<EmployeeShift> shifts = shiftRepository.findActiveAt(employeeId, at);
        return shifts.stream().findFirst();
    }

    public RepairOrderAssignment start(
            RepairOrder order,
            Long employeeId,
            AssignmentStage stage,
            Long taskId,
            Instant at,
            String notes) {
        BigDecimal rate = employeeRepository.findById(employeeId)
                .map(Employee::getHourlyCost)
                .orElse(BigDecimal.ZERO);
        return assignmentRepository.save(RepairOrderAssignment.builder()
                .repairOrderId(order.getId())
                .vehicleId(order.getVehicleId())
                .employeeId(employeeId)
                .shiftId(activeShift(employeeId, at).map(EmployeeShift::getId).orElse(null))
                .assignmentStage(stage)
                .taskId(taskId)
                .startedAt(at)
                .hourlyRateSnapshot(rate)
                .notes(notes)
                .build());
    }

    /** Closes the repair rows of a task using the timer seconds already recorded for each assignment. */
    public void completeTask(Long taskId, Collection<TaskAssignment> assignments, Instant at, String note) {
        for (TaskAssignment assignment : assignments) {
            AssignmentStage stage = switch (assignment.getAssignmentRole()) {
                case MAIN -> AssignmentStage.REPAIR_MAIN;
                case ASSISTANT -> AssignmentStage.REPAIR_ASSISTANT;
                case QC -> null;
            };
            if (stage == null) {
                continue;
            }
            assignmentRepository
                    .findFirstByTaskIdAndEmployeeIdAndAssignmentStageOrderByIdDesc(
                            taskId, assignment.getEmployeeId(), stage)
                    .ifPresent(row -> {
                        row.setActualSeconds(trackedSeconds(assignment.getId(), at));
                        row.setCompletedAt(at);
                        row.setResultStatus(WorkResultStatus.COMPLETED);
                        if (note != null && !note.isBlank()) {
                            row.setNotes(note);
                        }
                        assignmentRepository.save(row);
                    });
        }
    }

    /** Closes the latest open row of a stage for an employee on an order. */
    public void finish(
            Long orderId,
            Long employeeId,
            AssignmentStage stage,
            Instant at,
            WorkResultStatus result,
            String notes) {
        assignmentRepository.findByRepairOrderIdAndAssignmentStage(orderId, stage).stream()
                .filter(row -> row.getEmployeeId().equals(employeeId) && row.getCompletedAt() == null)
                .findFirst()
                .ifPresent(row -> {
                    row.setCompletedAt(at.isBefore(row.getStartedAt()) ? row.getStartedAt() : at);
                    row.setActualSeconds(
                            Duration.between(row.getStartedAt(), row.getCompletedAt()).getSeconds());
                    row.setResultStatus(result);
                    if (notes != null && !notes.isBlank()) {
                        row.setNotes(notes);
                    }
                    assignmentRepository.save(row);
                });
    }

    private long trackedSeconds(Long assignmentId, Instant at) {
        return laborSessionRepository.findByAssignmentId(assignmentId).stream()
                .mapToLong(session -> secondsOf(session, at))
                .sum();
    }

    private long secondsOf(LaborSession session, Instant at) {
        Instant end = session.getEndsAt() != null ? session.getEndsAt() : at;
        return Math.max(0, Duration.between(session.getStartsAt(), end).getSeconds());
    }
}
