package com.Group13_65CNTT.AutoGarage.repair.service.impl;

import com.Group13_65CNTT.AutoGarage.repair.dto.request.LaborSessionStopRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskAssignmentRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskCompletionRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.LaborSessionResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskAssignmentResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskWorkSummaryResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.*;
import com.Group13_65CNTT.AutoGarage.repair.mapper.LaborSessionMapper;
import com.Group13_65CNTT.AutoGarage.repair.mapper.TaskAssignmentMapper;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.TaskAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.repair.service.LaborExecutionService;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LaborExecutionServiceImpl implements LaborExecutionService {

    private final RepairTaskRepository repairTaskRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final LaborSessionRepository laborSessionRepository;
    private final EmployeeRepository employeeRepository;
    private final com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository shiftRepository;
    private final com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository serviceRepository;
    private final com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService objectAccess;

    private final TaskAssignmentMapper taskAssignmentMapper;
    private final LaborSessionMapper laborSessionMapper;

    // =========================================================================
    // TV3-06 Phân công KTV
    // =========================================================================

    @Override
    @Transactional
    public TaskAssignmentResponse assignTask(Long actorUserId, Long taskId, TaskAssignmentRequest request) {
        log.info("Assigning task #{} to employee #{}, role={}", taskId, request.getEmployeeId(), request.getAssignmentRole());

        if (request.getEndsAt().isBefore(request.getStartsAt()) || request.getEndsAt().equals(request.getStartsAt())) {
            throw new BusinessRuleException("Thời gian kết thúc phân công phải sau thời gian bắt đầu");
        }

        RepairTask task = repairTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        if ((task.getStatus() == TaskStatus.COMPLETED && request.getAssignmentRole() != AssignmentRole.QC) || task.getStatus() == TaskStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể phân công công việc đã hoàn thành hoặc đã bị hủy");
        }

        RepairOrder order = repairOrderRepository.findById(task.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa của công việc này"));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED || order.getStatus() == RepairOrderStatus.INVOICED) {
            throw new BusinessRuleException("Không thể phân công công việc của phiếu sửa chữa đã kết thúc");
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + request.getEmployeeId()));

        if (Boolean.FALSE.equals(employee.getIsActive())) {
            throw new BusinessRuleException("Nhân viên đang ngưng hoạt động");
        }

        // Quy tắc: Nếu phân công vai trò QC thì nhân viên phải can_qc
        if (request.getAssignmentRole() == AssignmentRole.QC && Boolean.FALSE.equals(employee.getCanQc())) {
            throw new BusinessRuleException("Nhân viên không có thẩm quyền kiểm tra chất lượng (can_qc = false)");
        }

        // Quy tắc: Mỗi công việc tối đa một thợ chính (MAIN) active
        if (request.getAssignmentRole() == AssignmentRole.MAIN) {
            taskAssignmentRepository.findByTaskIdAndAssignmentRoleAndIsActiveTrue(taskId, AssignmentRole.MAIN)
                    .ifPresent(existingMain -> {
                        throw new ConflictException("Công việc này đã có thợ chính (MAIN) đang phụ trách. Hãy giải phóng thợ chính trước khi phân công người mới.");
                    });
        }

        // Quy tắc: Chống chồng ca/lịch làm của cùng kỹ thuật viên
        List<TaskAssignment> overlapping = taskAssignmentRepository.findOverlappingAssignments(
                request.getEmployeeId(), request.getStartsAt(), request.getEndsAt());
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Kỹ thuật viên đã có phân công công việc khác trong khoảng thời gian này");
        }

        if (!shiftRepository.coversAssignment(employee.getId(), request.getStartsAt(), request.getEndsAt())) {
            throw new BusinessRuleException("Thời gian phân công phải nằm trong ca làm việc đang hoạt động của nhân viên");
        }
        if (task.getServiceId() != null) {
            var service = serviceRepository.findById(task.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ của công việc"));
            if (!skills(employee.getSkillsJson()).containsAll(skills(service.getRequiredSkillsJson()))) {
                throw new BusinessRuleException("Nhân viên chưa có đủ kỹ năng yêu cầu của dịch vụ");
            }
        }

        TaskAssignment assignment = taskAssignmentMapper.toEntity(request);
        assignment.setTaskId(taskId);
        assignment.setIsActive(true);
        assignment.setAssignedBy(actorUserId);
        assignment.setAssignedAt(Instant.now());

        TaskAssignment saved = taskAssignmentRepository.save(assignment);

        // Cập nhật trạng thái task nếu đang PENDING
        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.ASSIGNED);
            repairTaskRepository.save(task);
        }

        // Cập nhật trạng thái phiếu sửa chữa nếu đang APPROVED
        if (order.getStatus() == RepairOrderStatus.APPROVED) {
            order.setStatus(RepairOrderStatus.IN_PROGRESS);
            repairOrderRepository.save(order);
        }

        TaskAssignmentResponse response = taskAssignmentMapper.toResponse(saved);
        response.setEmployeeName(employee.getEmployeeCode());
        return response;
    }

    @Override
    @Transactional
    public void releaseAssignment(Long actorUserId, Long assignmentId) {
        log.info("Releasing assignment #{} by actor #{}", assignmentId, actorUserId);

        TaskAssignment assignment = taskAssignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công với ID: " + assignmentId));

        if (Boolean.FALSE.equals(assignment.getIsActive())) {
            return;
        }

        // Đóng timer đang mở nếu có
        laborSessionRepository.findTopByAssignmentIdAndEndsAtIsNull(assignmentId).ifPresent(s -> {
            s.setEndsAt(Instant.now());
            s.setStopReason(StopReason.CANCEL);
            laborSessionRepository.save(s);
        });

        assignment.setIsActive(false);
        assignment.setReleasedAt(Instant.now());
        taskAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignmentResponse> getAssignmentsByTaskId(Long taskId) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(taskId);
        List<TaskAssignmentResponse> responses = new ArrayList<>();
        for (TaskAssignment a : assignments) {
            TaskAssignmentResponse res = taskAssignmentMapper.toResponse(a);
            employeeRepository.findById(a.getEmployeeId()).ifPresent(e -> res.setEmployeeName(e.getEmployeeCode()));
            responses.add(res);
        }
        return responses;
    }

    // =========================================================================
    // TV3-07 Timer KTV
    // =========================================================================

    @Override
    @Transactional
    public LaborSessionResponse startTimer(Long actorUserId, Long assignmentId) {
        objectAccess.requireAssignmentExecutionAccess(actorUserId, assignmentId);
        log.info("Starting timer for assignment #{} by actor #{}", assignmentId, actorUserId);

        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công với ID: " + assignmentId));

        if (Boolean.FALSE.equals(assignment.getIsActive())) {
            throw new BusinessRuleException("Phân công này đã kết thúc hoặc bị giải phóng");
        }

        RepairTask task = repairTaskRepository.findByIdForUpdate(assignment.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc"));
        requireOpenOrder(task.getRepairOrderId());
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED
                || assignment.getAssignmentRole() == AssignmentRole.QC) {
            throw new BusinessRuleException("Không thể bấm giờ thi công cho công việc đã kết thúc hoặc phân công QC");
        }

        Employee employee = employeeRepository.findById(assignment.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỹ thuật viên"));

        // Kiểm tra xem thợ có timer đang mở không (đảm bảo chỉ 1 timer mở mỗi thợ)
        laborSessionRepository.findTopByEmployeeIdAndEndsAtIsNull(employee.getId())
                .ifPresent(existing -> {
                    throw new ConflictException("Kỹ thuật viên đang có một phiên bấm giờ chưa kết thúc");
                });

        BigDecimal hourlyRate = employee.getHourlyCost() != null ? employee.getHourlyCost() : BigDecimal.ZERO;

        LaborSession session = LaborSession.builder()
                .assignmentId(assignmentId)
                .employeeId(employee.getId())
                .startsAt(Instant.now())
                .hourlyCostSnapshot(hourlyRate)
                .build();

        LaborSession savedSession = laborSessionRepository.save(session);

        // Cập nhật trạng thái task sang IN_PROGRESS
        if (task.getStartedAt() == null) {
            task.setStartedAt(Instant.now());
        }
        if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.ASSIGNED || task.getStatus() == TaskStatus.PAUSED || task.getStatus() == TaskStatus.REWORK) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }
        repairTaskRepository.save(task);

        return laborSessionMapper.toResponse(savedSession);
    }

    @Override
    @Transactional
    public LaborSessionResponse stopTimer(Long actorUserId, Long assignmentId, LaborSessionStopRequest request) {
        objectAccess.requireAssignmentExecutionAccess(actorUserId, assignmentId);
        log.info("Stopping timer for assignment #{}, reason={}", assignmentId, request.getStopReason());

        LaborSession session = laborSessionRepository.findTopByAssignmentIdAndEndsAtIsNull(assignmentId)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy phiên bấm giờ đang mở cho phân công này"));

        session.setEndsAt(Instant.now());
        session.setStopReason(request.getStopReason());
        LaborSession saved = laborSessionRepository.save(session);

        // Nếu dừng do PAUSE thì cập nhật task sang PAUSED
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId).orElseThrow();
        RepairTask task = repairTaskRepository.findByIdForUpdate(assignment.getTaskId()).orElseThrow();
        if (request.getStopReason() == StopReason.PAUSE && task.getStatus() == TaskStatus.IN_PROGRESS) {
            task.setStatus(TaskStatus.PAUSED);
            repairTaskRepository.save(task);
        }

        return laborSessionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LaborSessionResponse getActiveTimerByEmployee(Long employeeId) {
        return laborSessionRepository.findTopByEmployeeIdAndEndsAtIsNull(employeeId)
                .map(laborSessionMapper::toResponse)
                .orElse(null);
    }

    // =========================================================================
    // TV3-08 Hoàn thành công việc
    // =========================================================================

    @Override
    @Transactional
    public TaskWorkSummaryResponse completeTask(Long actorUserId, Long taskId, TaskCompletionRequest request) {
        objectAccess.requireTaskExecutionAccess(actorUserId, taskId);
        log.info("Completing task #{} by actor #{}", taskId, actorUserId);

        RepairTask task = repairTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        if (task.getStatus() == TaskStatus.COMPLETED) {
            return getTaskWorkSummary(taskId);
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể hoàn thành công việc đã bị hủy");
        }

        requireOpenOrder(task.getRepairOrderId());

        Instant now = Instant.now();

        // 1. Đóng tất cả các timer đang mở của task này với stopReason = COMPLETE
        List<LaborSession> openSessions = laborSessionRepository.findOpenSessionsByTaskId(taskId);
        for (LaborSession s : openSessions) {
            s.setEndsAt(now);
            s.setStopReason(StopReason.COMPLETE);
        }
        laborSessionRepository.saveAll(openSessions);

        // 2. Giải phóng các phân công active của task
        List<TaskAssignment> activeAssignments = taskAssignmentRepository.findByTaskIdAndIsActiveTrue(taskId);
        for (TaskAssignment a : activeAssignments) {
            // QC assignment remains active until inspection; releasing it here blocks QC authorization.
            if (a.getAssignmentRole() != AssignmentRole.QC) {
                a.setIsActive(false);
                a.setReleasedAt(now);
            }
        }
        taskAssignmentRepository.saveAll(activeAssignments);

        // 3. Cập nhật task sang COMPLETED
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(now);
        if (request != null && request.getCompletionNote() != null && !request.getCompletionNote().isBlank()) {
            task.setCompletionNote(request.getCompletionNote());
        }
        repairTaskRepository.save(task);

        // 4. Nếu tất cả tasks của phiếu đều COMPLETED $\rightarrow$ chuyển phiếu sang WAITING_QC
        boolean hasUnfinishedTasks = repairTaskRepository.existsByRepairOrderIdAndStatusNot(
                task.getRepairOrderId(), TaskStatus.COMPLETED);
        if (!hasUnfinishedTasks) {
            RepairOrder order = repairOrderRepository.findByIdForUpdate(task.getRepairOrderId()).orElseThrow();
            if (order.getStatus() == RepairOrderStatus.APPROVED || order.getStatus() == RepairOrderStatus.IN_PROGRESS || order.getStatus() == RepairOrderStatus.WAITING_PARTS || order.getStatus() == RepairOrderStatus.QC_FAILED) {
                order.setStatus(RepairOrderStatus.WAITING_QC);
                repairOrderRepository.save(order);
                log.info("Tất cả công việc đã hoàn thành. Phiếu sửa chữa #{} chuyển sang trạng thái WAITING_QC", order.getId());
            }
        }

        return getTaskWorkSummary(taskId);
    }

    private void requireOpenOrder(Long orderId) {
        RepairOrder order = repairOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa"));
        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED
                || order.getStatus() == RepairOrderStatus.INVOICED) {
            throw new BusinessRuleException("Phiếu đã kết thúc hoặc xuất hóa đơn, không thể tiếp tục thi công");
        }
    }

    private java.util.Set<String> skills(String value) {
        if (value == null || value.isBlank()) return java.util.Set.of();
        try {
            var node = tools.jackson.databind.json.JsonMapper.builder().build().readTree(value);
            if (!node.isArray()) throw new BusinessRuleException("Danh sách kỹ năng phải là mảng JSON");
            java.util.Set<String> result = new java.util.HashSet<>();
            for (var skill : node) {
                if (!skill.isString() || skill.asText().isBlank()) throw new BusinessRuleException("Mã kỹ năng không hợp lệ");
                result.add(skill.asText());
            }
            return result;
        } catch (tools.jackson.core.JacksonException failure) {
            throw new com.Group13_65CNTT.AutoGarage.common.exception.BusinessException(
                    com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.BAD_REQUEST, failure);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskWorkSummaryResponse getTaskWorkSummary(Long taskId) {
        RepairTask task = repairTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(taskId);
        List<TaskAssignmentResponse> assignmentResponses = new ArrayList<>();
        List<Long> assignmentIds = new ArrayList<>();

        for (TaskAssignment a : assignments) {
            TaskAssignmentResponse res = taskAssignmentMapper.toResponse(a);
            employeeRepository.findById(a.getEmployeeId()).ifPresent(e -> res.setEmployeeName(e.getEmployeeCode()));
            assignmentResponses.add(res);
            assignmentIds.add(a.getId());
        }

        List<LaborSession> sessions = assignmentIds.isEmpty() ? List.of() :
                laborSessionRepository.findByAssignmentIdIn(assignmentIds);

        long totalSeconds = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<LaborSessionResponse> sessionResponses = new ArrayList<>();

        for (LaborSession s : sessions) {
            LaborSessionResponse res = laborSessionMapper.toResponse(s);
            sessionResponses.add(res);

            if (s.getStartsAt() != null && s.getEndsAt() != null) {
                long duration = Duration.between(s.getStartsAt(), s.getEndsAt()).getSeconds();
                totalSeconds += duration;
                BigDecimal hours = BigDecimal.valueOf(duration).divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
                totalCost = totalCost.add(hours.multiply(s.getHourlyCostSnapshot()).setScale(2, RoundingMode.HALF_UP));
            }
        }

        return TaskWorkSummaryResponse.builder()
                .taskId(task.getId())
                .repairOrderId(task.getRepairOrderId())
                .taskType(task.getTaskType())
                .taskName(task.getTaskName())
                .status(task.getStatus())
                .quantity(task.getQuantity())
                .unitPrice(task.getUnitPrice())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .completionNote(task.getCompletionNote())
                .totalActualSeconds(totalSeconds)
                .totalLaborCost(totalCost)
                .assignments(assignmentResponses)
                .sessions(sessionResponses)
                .build();
    }
}
