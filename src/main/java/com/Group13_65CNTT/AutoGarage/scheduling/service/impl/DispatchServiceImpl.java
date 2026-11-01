package com.Group13_65CNTT.AutoGarage.scheduling.service.impl;

import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.LaborSession;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.TaskAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse.Availability;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatchServiceImpl implements DispatchService {

    /** A technician handling this many orders at once is flagged as overloaded. */
    private static final int OVERLOAD_ORDER_COUNT = 3;

    private final EmployeeShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceBayRepository bayRepository;
    private final UserRepository userRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final RepairTaskRepository taskRepository;
    private final LaborSessionRepository laborSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OnShiftTechnicianResponse> getOnShiftTechnicians() {
        Map<Long, EmployeeShift> shiftByEmployee = new LinkedHashMap<>();
        for (EmployeeShift shift : shiftRepository.findAllActiveAt(Instant.now())) {
            shiftByEmployee.putIfAbsent(shift.getEmployeeId(), shift);
        }
        if (shiftByEmployee.isEmpty()) {
            return List.of();
        }

        Map<Long, Employee> employees = employeeRepository.findAllById(shiftByEmployee.keySet()).stream()
                .filter(employee -> Boolean.TRUE.equals(employee.getIsActive()))
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> names = userRepository
                .findAllById(employees.values().stream().map(Employee::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
        Map<Long, ServiceBay> bays = bayRepository
                .findAllById(shiftByEmployee.values().stream()
                        .map(EmployeeShift::getServiceBayId)
                        .filter(java.util.Objects::nonNull)
                        .toList())
                .stream()
                .collect(Collectors.toMap(ServiceBay::getId, Function.identity()));

        List<OnShiftTechnicianResponse> result = new ArrayList<>();
        for (Employee employee : employees.values()) {
            EmployeeShift shift = shiftByEmployee.get(employee.getId());
            ServiceBay bay = shift.getServiceBayId() == null ? null : bays.get(shift.getServiceBayId());

            List<TaskAssignment> active = assignmentRepository.findByEmployeeIdAndIsActiveTrue(employee.getId());
            long orders = taskRepository.findAllById(active.stream().map(TaskAssignment::getTaskId).toList()).stream()
                    .map(RepairTask::getRepairOrderId)
                    .distinct()
                    .count();
            LaborSession timer = laborSessionRepository.findTopByEmployeeIdAndEndsAtIsNull(employee.getId()).orElse(null);
            Long activeTaskId = timer == null ? null : active.stream()
                    .filter(assignment -> assignment.getId().equals(timer.getAssignmentId()))
                    .map(TaskAssignment::getTaskId)
                    .findFirst()
                    .orElse(null);

            Availability availability = orders >= OVERLOAD_ORDER_COUNT
                    ? Availability.OVERLOADED
                    : timer != null ? Availability.BUSY : Availability.AVAILABLE;

            result.add(new OnShiftTechnicianResponse(
                    employee.getId(),
                    employee.getEmployeeCode(),
                    names.getOrDefault(employee.getUserId(), employee.getEmployeeCode()),
                    shift.getId(),
                    shift.getShiftType(),
                    shift.getStartsAt(),
                    shift.getEndsAt(),
                    bay == null ? null : bay.getId(),
                    bay == null ? null : bay.getBayCode(),
                    bay == null ? null : bay.getBayName(),
                    parseSkills(employee.getSkillsJson()),
                    Boolean.TRUE.equals(employee.getCanQc()),
                    (int) orders,
                    timer != null,
                    activeTaskId,
                    availability));
        }
        return result;
    }

    private List<String> parseSkills(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            var node = tools.jackson.databind.json.JsonMapper.builder().build().readTree(json);
            List<String> skills = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(item -> {
                    if (item.isString()) {
                        skills.add(item.asText());
                    }
                });
            }
            return skills;
        } catch (tools.jackson.core.JacksonException failure) {
            return List.of();
        }
    }
}
