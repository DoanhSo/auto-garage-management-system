package com.Group13_65CNTT.AutoGarage.repair.service;

import com.Group13_65CNTT.AutoGarage.repair.dto.request.LaborSessionStopRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskAssignmentRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskCompletionRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.LaborSessionResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskAssignmentResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskWorkSummaryResponse;

import java.util.List;

public interface LaborExecutionService {

    TaskAssignmentResponse assignTask(Long actorUserId, Long taskId, TaskAssignmentRequest request);

    void releaseAssignment(Long actorUserId, Long assignmentId);

    List<TaskAssignmentResponse> getAssignmentsByTaskId(Long taskId);

    LaborSessionResponse startTimer(Long actorUserId, Long assignmentId);

    LaborSessionResponse stopTimer(Long actorUserId, Long assignmentId, LaborSessionStopRequest request);

    LaborSessionResponse getActiveTimerByEmployee(Long employeeId);

    TaskWorkSummaryResponse completeTask(Long actorUserId, Long taskId, TaskCompletionRequest request);

    TaskWorkSummaryResponse getTaskWorkSummary(Long taskId);
}
