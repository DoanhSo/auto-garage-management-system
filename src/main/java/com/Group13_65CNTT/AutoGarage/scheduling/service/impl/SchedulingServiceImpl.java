package com.Group13_65CNTT.AutoGarage.scheduling.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.MaintenanceReservationRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftCreateRequest;
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
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulingServiceImpl implements SchedulingService, BayScheduling {

    private final EmployeeRepository employeeRepository;
    private final EmployeeShiftRepository employeeShiftRepository;
    private final ServiceBayRepository serviceBayRepository;
    private final BayReservationRepository bayReservationRepository;
    private final UserRepository userRepository;

    private final EmployeeMapper employeeMapper;
    private final EmployeeShiftMapper employeeShiftMapper;
    private final ServiceBayMapper serviceBayMapper;
    private final BayReservationMapper bayReservationMapper;

    @Override
    @Transactional
    @Audited(action = "CREATE_EMPLOYEE", entity = "Employee")
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
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByUserId(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployees(Boolean isActive, Pageable pageable) {
        Page<Employee> page = (isActive == null)
                ? employeeRepository.findAll(pageable)
                : employeeRepository.findByIsActive(isActive, pageable);
        return page.map(employeeMapper::toResponse);
    }

    @Override
    @Transactional
    @Audited(action = "UPDATE_EMPLOYEE", entity = "Employee")
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        employeeMapper.updateEntityFromRequest(request, employee);
        Employee updated = employeeRepository.save(employee);
        return employeeMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Audited(action = "DEACTIVATE_EMPLOYEE", entity = "Employee")
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        employee.setIsActive(false);
        employeeRepository.save(employee);
    }

    @Override
    @Transactional
    @Audited(action = "CREATE_SHIFT", entity = "EmployeeShift")
    public ShiftResponse createShift(Long actorUserId, ShiftCreateRequest request) {
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (!Boolean.TRUE.equals(employee.getIsActive())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        List<EmployeeShift> overlaps = employeeShiftRepository.findOverlappingShifts(
                request.getEmployeeId(), ShiftStatus.ACTIVE, request.getStartsAt(), request.getEndsAt());
        if (!overlaps.isEmpty()) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }

        EmployeeShift shift = employeeShiftMapper.toEntity(request);
        shift.setStatus(ShiftStatus.ACTIVE);
        shift.setCreatedBy(actorUserId != null ? actorUserId : 1L);
        EmployeeShift saved = employeeShiftRepository.save(shift);
        return employeeShiftMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Audited(action = "CANCEL_SHIFT", entity = "EmployeeShift")
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
    @Transactional
    @Audited(action = "CREATE_BAY", entity = "ServiceBay")
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
    @Audited(action = "UPDATE_BAY", entity = "ServiceBay")
    public ServiceBayResponse updateServiceBay(Long id, ServiceBayUpdateRequest request) {
        ServiceBay bay = serviceBayRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        serviceBayMapper.updateEntityFromRequest(request, bay);
        ServiceBay updated = serviceBayRepository.save(bay);
        return serviceBayMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Audited(action = "DEACTIVATE_BAY", entity = "ServiceBay")
    public void deactivateServiceBay(Long id) {
        ServiceBay bay = serviceBayRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        bay.setIsActive(false);
        serviceBayRepository.save(bay);
    }

    @Override
    @Transactional
    @Audited(action = "SCHEDULE_MAINTENANCE", entity = "BayReservation")
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
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }

        BayReservation reservation = BayReservation.builder()
                .bayId(bay.getId())
                .reservationType(ReservationType.MAINTENANCE)
                .status(ReservationStatus.IN_USE)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .notes(request.getNotes())
                .createdBy(actorUserId != null ? actorUserId : 1L)
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
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }

        BayReservation reservation = BayReservation.builder()
                .bayId(bayId)
                .bookingId(bookingId)
                .reservationType(ReservationType.BOOKING)
                .status(ReservationStatus.HELD)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(30)))
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
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
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
