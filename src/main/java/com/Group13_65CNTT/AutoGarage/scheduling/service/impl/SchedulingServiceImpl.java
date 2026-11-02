package com.Group13_65CNTT.AutoGarage.scheduling.service.impl;

import com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.security.ActorIds;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.MaintenanceReservationRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftRegisterRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayReservationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.EmployeeResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ServiceBayResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.BayReservation;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationType;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;
import com.Group13_65CNTT.AutoGarage.scheduling.constant.ShiftPolicy;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.BayReservationMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.EmployeeMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.EmployeeShiftMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.ServiceBayMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.BayReservationRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.SchedulingService;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BayScheduling;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulingServiceImpl implements SchedulingService, BayScheduling {
    private final WorkshopProperties workshop;

    private final EmployeeRepository employeeRepository;
    private final EmployeeShiftRepository employeeShiftRepository;
    private final ServiceBayRepository serviceBayRepository;
    private final BayReservationRepository bayReservationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUser;

    private final EmployeeMapper employeeMapper;
    private final EmployeeShiftMapper employeeShiftMapper;
    private final ServiceBayMapper serviceBayMapper;
    private final BayReservationMapper bayReservationMapper;

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_CREATE_EMPLOYEE, entity = AuditConstants.ENTITY_EMPLOYEE)
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (!userRepository.existsById(request.getUserId())) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
        }
        if (employeeRepository.existsByUserId(request.getUserId())) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode().trim())) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }

        Employee employee = employeeMapper.toEntity(request);
        employee.setEmployeeCode(request.getEmployeeCode().trim());
        Employee saved = employeeRepository.save(employee);
        return toEmployeeResponse(saved, loadAccounts(List.of(saved)));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return toEmployeeResponse(employee, loadAccounts(List.of(employee)));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByUserId(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return toEmployeeResponse(employee, loadAccounts(List.of(employee)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployees(Boolean isActive, Pageable pageable) {
        Page<Employee> page = (isActive == null)
                ? employeeRepository.findAll(pageable)
                : employeeRepository.findByIsActive(isActive, pageable);
        Map<Long, User> accounts = loadAccounts(page.getContent());
        return page.map(employee -> toEmployeeResponse(employee, accounts));
    }

    private Map<Long, User> loadAccounts(List<Employee> employees) {
        return userRepository.findAllById(employees.stream().map(Employee::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /** Adds the account's display name; pay and bank details stay with the front desk only. */
    private EmployeeResponse toEmployeeResponse(Employee employee, Map<Long, User> accounts) {
        EmployeeResponse response = employeeMapper.toResponse(employee);
        User account = accounts.get(employee.getUserId());
        if (account != null) {
            response.setFullName(account.getFullName());
            response.setUsername(account.getUsername());
            response.setEmail(account.getEmail());
        }
        Role role = currentUser.role();
        if (role != Role.ADMIN && role != Role.RECEPTIONIST) {
            response.setBankName(null);
            response.setBankAccountNumber(null);
            response.setBankAccountHolder(null);
            response.setBaseSalary(null);
            response.setSalaryCoefficient(null);
            response.setHourlyCost(null);
            response.setOvertimeMultiplier(null);
            response.setDefaultCommissionRate(null);
        }
        return response;
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_UPDATE_EMPLOYEE, entity = AuditConstants.ENTITY_EMPLOYEE)
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        employeeMapper.updateEntityFromRequest(request, employee);
        Employee updated = employeeRepository.save(employee);
        return toEmployeeResponse(updated, loadAccounts(List.of(updated)));
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_DEACTIVATE_EMPLOYEE, entity = AuditConstants.ENTITY_EMPLOYEE, idArgument = 0)
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        employee.setIsActive(false);
        employeeRepository.save(employee);
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_CREATE_SHIFT, entity = AuditConstants.ENTITY_EMPLOYEE_SHIFT)
    public ShiftResponse createShift(Long actorUserId, ShiftCreateRequest request) {
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (!Boolean.TRUE.equals(employee.getIsActive())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        if (request.getServiceBayId() != null && !serviceBayRepository.existsById(request.getServiceBayId())) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
        }

        List<EmployeeShift> overlaps = employeeShiftRepository.findOverlappingShifts(
                request.getEmployeeId(), ShiftStatus.ACTIVE, request.getStartsAt(), request.getEndsAt());
        if (!overlaps.isEmpty()) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }

        validateWorkingLimits(request.getEmployeeId(), request.getStartsAt(), request.getEndsAt());
        validateBayCapacity(request);

        EmployeeShift shift = employeeShiftMapper.toEntity(request);
        if (shift.getShiftType() == null) {
            shift.setShiftType(ShiftType.FULL_DAY);
        }
        shift.setStatus(ShiftStatus.ACTIVE);
        shift.setCreatedBy(ActorIds.require(actorUserId));
        EmployeeShift saved = employeeShiftRepository.save(shift);
        return employeeShiftMapper.toResponse(saved);
    }

    /** A bay hosts at most one main and one assistant technician in the same period. */
    private void validateBayCapacity(ShiftCreateRequest request) {
        if (request.getServiceBayId() == null) {
            return;
        }
        List<EmployeeShift> sameBay = employeeShiftRepository.findOverlappingByBay(
                request.getServiceBayId(), request.getStartsAt(), request.getEndsAt());
        long distinctTechnicians = sameBay.stream().map(EmployeeShift::getEmployeeId).distinct().count();
        if (distinctTechnicians >= ShiftPolicy.MAX_TECHNICIANS_PER_BAY) {
            throw new BusinessRuleException("Khoang đã đủ " + ShiftPolicy.MAX_TECHNICIANS_PER_BAY
                    + " thợ trực trong khung giờ này (1 thợ chính và 1 thợ phụ). Hãy chọn khoang khác hoặc đổi giờ.");
        }
    }

    /** Daily (12h), weekly (60h) and overnight rest (8h) limits for one technician. */
    private void validateWorkingLimits(Long employeeId, Instant startsAt, Instant endsAt) {
        ZoneId zone = ShiftPolicy.ZONE;
        LocalDate startDay = startsAt.atZone(zone).toLocalDate();
        LocalDate endDay = endsAt.atZone(zone).toLocalDate();

        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
            Instant from = day.atStartOfDay(zone).toInstant();
            Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
            if (workedSeconds(employeeId, from, to, startsAt, endsAt) > ShiftPolicy.MAX_DAILY_HOURS * 3600) {
                throw new BusinessRuleException("Vượt giới hạn " + ShiftPolicy.MAX_DAILY_HOURS
                        + " giờ làm việc trong ngày " + day.getDayOfMonth() + "/" + day.getMonthValue());
            }
        }

        for (LocalDate weekStart = startDay.with(DayOfWeek.MONDAY);
                !weekStart.isAfter(endDay);
                weekStart = weekStart.plusWeeks(1)) {
            Instant from = weekStart.atStartOfDay(zone).toInstant();
            Instant to = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant();
            if (workedSeconds(employeeId, from, to, startsAt, endsAt) > ShiftPolicy.MAX_WEEKLY_HOURS * 3600) {
                throw new BusinessRuleException("Vượt giới hạn " + ShiftPolicy.MAX_WEEKLY_HOURS
                        + " giờ làm việc trong tuần bắt đầu ngày " + weekStart.getDayOfMonth() + "/" + weekStart.getMonthValue());
            }
        }

        Duration rest = Duration.ofHours(ShiftPolicy.MIN_REST_HOURS);
        for (EmployeeShift other : employeeShiftRepository.findOverlappingShifts(
                employeeId, ShiftStatus.ACTIVE, startsAt.minus(rest), endsAt.plus(rest))) {
            boolean before = !other.getEndsAt().isAfter(startsAt);
            boolean after = !other.getStartsAt().isBefore(endsAt);
            if (before && other.getEndsAt().plus(rest).isAfter(startsAt)
                    && !other.getEndsAt().atZone(zone).toLocalDate().equals(startDay)) {
                throw new BusinessRuleException("Cần nghỉ tối thiểu " + ShiftPolicy.MIN_REST_HOURS
                        + " giờ giữa hai ca ở hai ngày liên tiếp");
            }
            if (after && endsAt.plus(rest).isAfter(other.getStartsAt())
                    && !other.getStartsAt().atZone(zone).toLocalDate().equals(endDay)) {
                throw new BusinessRuleException("Cần nghỉ tối thiểu " + ShiftPolicy.MIN_REST_HOURS
                        + " giờ giữa hai ca ở hai ngày liên tiếp");
            }
        }
    }

    private long workedSeconds(Long employeeId, Instant from, Instant to, Instant newStart, Instant newEnd) {
        long total = overlapSeconds(newStart, newEnd, from, to);
        for (EmployeeShift existing : employeeShiftRepository.findOverlappingShifts(
                employeeId, ShiftStatus.ACTIVE, from, to)) {
            total += overlapSeconds(existing.getStartsAt(), existing.getEndsAt(), from, to);
        }
        return total;
    }

    private long overlapSeconds(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        Instant start = aStart.isAfter(bStart) ? aStart : bStart;
        Instant end = aEnd.isBefore(bEnd) ? aEnd : bEnd;
        return end.isAfter(start) ? Duration.between(start, end).getSeconds() : 0;
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_REGISTER_SHIFT, entity = AuditConstants.ENTITY_EMPLOYEE_SHIFT)
    public ShiftResponse registerShift(Long actorUserId, ShiftRegisterRequest request) {
        Long actorId = ActorIds.require(actorUserId);
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new BusinessRuleException("Giờ kết thúc ca phải sau giờ bắt đầu");
        }
        if (!request.getEndsAt().isAfter(Instant.now())) {
            throw new BusinessRuleException("Không thể đăng ký ca đã kết thúc");
        }
        Employee employee = employeeRepository.findByUserId(actorId)
                .filter(emp -> Boolean.TRUE.equals(emp.getIsActive()))
                .orElseThrow(() -> new BusinessRuleException("Tài khoản chưa có hồ sơ kỹ thuật viên đang hoạt động"));
        if (request.getServiceBayId() != null && !serviceBayRepository.existsById(request.getServiceBayId())) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
        }
        requireNoOwnOverlap(employee.getId(), request.getStartsAt(), request.getEndsAt());
        validateWorkingLimits(employee.getId(), request.getStartsAt(), request.getEndsAt());

        EmployeeShift shift = EmployeeShift.builder()
                .employeeId(employee.getId())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .shiftType(request.getShiftType() != null ? request.getShiftType() : ShiftType.FULL_DAY)
                .serviceBayId(request.getServiceBayId())
                .status(ShiftStatus.PENDING)
                .notes(request.getNotes())
                .registeredAt(Instant.now())
                .createdBy(actorId)
                .build();
        return employeeShiftMapper.toResponse(employeeShiftRepository.save(shift));
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_APPROVE_SHIFT_REGISTRATION, entity = AuditConstants.ENTITY_EMPLOYEE_SHIFT)
    public ShiftResponse approveShiftRegistration(Long actorUserId, Long shiftId) {
        EmployeeShift shift = loadPendingShift(shiftId);
        requireNoOwnOverlap(shift.getEmployeeId(), shift.getStartsAt(), shift.getEndsAt());
        validateWorkingLimits(shift.getEmployeeId(), shift.getStartsAt(), shift.getEndsAt());
        validateBayCapacity(ShiftCreateRequest.builder()
                .employeeId(shift.getEmployeeId())
                .startsAt(shift.getStartsAt())
                .endsAt(shift.getEndsAt())
                .serviceBayId(shift.getServiceBayId())
                .build());
        shift.setStatus(ShiftStatus.ACTIVE);
        shift.setApprovedBy(ActorIds.require(actorUserId));
        shift.setRejectionReason(null);
        return employeeShiftMapper.toResponse(employeeShiftRepository.save(shift));
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_REJECT_SHIFT_REGISTRATION, entity = AuditConstants.ENTITY_EMPLOYEE_SHIFT)
    public ShiftResponse rejectShiftRegistration(Long actorUserId, Long shiftId, String reason) {
        EmployeeShift shift = loadPendingShift(shiftId);
        shift.setStatus(ShiftStatus.REJECTED);
        shift.setApprovedBy(ActorIds.require(actorUserId));
        shift.setRejectionReason(reason.trim());
        return employeeShiftMapper.toResponse(employeeShiftRepository.save(shift));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftResponse> getPendingRegistrations(Pageable pageable) {
        return employeeShiftRepository.findByStatus(ShiftStatus.PENDING, pageable)
                .map(employeeShiftMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftResponse> getMyShifts(Long actorUserId, Pageable pageable) {
        return employeeRepository.findByUserId(ActorIds.require(actorUserId))
                .map(emp -> employeeShiftRepository.findByEmployeeId(emp.getId(), pageable)
                        .map(employeeShiftMapper::toResponse))
                .orElseGet(() -> Page.empty(pageable));
    }

    private EmployeeShift loadPendingShift(Long shiftId) {
        EmployeeShift shift = employeeShiftRepository.findById(shiftId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        if (shift.getStatus() != ShiftStatus.PENDING) {
            throw new BusinessRuleException("Yêu cầu đăng ký ca này đã được xử lý");
        }
        return shift;
    }

    /** Neither a working nor a still-pending shift of the same technician may overlap the new one. */
    private void requireNoOwnOverlap(Long employeeId, Instant startsAt, Instant endsAt) {
        for (ShiftStatus status : List.of(ShiftStatus.ACTIVE, ShiftStatus.PENDING)) {
            if (!employeeShiftRepository.findOverlappingShifts(employeeId, status, startsAt, endsAt).isEmpty()) {
                throw new BusinessRuleException(status == ShiftStatus.ACTIVE
                        ? "Kỹ thuật viên đã có ca làm việc khác trong khoảng thời gian này"
                        : "Đã có một yêu cầu đăng ký ca chờ duyệt trùng khoảng thời gian này");
            }
        }
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_CANCEL_SHIFT, entity = AuditConstants.ENTITY_EMPLOYEE_SHIFT, idArgument = 1)
    public void cancelShift(Long actorUserId, Long shiftId) {
        EmployeeShift shift = employeeShiftRepository.findById(shiftId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        shift.setStatus(ShiftStatus.CANCELLED);
        employeeShiftRepository.save(shift);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftResponse> getEmployeeShifts(Long employeeId, Pageable pageable) {
        return employeeShiftRepository.findByEmployeeId(employeeId, pageable)
                .map(employeeShiftMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftResponse> getShifts(Long employeeId, ShiftStatus status, Pageable pageable) {
        Page<EmployeeShift> page;
        if (employeeId != null && status != null) {
            page = employeeShiftRepository.findByEmployeeIdAndStatus(employeeId, status, pageable);
        } else if (employeeId != null) {
            page = employeeShiftRepository.findByEmployeeId(employeeId, pageable);
        } else if (status != null) {
            page = employeeShiftRepository.findByStatus(status, pageable);
        } else {
            page = employeeShiftRepository.findAll(pageable);
        }
        return page.map(employeeShiftMapper::toResponse);
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_CREATE_BAY, entity = AuditConstants.ENTITY_SERVICE_BAY)
    public ServiceBayResponse createServiceBay(ServiceBayCreateRequest request) {
        if (serviceBayRepository.existsByBayCode(request.getBayCode().trim())) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }
        ServiceBay bay = serviceBayMapper.toEntity(request);
        bay.setBayCode(request.getBayCode().trim());
        ServiceBay saved = serviceBayRepository.save(bay);
        return serviceBayMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceBayResponse getServiceBayById(Long id) {
        ServiceBay bay = serviceBayRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return serviceBayMapper.toResponse(bay);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceBayResponse> getServiceBays(Boolean isActive, Pageable pageable) {
        Page<ServiceBay> page = (isActive == null)
                ? serviceBayRepository.findAll(pageable)
                : serviceBayRepository.findByIsActive(isActive, pageable);
        return page.map(serviceBayMapper::toResponse);
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_UPDATE_BAY, entity = AuditConstants.ENTITY_SERVICE_BAY)
    public ServiceBayResponse updateServiceBay(Long id, ServiceBayUpdateRequest request) {
        ServiceBay bay = serviceBayRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        serviceBayMapper.updateEntityFromRequest(request, bay);
        ServiceBay updated = serviceBayRepository.save(bay);
        return serviceBayMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_DEACTIVATE_BAY, entity = AuditConstants.ENTITY_SERVICE_BAY, idArgument = 0)
    public void deactivateServiceBay(Long id) {
        ServiceBay bay = serviceBayRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        bay.setIsActive(false);
        serviceBayRepository.save(bay);
    }

    @Override
    @Transactional
    @Audited(action = AuditConstants.ACTION_SCHEDULE_MAINTENANCE, entity = AuditConstants.ENTITY_BAY_RESERVATION)
    public BayReservationResponse scheduleMaintenance(Long actorUserId, MaintenanceReservationRequest request) {
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        ServiceBay bay = serviceBayRepository.findByIdWithLock(request.getBayId())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (!Boolean.TRUE.equals(bay.getIsActive())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        List<BayReservation> overlaps = checkBayAvailability(bay.getId(), request.getStartsAt(), request.getEndsAt());
        if (!overlaps.isEmpty()) {
            throw new BusinessException(ResponseCode.BAY_UNAVAILABLE);
        }

        BayReservation reservation = BayReservation.builder()
                .bayId(bay.getId())
                .reservationType(ReservationType.MAINTENANCE)
                .status(ReservationStatus.IN_USE)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .notes(request.getNotes())
                .createdBy(ActorIds.require(actorUserId))
                .build();

        BayReservation saved = bayReservationRepository.save(reservation);
        return bayReservationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BayReservationResponse> getBayReservations(Long bayId, Pageable pageable) {
        return bayReservationRepository.findByBayId(bayId, pageable)
                .map(bayReservationMapper::toResponse);
    }

    // --- BayScheduling (I02) implementation ---

    @Override
    @Transactional
    public long reserveBooking(long actorUserId, long bookingId, long bayId, Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        ServiceBay bay = serviceBayRepository.findByIdWithLock(bayId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (!Boolean.TRUE.equals(bay.getIsActive())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        List<BayReservation> activeOverlaps = checkBayAvailability(bayId, startsAt, endsAt);
        if (!activeOverlaps.isEmpty()) {
            throw new BusinessException(ResponseCode.BAY_UNAVAILABLE);
        }

        BayReservation reservation = BayReservation.builder()
                .bayId(bayId)
                .bookingId(bookingId)
                .reservationType(ReservationType.BOOKING)
                .status(ReservationStatus.HELD)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .expiresAt(Instant.now().plus(workshop.getReservationHold()))
                .createdBy(actorUserId)
                .build();

        BayReservation saved = bayReservationRepository.save(reservation);
        log.info("Reserved bay {} for booking {} with reservation ID {}", bayId, bookingId, saved.getId());
        return saved.getId();
    }

    @Override
    @Transactional
    public long reserveRepair(long actorUserId, long repairOrderId, long bayId, Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        ServiceBay bay = serviceBayRepository.findByIdWithLock(bayId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (!Boolean.TRUE.equals(bay.getIsActive())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        List<BayReservation> activeOverlaps = checkBayAvailability(bayId, startsAt, endsAt);
        if (!activeOverlaps.isEmpty()) {
            throw new BusinessException(ResponseCode.BAY_UNAVAILABLE);
        }

        BayReservation reservation = BayReservation.builder()
                .bayId(bayId)
                .repairOrderId(repairOrderId)
                .reservationType(ReservationType.REPAIR)
                .status(ReservationStatus.IN_USE)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .createdBy(actorUserId)
                .build();

        BayReservation saved = bayReservationRepository.save(reservation);
        log.info("Reserved bay {} for repair order {} with reservation ID {}", bayId, repairOrderId, saved.getId());
        return saved.getId();
    }

    @Override
    @Transactional
    public void releaseForBooking(long actorUserId, long bookingId) {
        List<BayReservation> reservations = bayReservationRepository.findByBookingIdAndStatusIn(
                bookingId, Set.of(ReservationStatus.HELD, ReservationStatus.IN_USE));
        Instant now = Instant.now();
        for (BayReservation reservation : reservations) {
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReleasedAt(now);
            bayReservationRepository.save(reservation);
            log.info("Released bay reservation {} for booking {}", reservation.getId(), bookingId);
        }
    }

    @Override
    @Transactional
    public void releaseForRepair(long actorUserId, long repairOrderId) {
        List<BayReservation> reservations = bayReservationRepository.findByRepairOrderIdAndStatusIn(
                repairOrderId, Set.of(ReservationStatus.HELD, ReservationStatus.IN_USE));
        Instant now = Instant.now();
        for (BayReservation reservation : reservations) {
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReleasedAt(now);
            bayReservationRepository.save(reservation);
            log.info("Released bay reservation {} for repair order {}", reservation.getId(), repairOrderId);
        }
    }

    private List<BayReservation> checkBayAvailability(Long bayId, Instant startsAt, Instant endsAt) {
        List<BayReservation> candidates = bayReservationRepository.findOverlappingReservations(
                bayId, Set.of(ReservationStatus.HELD, ReservationStatus.IN_USE), startsAt, endsAt);

        Instant now = Instant.now();
        return candidates.stream()
                .filter(r -> {
                    if (r.getStatus() == ReservationStatus.HELD && r.getExpiresAt() != null && r.getExpiresAt().isBefore(now)) {
                        r.setStatus(ReservationStatus.EXPIRED);
                        bayReservationRepository.save(r);
                        return false;
                    }
                    return true;
                })
                .toList();
    }
}
