package com.Group13_65CNTT.AutoGarage.scheduling.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.LaborSession;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.constant.ShiftPolicy;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftReconciliationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.AttendanceStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.EmployeeShiftMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.ShiftAttendanceService;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftAttendanceServiceImpl implements ShiftAttendanceService {

    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);

    private final EmployeeShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LaborSessionRepository laborSessionRepository;
    private final com.Group13_65CNTT.AutoGarage.repair.service.LaborExecutionService laborExecutionService;
    private final EmployeeShiftMapper shiftMapper;
    private final CurrentUserProvider currentUser;

    @Override
    @Transactional
    @Audited(action = "SHIFT_CHECK_IN", entity = "EmployeeShift")
    public ShiftResponse checkIn(Long actorUserId, Long shiftId) {
        EmployeeShift shift = loadActiveShift(shiftId);
        requireOwnerOrFrontDesk(actorUserId, shift);
        if (shift.getAttendanceStatus() != AttendanceStatus.SCHEDULED
                && shift.getAttendanceStatus() != AttendanceStatus.ABSENT) {
            throw new BusinessRuleException("Ca trực này đã được chấm công vào ca");
        }
        Instant now = Instant.now();
        if (now.isBefore(shift.getStartsAt().minus(ShiftPolicy.EARLY_CHECK_IN_WINDOW))) {
            throw new BusinessRuleException("Chưa đến giờ vào ca, chỉ được chấm công trước giờ ca tối đa "
                    + ShiftPolicy.EARLY_CHECK_IN_WINDOW.toMinutes() + " phút");
        }
        if (!now.isBefore(shift.getEndsAt())) {
            throw new BusinessRuleException("Ca trực đã kết thúc, không thể chấm công vào ca");
        }
        shift.setActualStartsAt(now);
        shift.setAttendanceStatus(now.isAfter(shift.getStartsAt().plus(ShiftPolicy.LATE_GRACE))
                ? AttendanceStatus.LATE
                : AttendanceStatus.CHECKED_IN);
        return shiftMapper.toResponse(shiftRepository.save(shift));
    }

    @Override
    @Transactional
    @Audited(action = "SHIFT_CHECK_OUT", entity = "EmployeeShift")
    public ShiftResponse checkOut(Long actorUserId, Long shiftId) {
        EmployeeShift shift = loadActiveShift(shiftId);
        requireOwnerOrFrontDesk(actorUserId, shift);
        if (shift.getAttendanceStatus() != AttendanceStatus.CHECKED_IN
                && shift.getAttendanceStatus() != AttendanceStatus.LATE) {
            throw new BusinessRuleException("Cần chấm công vào ca trước khi chấm công ra ca");
        }
        Instant now = Instant.now();
        laborExecutionService.pauseOpenTimersForEmployee(shift.getEmployeeId(), now);
        shift.setActualEndsAt(now);
        shift.setAttendanceStatus(AttendanceStatus.CHECKED_OUT);
        return shiftMapper.toResponse(shiftRepository.save(shift));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftReconciliationResponse> reconcile(LocalDate date) {
        Instant from = date.atStartOfDay(ShiftPolicy.ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ShiftPolicy.ZONE).toInstant();
        List<EmployeeShift> shifts = shiftRepository.findActiveStartingBetween(from, to);
        if (shifts.isEmpty()) {
            return List.of();
        }
        Map<Long, Employee> employees = employeeRepository
                .findAllById(shifts.stream().map(EmployeeShift::getEmployeeId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> names = userRepository
                .findAllById(employees.values().stream().map(Employee::getUserId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
        Instant now = Instant.now();

        return shifts.stream().map(shift -> {
            Employee employee = employees.get(shift.getEmployeeId());
            List<LaborSession> sessions = laborSessionRepository.findOverlapping(
                    shift.getEmployeeId(), shift.getStartsAt(), shift.getEndsAt().plus(Duration.ofHours(12)));
            long inShift = 0;
            long afterShift = 0;
            for (LaborSession session : sessions) {
                Instant end = session.getEndsAt() != null ? session.getEndsAt() : now;
                inShift += overlapSeconds(session.getStartsAt(), end, shift.getStartsAt(), shift.getEndsAt());
                afterShift += overlapSeconds(session.getStartsAt(), end, shift.getEndsAt(), shift.getEndsAt().plus(Duration.ofHours(12)));
            }
            long scheduled = Duration.between(shift.getStartsAt(), shift.getEndsAt()).getSeconds();
            long idle = Math.max(0, scheduled - inShift);
            int utilization = scheduled == 0 ? 0 : (int) Math.min(100, Math.round(inShift * 100.0 / scheduled));
            return new ShiftReconciliationResponse(
                    shift.getId(),
                    shift.getEmployeeId(),
                    employee == null ? null : employee.getEmployeeCode(),
                    employee == null ? null : names.get(employee.getUserId()),
                    shift.getShiftType(),
                    shift.getStartsAt(),
                    shift.getEndsAt(),
                    shift.getAttendanceStatus(),
                    shift.getActualStartsAt(),
                    shift.getActualEndsAt(),
                    hours(scheduled),
                    hours(inShift),
                    hours(idle),
                    hours(afterShift),
                    utilization);
        }).toList();
    }

    private EmployeeShift loadActiveShift(Long shiftId) {
        EmployeeShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca trực với ID: " + shiftId));
        if (shift.getStatus() != ShiftStatus.ACTIVE) {
            throw new BusinessRuleException("Ca trực đã bị hủy");
        }
        return shift;
    }

    private void requireOwnerOrFrontDesk(Long actorUserId, EmployeeShift shift) {
        Set<Role> frontDesk = EnumSet.of(Role.ADMIN, Role.RECEPTIONIST);
        if (frontDesk.contains(currentUser.role())) {
            return;
        }
        boolean owner = employeeRepository.findById(shift.getEmployeeId())
                .map(employee -> employee.getUserId().equals(actorUserId))
                .orElse(false);
        if (!owner) {
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }
    }

    private long overlapSeconds(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        Instant start = aStart.isAfter(bStart) ? aStart : bStart;
        Instant end = aEnd.isBefore(bEnd) ? aEnd : bEnd;
        return end.isAfter(start) ? Duration.between(start, end).getSeconds() : 0;
    }

    private BigDecimal hours(long seconds) {
        return BigDecimal.valueOf(seconds).divide(SECONDS_PER_HOUR, 2, RoundingMode.HALF_UP);
    }
}
