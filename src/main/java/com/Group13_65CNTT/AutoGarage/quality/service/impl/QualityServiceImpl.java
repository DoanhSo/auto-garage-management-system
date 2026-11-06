package com.Group13_65CNTT.AutoGarage.quality.service.impl;

import com.Group13_65CNTT.AutoGarage.common.security.ActorIds;
import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.security.CustomerScope;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService;
import com.Group13_65CNTT.AutoGarage.quality.dto.request.QualityCheckCreateRequest;
import com.Group13_65CNTT.AutoGarage.quality.dto.response.QualityCheckResponse;
import com.Group13_65CNTT.AutoGarage.quality.mapper.QualityCheckMapper;
import com.Group13_65CNTT.AutoGarage.quality.service.QualityService;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentStage;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.WorkResultStatus;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.repair.service.WorkLogRecorder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityServiceImpl implements QualityService {

    private final VehicleCheckRepository vehicleCheckRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final RepairTaskRepository repairTaskRepository;
    private final LaborSessionRepository laborSessionRepository;
    private final EmployeeRepository employeeRepository;
    private final QualityCheckMapper qualityCheckMapper;
    private final ObjectAccessService objectAccess;
    private final CustomerScope customerScope;
    private final com.Group13_65CNTT.AutoGarage.repair.repository.TaskAssignmentRepository assignmentRepository;
    private final WorkLogRecorder workLogRecorder;
    private final RepairOrderAssignmentRepository workLogRepository;
    private final CurrentUserProvider currentUserProvider;

    /**
     * A technician inspects as themselves and must be an active, QC-capable employee.
     * An admin records the result on behalf of the single QC inspector assigned to the order.
     */
    private Employee resolveInspector(long actorId, RepairOrder order) {
        if (currentUserProvider.role() != Role.ADMIN) {
            return employeeRepository.findByUserId(actorId)
                    .filter(employee -> Boolean.TRUE.equals(employee.getIsActive())
                            && Boolean.TRUE.equals(employee.getCanQc()))
                    .orElseThrow(() -> new BusinessException(ResponseCode.FORBIDDEN));
        }
        List<Long> assignedIds = assignmentRepository.findActiveQcForOrder(order.getId()).stream()
                .map(TaskAssignment::getEmployeeId)
                .distinct()
                .toList();
        if (assignedIds.isEmpty()) {
            return null;
        }
        if (assignedIds.size() > 1) {
            throw new ConflictException(ResponseCode.QC_INSPECTOR_INVALID,
                    "Phiếu có nhiều kỹ thuật viên QC đang được phân công, hãy giữ lại một người");
        }
        return employeeRepository.findById(assignedIds.get(0))
                .filter(employee -> Boolean.TRUE.equals(employee.getIsActive())
                        && Boolean.TRUE.equals(employee.getCanQc()))
                .orElseThrow(() -> new ConflictException(ResponseCode.QC_INSPECTOR_INVALID,
                        "Kỹ thuật viên QC được phân công không còn đủ điều kiện kiểm định"));
    }

    @Override
    @Transactional
    public QualityCheckResponse performQualityCheck(Long actorUserId, QualityCheckCreateRequest request) {
        log.info("Performing QC for order #{} by actor #{}", request.getRepairOrderId(), actorUserId);

        if (request.getResult() != CheckResult.PASSED && request.getResult() != CheckResult.FAILED) {
            throw new BusinessRuleException("Kết quả kiểm tra chất lượng (QC) chỉ có thể là PASSED hoặc FAILED");
        }
        long actorId = ActorIds.require(actorUserId);
        objectAccess.requireQualityCheckAccess(actorId, request.getRepairOrderId());

        RepairOrder order = repairOrderRepository.findByIdForUpdate(request.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + request.getRepairOrderId()));

        if (order.getStatus() != RepairOrderStatus.WAITING_QC) {
            throw new BusinessRuleException("Chỉ kiểm tra chất lượng khi phiếu đang chờ QC");
        }

        // 1. Kiểm tra tất cả công việc phải hoàn thành trước khi QC
        if (repairTaskRepository.existsByRepairOrderIdAndStatusNot(order.getId(), TaskStatus.COMPLETED)) {
            throw new BusinessRuleException("Vẫn còn công việc chưa hoàn thành, không thể tiến hành kiểm tra chất lượng (QC)");
        }

        // 2. Kiểm tra không còn timer nào đang mở
        if (laborSessionRepository.countOpenSessionsByRepairOrderId(order.getId()) > 0) {
            throw new BusinessRuleException("Vẫn còn phiên làm việc (timer) của kỹ thuật viên chưa kết thúc");
        }

        Employee inspector = resolveInspector(actorId, order);

        // Four-eyes principle: inspector must be on shift and must not be a main repairer of this order.
        Instant inspectionTime = Instant.now();
        if (inspector != null) {
            if (workLogRecorder.activeShift(inspector.getId(), inspectionTime).isEmpty()) {
                throw new BusinessRuleException("Kỹ thuật viên QC phải đang trong ca trực");
            }
            boolean repairedByInspector = workLogRepository
                    .findByRepairOrderIdAndAssignmentStage(order.getId(), AssignmentStage.REPAIR_MAIN).stream()
                    .anyMatch(row -> row.getEmployeeId().equals(inspector.getId()));
            if (repairedByInspector) {
                throw new BusinessRuleException("Thợ chính đã sửa chữa không được tự kiểm định QC cho chính phiếu này");
            }
        }

        int nextAttemptNo = vehicleCheckRepository.findMaxAttemptNo(order.getId(), CheckType.QC) + 1;

        List<RepairTask> requestedRework = request.getReworkTaskIds() == null ? List.of()
                : repairTaskRepository.findAllById(request.getReworkTaskIds());
        if (request.getReworkTaskIds() != null && (requestedRework.size() != request.getReworkTaskIds().size()
                || requestedRework.stream().anyMatch(task -> !task.getRepairOrderId().equals(order.getId())))) {
            throw new BusinessRuleException("Danh sách làm lại phải gồm công việc thuộc đúng phiếu sửa chữa");
        }

        VehicleCheck check = new VehicleCheck();
        check.setRepairOrderId(order.getId());
        check.setCheckType(CheckType.QC);
        check.setAttemptNo(nextAttemptNo);
        check.setResult(request.getResult());
        check.setChecklistJson(request.getChecklistJson() != null && !request.getChecklistJson().isBlank()
                ? request.getChecklistJson() : "{}");
        check.setSchemaVersion(1);
        check.setOdometer(request.getOdometer());
        check.setNotes(request.getNotes());
        check.setCheckedBy(ActorIds.require(actorUserId));
        check.setCheckedAt(Instant.now());

        VehicleCheck savedCheck = vehicleCheckRepository.save(check);

        // Cập nhật latest_qc_check_id cho phiếu sửa chữa
        order.setLatestQcCheckId(savedCheck.getId());
        if (inspector != null) {
            order.setQcTechnicianId(inspector.getId());

            Instant inspectionStart = order.getUpdatedAt() != null && order.getUpdatedAt().isBefore(inspectionTime)
                    ? order.getUpdatedAt() : inspectionTime;
            workLogRecorder.start(order, inspector.getId(), AssignmentStage.QC_INSPECTION, null,
                    inspectionStart, request.getNotes());
            workLogRecorder.finish(order.getId(), inspector.getId(), AssignmentStage.QC_INSPECTION, inspectionTime,
                    request.getResult() == CheckResult.PASSED ? WorkResultStatus.PASSED : WorkResultStatus.REWORK_FAILED,
                    request.getNotes());
        }

        if (request.getResult() == CheckResult.PASSED) {
            var qcAssignments = assignmentRepository.findActiveQcForOrder(order.getId());
            qcAssignments.forEach(assignment -> {
                assignment.setIsActive(false);
                assignment.setReleasedAt(check.getCheckedAt());
            });
            assignmentRepository.saveAll(qcAssignments);
            order.setStatus(RepairOrderStatus.COMPLETED);
            repairOrderRepository.save(order);
            log.info("QC PASSED cho phiếu #{}, attempt={}. Phiếu chuyển sang COMPLETED", order.getId(), nextAttemptNo);
        } else {
            order.setStatus(RepairOrderStatus.QC_FAILED);
            repairOrderRepository.save(order);
            log.info("QC FAILED cho phiếu #{}, attempt={}. Phiếu chuyển sang QC_FAILED", order.getId(), nextAttemptNo);

            // Chuyển các task cần làm lại sang REWORK
            if (request.getReworkTaskIds() != null && !request.getReworkTaskIds().isEmpty()) {
                List<RepairTask> reworkTasks = requestedRework;
                for (RepairTask rt : reworkTasks) {
                    if (rt.getRepairOrderId().equals(order.getId())) {
                        rt.setStatus(TaskStatus.REWORK);
                        rt.setCompletedAt(null);
                    }
                }
                repairTaskRepository.saveAll(reworkTasks);
                var reworkIds = reworkTasks.stream().map(RepairTask::getId).collect(java.util.stream.Collectors.toSet());
                var failedRows = workLogRepository.findByRepairOrderIdOrderByStartedAtAsc(order.getId()).stream()
                        .filter(row -> row.getTaskId() != null && reworkIds.contains(row.getTaskId()))
                        .toList();
                failedRows.forEach(row -> row.setResultStatus(WorkResultStatus.REWORK_FAILED));
                workLogRepository.saveAll(failedRows);
                log.info("Đã chuyển {} công việc sang trạng thái REWORK để xử lý lại", reworkTasks.size());
            }
        }

        return qualityCheckMapper.toResponse(savedCheck);
    }

    @Override
    @Transactional(readOnly = true)
    public QualityCheckResponse getQualityCheckById(Long id) {
        VehicleCheck check = vehicleCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biên bản kiểm tra với ID: " + id));
        customerScope.requireResource(AccessResource.REPAIR_ORDER, check.getRepairOrderId());
        return qualityCheckMapper.toResponse(check);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityCheckResponse> getQualityChecksByRepairOrderId(Long repairOrderId) {
        customerScope.requireResource(AccessResource.REPAIR_ORDER, repairOrderId);
        return vehicleCheckRepository.findAllByRepairOrderIdAndCheckTypeOrderByAttemptNoAsc(
                repairOrderId, CheckType.QC).stream()
                .map(qualityCheckMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QualityCheckResponse getLatestQualityCheck(Long repairOrderId) {
        customerScope.requireResource(AccessResource.REPAIR_ORDER, repairOrderId);
        RepairOrder order = repairOrderRepository.findById(repairOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + repairOrderId));

        if (order.getLatestQcCheckId() == null) {
            return null;
        }

        return getQualityCheckById(order.getLatestQcCheckId());
    }
}
