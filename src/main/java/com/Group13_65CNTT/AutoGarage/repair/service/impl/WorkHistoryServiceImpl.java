package com.Group13_65CNTT.AutoGarage.repair.service.impl;

import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.EmployeeWorkSummaryResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.VehicleWorkHistoryResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.WorkResultStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentStage;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderAssignment;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.service.WorkHistoryService;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkHistoryServiceImpl implements WorkHistoryService {

    private static final Map<AssignmentStage, String> STAGE_LABELS = Map.of(
            AssignmentStage.DIAGNOSIS, "Chẩn đoán hiện trạng tiếp nhận",
            AssignmentStage.REPAIR_MAIN, "Sửa chữa",
            AssignmentStage.REPAIR_ASSISTANT, "Hỗ trợ sửa chữa",
            AssignmentStage.QC_INSPECTION, "Kiểm tra chất lượng hoàn tất");

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RepairOrderAssignmentRepository assignmentRepository;
    private final RepairOrderRepository orderRepository;
    private final RepairTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VehicleWorkHistoryResponse> getVehicleWorkHistory(Long vehicleId) {
        return toResponses(assignmentRepository.findByVehicleIdOrderByStartedAtDesc(vehicleId));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeWorkSummaryResponse getEmployeeWorkSummary(Long employeeId, String month) {
        YearMonth period;
        try {
            period = YearMonth.parse(month);
        } catch (DateTimeParseException failure) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, failure);
        }
        Instant from = period.atDay(1).atStartOfDay(ZONE).toInstant();
        Instant to = period.plusMonths(1).atDay(1).atStartOfDay(ZONE).toInstant();
        List<VehicleWorkHistoryResponse> entries = toResponses(assignmentRepository
                .findByEmployeeIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        employeeId, from, to));
        return new EmployeeWorkSummaryResponse(
                employeeId,
                month,
                entries.stream().mapToLong(VehicleWorkHistoryResponse::actualMinutes).sum(),
                (int) entries.stream().map(VehicleWorkHistoryResponse::repairOrderId).distinct().count(),
                (int) entries.stream()
                        .filter(entry -> entry.resultStatus() == WorkResultStatus.PASSED
                                || entry.resultStatus() == WorkResultStatus.COMPLETED)
                        .map(VehicleWorkHistoryResponse::repairOrderId)
                        .distinct()
                        .count(),
                entries);
    }

    private List<VehicleWorkHistoryResponse> toResponses(List<RepairOrderAssignment> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, String> plates = vehicleRepository
                .findAllById(rows.stream().map(RepairOrderAssignment::getVehicleId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getLicensePlate));
        Map<Long, RepairOrder> orders = orderRepository
                .findAllById(rows.stream().map(RepairOrderAssignment::getRepairOrderId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(RepairOrder::getId, Function.identity()));
        Map<Long, RepairTask> tasks = taskRepository
                .findAllById(rows.stream().map(RepairOrderAssignment::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(RepairTask::getId, Function.identity()));
        Map<Long, Employee> employees = employeeRepository
                .findAllById(rows.stream().map(RepairOrderAssignment::getEmployeeId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> names = userRepository
                .findAllById(employees.values().stream().map(Employee::getUserId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
        Set<Long> shiftIds = rows.stream().map(RepairOrderAssignment::getShiftId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, EmployeeShift> shifts = shiftRepository.findAllById(shiftIds).stream()
                .collect(Collectors.toMap(EmployeeShift::getId, Function.identity()));

        return rows.stream().map(row -> {
            Employee employee = employees.get(row.getEmployeeId());
            RepairOrder order = orders.get(row.getRepairOrderId());
            RepairTask task = row.getTaskId() == null ? null : tasks.get(row.getTaskId());
            EmployeeShift shift = row.getShiftId() == null ? null : shifts.get(row.getShiftId());
            return new VehicleWorkHistoryResponse(
                    row.getId(),
                    row.getRepairOrderId(),
                    order == null ? null : order.getOrderCode(),
                    plates.get(row.getVehicleId()),
                    row.getAssignmentStage(),
                    task != null ? task.getTaskName() : STAGE_LABELS.get(row.getAssignmentStage()),
                    row.getEmployeeId(),
                    employee == null ? null : names.getOrDefault(employee.getUserId(), employee.getEmployeeCode()),
                    shift == null ? null : shift.getShiftType(),
                    row.getStartedAt(),
                    row.getCompletedAt(),
                    row.getActualSeconds() / 60,
                    row.getResultStatus(),
                    row.getNotes());
        }).toList();
    }
}
