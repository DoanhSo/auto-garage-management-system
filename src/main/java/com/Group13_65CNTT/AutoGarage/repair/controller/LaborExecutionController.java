package com.Group13_65CNTT.AutoGarage.repair.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.LaborSessionStopRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskAssignmentRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskCompletionRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.LaborSessionResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskAssignmentResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskWorkSummaryResponse;
import com.Group13_65CNTT.AutoGarage.repair.service.LaborExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.REPAIR_TASKS_BASE)
@RequiredArgsConstructor
@Tag(name = "Labor Execution", description = "Phân công kỹ thuật viên, bấm giờ timer và hoàn thành công việc (TV3)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class LaborExecutionController {

    private final LaborExecutionService laborExecutionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{taskId}/assignments")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Phân công kỹ thuật viên cho công việc (MAIN, ASSISTANT, QC)")
    public ResponseEntity<ApiResponse<TaskAssignmentResponse>> assignTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssignmentRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        TaskAssignmentResponse response = laborExecutionService.assignTask(actorId, taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Giải phóng phân công kỹ thuật viên")
    public ResponseEntity<ApiResponse<Void>> releaseAssignment(
            @PathVariable Long assignmentId,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        laborExecutionService.releaseAssignment(actorId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, null, httpRequest.getRequestURI()));
    }

    @GetMapping("/{taskId}/assignments")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy danh sách phân công của công việc")
    public ResponseEntity<ApiResponse<List<TaskAssignmentResponse>>> getAssignmentsByTaskId(
            @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        List<TaskAssignmentResponse> response = laborExecutionService.getAssignmentsByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/assignments/{assignmentId}/timer/start")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Bắt đầu bấm giờ làm việc (Timer) cho kỹ thuật viên")
    public ResponseEntity<ApiResponse<LaborSessionResponse>> startTimer(
            @PathVariable Long assignmentId,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        LaborSessionResponse response = laborExecutionService.startTimer(actorId, assignmentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/assignments/{assignmentId}/timer/stop")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Tạm dừng hoặc dừng bấm giờ làm việc (PAUSE, COMPLETE, CANCEL)")
    public ResponseEntity<ApiResponse<LaborSessionResponse>> stopTimer(
            @PathVariable Long assignmentId,
            @Valid @RequestBody LaborSessionStopRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        LaborSessionResponse response = laborExecutionService.stopTimer(actorId, assignmentId, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/technicians/{employeeId}/active-timer")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Xem phiên bấm giờ đang mở của kỹ thuật viên")
    public ResponseEntity<ApiResponse<LaborSessionResponse>> getActiveTimer(
            @PathVariable Long employeeId,
            HttpServletRequest httpRequest) {
        LaborSessionResponse response = laborExecutionService.getActiveTimerByEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{taskId}/complete")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Xác nhận hoàn thành công việc, đóng timer và tính tổng giờ công thực")
    public ResponseEntity<ApiResponse<TaskWorkSummaryResponse>> completeTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) TaskCompletionRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        TaskWorkSummaryResponse response = laborExecutionService.completeTask(actorId, taskId, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{taskId}/summary")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy tổng hợp chi tiết thi công và giờ công thực tế của công việc")
    public ResponseEntity<ApiResponse<TaskWorkSummaryResponse>> getTaskWorkSummary(
            @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        TaskWorkSummaryResponse response = laborExecutionService.getTaskWorkSummary(taskId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }
}
