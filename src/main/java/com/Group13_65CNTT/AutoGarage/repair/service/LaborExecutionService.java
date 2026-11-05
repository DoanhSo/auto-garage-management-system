package com.Group13_65CNTT.AutoGarage.repair.service;

import java.time.Instant;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.LaborSessionStopRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.QcAssignmentRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskAssignmentRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskCompletionRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.LaborSessionResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskAssignmentResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskWorkSummaryResponse;

import java.util.List;

public interface LaborExecutionService {

    TaskAssignmentResponse assignTask(Long actorUserId, Long taskId, TaskAssignmentRequest request);

    /** Assigns the single QC inspector of an order waiting for QC, replacing any previous one. */
    TaskAssignmentResponse assignQcInspector(Long actorUserId, Long orderId, QcAssignmentRequest request);

    /** Active QC inspector assignment of the order, or {@code null} when none. */
    TaskAssignmentResponse getQcAssignment(Long orderId);

    void releaseAssignment(Long actorUserId, Long assignmentId);

    List<TaskAssignmentResponse> getAssignmentsByTaskId(Long taskId);

    LaborSessionResponse startTimer(Long actorUserId, Long assignmentId);

    LaborSessionResponse stopTimer(Long actorUserId, Long assignmentId, LaborSessionStopRequest request);

    LaborSessionResponse getActiveTimerByEmployee(Long employeeId);

    /** Pauses every running timer of the technician (end of shift) so it cannot run overnight. */
    void pauseOpenTimersForEmployee(Long employeeId, Instant at);

    TaskWorkSummaryResponse completeTask(Long actorUserId, Long taskId, TaskCompletionRequest request);

    TaskWorkSummaryResponse getTaskWorkSummary(Long taskId);
}
