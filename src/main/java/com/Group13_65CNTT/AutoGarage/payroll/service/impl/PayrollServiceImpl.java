package com.Group13_65CNTT.AutoGarage.payroll.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.payroll.constant.PayrollConstants;
import com.Group13_65CNTT.AutoGarage.payroll.dto.request.CommissionStatusUpdateRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.request.PayrollItemAdjustRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.request.PayrollPeriodCreateRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.OrderCommissionResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollItemResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodDetailResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayslipResponse;
import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionStatus;
import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionType;
import com.Group13_65CNTT.AutoGarage.payroll.entity.OrderCommission;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollItem;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollItemStatus;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriod;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriodStatus;
import com.Group13_65CNTT.AutoGarage.payroll.mapper.PayrollMapper;
import com.Group13_65CNTT.AutoGarage.payroll.repository.OrderCommissionRepository;
import com.Group13_65CNTT.AutoGarage.payroll.repository.PayrollItemRepository;
import com.Group13_65CNTT.AutoGarage.payroll.repository.PayrollPeriodRepository;
import com.Group13_65CNTT.AutoGarage.payroll.service.PayrollService;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentRole;
import com.Group13_65CNTT.AutoGarage.repair.entity.LaborSession;
import com.Group13_65CNTT.AutoGarage.repair.entity.OrderType;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskType;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.TaskAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private static final int MONEY_SCALE = 2;

    private final PayrollPeriodRepository periodRepository;
    private final PayrollItemRepository itemRepository;
    private final OrderCommissionRepository commissionRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final LaborSessionRepository laborSessionRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final RepairTaskRepository repairTaskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final VehicleCheckRepository vehicleCheckRepository;
    private final com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository serviceRepository;
    private final ServiceBayRepository serviceBayRepository;
    private final VehicleRepository vehicleRepository;
    private final PayrollMapper mapper;

    @Autowired
    @Lazy
    private BillingOperations billingOperations;

    // =========================================================================
    // Periods
    // =========================================================================

    @Override
    @Transactional
    @Audited(action = "CREATE_PAYROLL_PERIOD", entity = "PayrollPeriod")
    public PayrollPeriodResponse createPeriod(long actorUserId, PayrollPeriodCreateRequest request) {
        YearMonth month;
        try {
            month = YearMonth.parse(request.getPeriodMonth());
        } catch (DateTimeParseException failure) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, failure);
        }
        LocalDate from = request.getFromDate() != null ? request.getFromDate() : month.atDay(1);
        LocalDate to = request.getToDate() != null ? request.getToDate() : month.atEndOfMonth();
        if (to.isBefore(from)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        if (periodRepository.existsByPeriodMonth(request.getPeriodMonth())) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }
        PayrollPeriod period = PayrollPeriod.builder()
                .periodCode(PayrollConstants.PERIOD_CODE_PREFIX + request.getPeriodMonth())
                .periodMonth(request.getPeriodMonth())
                .fromDate(from)
                .toDate(to)
                .notes(request.getNotes())
                .build();
        return mapper.toResponse(periodRepository.save(period), 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollPeriodResponse> listPeriods(Integer year, Pageable pageable) {
        String yearPrefix = year == null ? null : String.format("%04d-", year);
        return periodRepository.search(yearPrefix, pageable)
                .map(period -> mapper.toResponse(period, commissionRepository.countDistinctOrdersByPeriod(period.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollPeriodDetailResponse getPeriod(Long periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND));
        return detail(period);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollPeriodDetailResponse getPeriodByMonth(String periodMonth) {
        return periodRepository.findByPeriodMonth(periodMonth).map(this::detail).orElse(null);
    }

    @Override
    @Transactional
    @Audited(action = "CALCULATE_PAYROLL", entity = "PayrollPeriod")
    public PayrollPeriodDetailResponse calculatePeriod(long actorUserId, Long periodId) {
        PayrollPeriod period = lockEditable(periodId);
        Instant from = period.getFromDate().atStartOfDay(PayrollConstants.ZONE).toInstant();
        Instant to = period.getToDate().plusDays(1).atStartOfDay(PayrollConstants.ZONE).toInstant();

        commissionRepository.unlinkFromPeriod(periodId, CommissionStatus.PAID_OUT);

        Map<Long, List<LaborSession>> sessionsByEmployee = laborSessionRepository.findClosedBetween(from, to).stream()
                .collect(Collectors.groupingBy(LaborSession::getEmployeeId));
        Map<Long, List<OrderCommission>> commissionsByEmployee =
                commissionRepository.findUnlinked(CommissionStatus.APPROVED, from, to).stream()
                        .collect(Collectors.groupingBy(OrderCommission::getEmployeeId));
        Map<Long, PayrollItem> existing = itemRepository.findByPayrollPeriodIdOrderByEmployeeIdAsc(periodId).stream()
                .collect(Collectors.toMap(PayrollItem::getEmployeeId, Function.identity()));

        Set<Long> employeeIds = new LinkedHashSet<>();
        employeeRepository.findByIsActiveTrue().forEach(employee -> employeeIds.add(employee.getId()));
        employeeIds.addAll(sessionsByEmployee.keySet());
        employeeIds.addAll(commissionsByEmployee.keySet());
        employeeIds.addAll(existing.keySet());

        for (Employee employee : employeeRepository.findAllById(employeeIds)) {
            List<LaborSession> sessions = sessionsByEmployee.getOrDefault(employee.getId(), List.of());
            List<OrderCommission> commissions = commissionsByEmployee.getOrDefault(employee.getId(), List.of());
            long shifts = shiftRepository.countByEmployeeIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                    employee.getId(), ShiftStatus.ACTIVE, from, to);
            PayrollItem item = existing.get(employee.getId());
            boolean relevant = item != null
                    || employee.getBaseSalary().signum() > 0
                    || !sessions.isEmpty()
                    || !commissions.isEmpty()
                    || shifts > 0;
            if (!relevant) {
                continue;
            }
            if (item == null) {
                item = PayrollItem.builder().payrollPeriodId(periodId).employeeId(employee.getId()).build();
            }
            applyLabor(item, employee, sessions);
            item.setShiftCount((int) shifts);
            item.setCommissionAmount(commissions.stream()
                    .map(commission -> commission.getCommissionType() == CommissionType.WARRANTY_DEDUCTION
                            ? commission.getCommissionAmount().negate()
                            : commission.getCommissionAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setOrderBonusAmount(commissions.stream()
                    .filter(commission -> commission.getCommissionType() == CommissionType.ORDER_COMPLETION)
                    .map(OrderCommission::getCommissionAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setCompletedOrderCount((int) commissions.stream()
                    .filter(commission -> commission.getCommissionType() != CommissionType.WARRANTY_DEDUCTION)
                    .map(OrderCommission::getRepairOrderId)
                    .distinct()
                    .count());
            item.setStatus(PayrollItemStatus.CALCULATED);
            recompute(item);
            PayrollItem saved = itemRepository.save(item);
            commissions.forEach(commission -> commission.setPayrollItemId(saved.getId()));
            commissionRepository.saveAll(commissions);
        }

        period.setStatus(PayrollPeriodStatus.CALCULATED);
        refreshTotals(period);
        periodRepository.save(period);
        return detail(period);
    }

    @Override
    @Transactional
    @Audited(action = "ADJUST_PAYROLL_ITEM", entity = "PayrollItem")
    public PayrollItemResponse adjustItem(
            long actorUserId, Long periodId, Long itemId, PayrollItemAdjustRequest request) {
        PayrollPeriod period = lockEditable(periodId);
        PayrollItem item = itemRepository.findByIdAndPayrollPeriodId(itemId, periodId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND));
        if (request.getAllowanceAmount() != null) {
            item.setAllowanceAmount(request.getAllowanceAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }
        if (request.getDeductionAmount() != null) {
            item.setDeductionAmount(request.getDeductionAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }
        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }
        recompute(item);
        if (item.getNetPay().signum() < 0) {
            throw new BusinessRuleException("Khoản giảm trừ không được lớn hơn tổng thu nhập");
        }
        itemRepository.save(item);
        refreshTotals(period);
        periodRepository.save(period);
        return toItemResponse(item);
    }

    @Override
    @Transactional
    @Audited(action = "APPROVE_PAYROLL", entity = "PayrollPeriod")
    public PayrollPeriodResponse approvePeriod(long actorUserId, Long periodId) {
        PayrollPeriod period = lockPeriod(periodId);
        if (period.getStatus() != PayrollPeriodStatus.CALCULATED) {
            throw new BusinessRuleException("Chỉ phê duyệt được bảng lương đã tính toán");
        }
        List<PayrollItem> items = itemRepository.findByPayrollPeriodIdOrderByEmployeeIdAsc(periodId);
        if (items.isEmpty()) {
            throw new BusinessRuleException("Bảng lương chưa có nhân viên nào");
        }
        items.forEach(item -> item.setStatus(PayrollItemStatus.CONFIRMED));
        itemRepository.saveAll(items);
        period.setStatus(PayrollPeriodStatus.APPROVED);
        period.setApprovedBy(actorUserId);
        period.setApprovedAt(Instant.now());
        return toPeriodResponse(periodRepository.save(period));
    }

    @Override
    @Transactional
    @Audited(action = "DISBURSE_PAYROLL", entity = "PayrollPeriod")
    public PayrollPeriodResponse disbursePeriod(long actorUserId, Long periodId) {
        PayrollPeriod period = lockPeriod(periodId);
        if (period.getStatus() != PayrollPeriodStatus.APPROVED) {
            throw new BusinessRuleException("Chỉ chi trả được bảng lương đã phê duyệt");
        }
        if (period.getTotalNetPay().signum() <= 0) {
            throw new BusinessRuleException("Tổng thực chi bằng 0, không có khoản nào để chi trả");
        }
        long paymentId = billingOperations.recordPayrollDisbursement(
                actorUserId, periodId, period.getTotalNetPay(), PayrollConstants.PAYMENT_NOTE_PREFIX + period.getPeriodCode());

        Instant now = Instant.now();
        List<PayrollItem> items = itemRepository.findByPayrollPeriodIdOrderByEmployeeIdAsc(periodId);
        items.forEach(item -> {
            item.setStatus(PayrollItemStatus.PAID);
            item.setPaidAt(now);
        });
        itemRepository.saveAll(items);
        commissionRepository.markPaidForPeriod(periodId, CommissionStatus.APPROVED, CommissionStatus.PAID_OUT);

        period.setStatus(PayrollPeriodStatus.PAID);
        period.setPaymentId(paymentId);
        return toPeriodResponse(periodRepository.save(period));
    }

    // =========================================================================
    // Payslips
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<PayslipResponse> getMyPayslips(long actorUserId, Pageable pageable) {
        Employee employee = employeeRepository.findByUserId(actorUserId).orElse(null);
        if (employee == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        String name = userRepository.findById(actorUserId).map(User::getFullName).orElse(null);
        return itemRepository.findPublishedByEmployee(employee.getId(), pageable).map(item -> {
            PayrollPeriod period = periodRepository.findById(item.getPayrollPeriodId()).orElseThrow();
            Instant from = period.getFromDate().atStartOfDay(PayrollConstants.ZONE).toInstant();
            Instant to = period.getToDate().plusDays(1).atStartOfDay(PayrollConstants.ZONE).toInstant();
            List<LaborSession> sessions = laborSessionRepository.findClosedBetween(from, to).stream()
                    .filter(session -> session.getEmployeeId().equals(employee.getId()))
                    .toList();
            List<OrderCommissionResponse> commissions = commissionRepository.findByPayrollItemId(item.getId()).stream()
                    .map(mapper::toResponse)
                    .toList();
            return new PayslipResponse(
                    mapper.toResponse(item, employee.getEmployeeCode(), name),
                    period.getPeriodCode(),
                    period.getPeriodMonth(),
                    period.getFromDate(),
                    period.getToDate(),
                    period.getStatus(),
                    laborDays(sessions),
                    commissions);
        });
    }

    // =========================================================================
    // Commissions
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderCommissionResponse> searchCommissions(
            Long employeeId,
            Long repairOrderId,
            CommissionStatus status,
            Instant fromEarnedAt,
            Instant toEarnedAt,
            Pageable pageable) {
        return commissionRepository
                .search(employeeId, repairOrderId, status, fromEarnedAt, toEarnedAt, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    @Audited(action = "CALCULATE_ORDER_COMMISSION", entity = "OrderCommission")
    public List<OrderCommissionResponse> calculateOrderCommissions(long actorUserId, Long repairOrderId) {
        RepairOrder order = repairOrderRepository.findById(repairOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND));
        if (order.getStatus() != RepairOrderStatus.CLOSED && order.getStatus() != RepairOrderStatus.INVOICED) {
            throw new BusinessRuleException("Chỉ tính thưởng cho phiếu đã xuất hóa đơn hoặc đã bàn giao");
        }
        createCommissions(order);
        return commissionRepository.search(null, repairOrderId, null, null, null, Pageable.unpaged())
                .map(mapper::toResponse)
                .getContent();
    }

    @Override
    @Transactional
    public int recordOrderCommissions(long actorUserId, long repairOrderId) {
        return repairOrderRepository.findById(repairOrderId).map(this::createCommissions).orElse(0);
    }

    @Override
    @Transactional
    @Audited(action = "UPDATE_COMMISSION_STATUS", entity = "OrderCommission")
    public OrderCommissionResponse updateCommissionStatus(
            long actorUserId, Long commissionId, CommissionStatusUpdateRequest request) {
        if (request.getStatus() != CommissionStatus.APPROVED && request.getStatus() != CommissionStatus.CANCELLED) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        OrderCommission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND));
        if (commission.getStatus() == CommissionStatus.PAID_OUT) {
            throw new BusinessRuleException("Khoản thưởng đã chi trả, không thể thay đổi");
        }
        if (commission.getPayrollItemId() != null) {
            PayrollItem item = itemRepository.findById(commission.getPayrollItemId()).orElseThrow();
            PayrollPeriod period = periodRepository.findById(item.getPayrollPeriodId()).orElseThrow();
            if (period.getStatus() == PayrollPeriodStatus.APPROVED || period.getStatus() == PayrollPeriodStatus.PAID) {
                throw new BusinessRuleException("Khoản thưởng thuộc bảng lương đã phê duyệt, không thể thay đổi");
            }
            commission.setPayrollItemId(null);
        }
        commission.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            commission.setNotes(request.getNotes());
        }
        return mapper.toResponse(commissionRepository.save(commission));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private int createCommissions(RepairOrder order) {
        if (order.getOrderType() == OrderType.WARRANTY
                || (order.getStatus() != RepairOrderStatus.CLOSED && order.getStatus() != RepairOrderStatus.INVOICED)
                || commissionRepository.existsByRepairOrderId(order.getId())) {
            return 0;
        }
        Instant earnedAt = order.getClosedAt() != null ? order.getClosedAt() : Instant.now();
        List<RepairTask> tasks = repairTaskRepository.findByRepairOrderId(order.getId()).stream()
                .filter(task -> task.getTaskType() == TaskType.REPAIR && task.getStatus() == TaskStatus.COMPLETED)
                .toList();
        if (tasks.isEmpty()) {
            return 0;
        }
        Set<Long> employeeIds = new HashSet<>();
        Map<Long, List<TaskAssignment>> assignmentsByTask = new HashMap<>();
        for (RepairTask task : tasks) {
            List<TaskAssignment> assignments = assignmentRepository.findByTaskId(task.getId());
            assignmentsByTask.put(task.getId(), assignments);
            assignments.forEach(assignment -> employeeIds.add(assignment.getEmployeeId()));
        }
        if (order.getLeadTechnicianId() != null) {
            employeeIds.add(order.getLeadTechnicianId());
        }
        Map<Long, Employee> employees = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        List<OrderCommission> created = new ArrayList<>();
        BigDecimal totalLabor = BigDecimal.ZERO;
        Set<Long> qcEmployees = new LinkedHashSet<>();
        List<TimeSavedCandidate> timeSavedTasks = new ArrayList<>();

        for (RepairTask task : tasks) {
            BigDecimal labor = task.getQuantity().multiply(task.getUnitPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            totalLabor = totalLabor.add(labor);
            List<TaskAssignment> assignments = assignmentsByTask.get(task.getId());
            Set<Long> mains = distinctEmployees(assignments, AssignmentRole.MAIN);
            Set<Long> assistants = distinctEmployees(assignments, AssignmentRole.ASSISTANT);
            qcEmployees.addAll(distinctEmployees(assignments, AssignmentRole.QC));
            if (mains.isEmpty()) {
                continue;
            }
            timeSavedTasks.add(new TimeSavedCandidate(task, mains, assignments));
            BigDecimal mainShare = assistants.isEmpty() ? BigDecimal.ONE : PayrollConstants.MAIN_SHARE_WITH_ASSISTANTS;
            addLaborShares(created, order, task, labor, mains, mainShare, employees, earnedAt);
            addLaborShares(created, order, task, labor, assistants, PayrollConstants.ASSISTANT_SHARE, employees, earnedAt);
        }

        List<VehicleCheck> qcChecks = vehicleCheckRepository
                .findAllByRepairOrderIdAndCheckTypeOrderByAttemptNoAsc(order.getId(), CheckType.QC);
        boolean passedFirstTime = !qcChecks.isEmpty() && qcChecks.get(0).getResult() == CheckResult.PASSED;
        boolean anyPassed = qcChecks.stream().anyMatch(check -> check.getResult() == CheckResult.PASSED);

        if (passedFirstTime) {
            for (TimeSavedCandidate candidate : timeSavedTasks) {
                addTimeSavedBonus(created, order, candidate, employees, earnedAt);
            }
        }

        if (passedFirstTime) {
            addShiftOrderBonuses(created, order, tasks, assignmentsByTask, employees, earnedAt);
        }
        if (anyPassed) {
            for (Long qcEmployee : qcEmployees) {
                created.add(commission(order.getId(), null, qcEmployee, CommissionType.QC_EXCELLENCE,
                        BigDecimal.ZERO, BigDecimal.ZERO, PayrollConstants.QC_PASS_BONUS, earnedAt, null));
            }
        }

        List<OrderCommission> payable = created.stream()
                .filter(commission -> commission.getCommissionAmount().signum() > 0)
                .toList();
        commissionRepository.saveAll(payable);
        log.info("Recorded {} commissions for repair order {}", payable.size(), order.getId());
        return payable.size();
    }

    /**
     * Pays the per-order bonus for a repair that passed QC first time. The tier depends on how many
     * orders the main technician has already completed in the same shift (1-2, 3-4, 5+); the main
     * technician takes 70% and assistants share 30%, or 100% when working alone.
     */
    private void addShiftOrderBonuses(
            List<OrderCommission> sink,
            RepairOrder order,
            List<RepairTask> tasks,
            Map<Long, List<TaskAssignment>> assignmentsByTask,
            Map<Long, Employee> employees,
            Instant earnedAt) {
        Set<Long> mains = new LinkedHashSet<>();
        Set<Long> assistants = new LinkedHashSet<>();
        for (RepairTask task : tasks) {
            List<TaskAssignment> assignments = assignmentsByTask.get(task.getId());
            mains.addAll(distinctEmployees(assignments, AssignmentRole.MAIN));
            assistants.addAll(distinctEmployees(assignments, AssignmentRole.ASSISTANT));
        }
        assistants.removeAll(mains);
        if (mains.isEmpty()) {
            return;
        }
        String plate = vehicleRepository.findById(order.getVehicleId()).map(Vehicle::getLicensePlate).orElse(null);
        Map<Long, EmployeeShift> shifts = new HashMap<>();
        for (Long employeeId : new LinkedHashSet<>(java.util.stream.Stream.concat(mains.stream(), assistants.stream()).toList())) {
            EmployeeShift shift = resolveShift(employeeId, order, tasks, assignmentsByTask, earnedAt);
            if (shift != null) {
                shifts.put(employeeId, shift);
            }
        }

        Long primary = mains.iterator().next();
        EmployeeShift primaryShift = shifts.get(primary);
        int orderNumber = primaryShift == null ? 1
                : (int) commissionRepository.countByEmployeeIdAndShiftIdAndCommissionTypeAndStatusNot(
                        primary, primaryShift.getId(), CommissionType.ORDER_COMPLETION, CommissionStatus.CANCELLED) + 1;
        BigDecimal pool = orderBonusForPosition(orderNumber);

        BigDecimal mainShare = assistants.isEmpty() ? BigDecimal.ONE : PayrollConstants.MAIN_SHARE_WITH_ASSISTANTS;
        addOrderBonusRows(sink, order, plate, mains, pool.multiply(mainShare), shifts, employees, earnedAt);
        addOrderBonusRows(sink, order, plate, assistants, pool.multiply(PayrollConstants.ASSISTANT_SHARE), shifts, employees, earnedAt);
    }

    private BigDecimal orderBonusForPosition(int orderNumber) {
        if (orderNumber <= PayrollConstants.TIER1_LAST_ORDER) {
            return PayrollConstants.TIER1_ORDER_BONUS;
        }
        return orderNumber <= PayrollConstants.TIER2_LAST_ORDER
                ? PayrollConstants.TIER2_ORDER_BONUS
                : PayrollConstants.TIER3_ORDER_BONUS;
    }

    private void addOrderBonusRows(
            List<OrderCommission> sink,
            RepairOrder order,
            String plate,
            Set<Long> recipients,
            BigDecimal groupAmount,
            Map<Long, EmployeeShift> shifts,
            Map<Long, Employee> employees,
            Instant earnedAt) {
        if (recipients.isEmpty()) {
            return;
        }
        BigDecimal each = groupAmount.divide(BigDecimal.valueOf(recipients.size()), MONEY_SCALE, RoundingMode.HALF_UP);
        for (Long employeeId : recipients) {
            if (!employees.containsKey(employeeId)) {
                continue;
            }
            EmployeeShift shift = shifts.get(employeeId);
            String bayCode = shift == null || shift.getServiceBayId() == null ? null
                    : serviceBayRepository.findById(shift.getServiceBayId()).map(ServiceBay::getBayCode).orElse(null);
            String shiftLabel = shift == null || shift.getShiftType() == null ? null
                    : PayrollConstants.SHIFT_LABELS.get(shift.getShiftType());
            StringBuilder note = new StringBuilder("Thưởng đơn xe ").append(plate == null ? "#" + order.getId() : plate);
            if (shiftLabel != null) {
                note.append(" ca ").append(shiftLabel);
            }
            if (bayCode != null) {
                note.append(" tại khoang ").append(bayCode);
            }
            OrderCommission row = commission(order.getId(), null, employeeId, CommissionType.ORDER_COMPLETION,
                    BigDecimal.ZERO, BigDecimal.ZERO, each, earnedAt, note.toString());
            row.setShiftId(shift == null ? null : shift.getId());
            row.setShiftType(shift == null || shift.getShiftType() == null ? null : shift.getShiftType().name());
            row.setServiceBayId(shift == null ? null : shift.getServiceBayId());
            row.setBayCode(bayCode);
            row.setLicensePlate(plate);
            sink.add(row);
        }
    }

    /** The shift covering the end of the technician's labor on this order, else the closing time. */
    private EmployeeShift resolveShift(
            Long employeeId,
            RepairOrder order,
            List<RepairTask> tasks,
            Map<Long, List<TaskAssignment>> assignmentsByTask,
            Instant earnedAt) {
        List<Long> assignmentIds = tasks.stream()
                .flatMap(task -> assignmentsByTask.get(task.getId()).stream())
                .filter(assignment -> assignment.getEmployeeId().equals(employeeId))
                .map(TaskAssignment::getId)
                .toList();
        List<Instant> candidates = new ArrayList<>();
        laborSessionRepository.findByAssignmentIdIn(assignmentIds).stream()
                .map(LaborSession::getEndsAt)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .ifPresent(candidates::add);
        candidates.add(earnedAt);
        for (Instant at : candidates) {
            List<EmployeeShift> found = shiftRepository.findActiveAt(employeeId, at);
            if (!found.isEmpty()) {
                return found.get(0);
            }
        }
        return null;
    }

    private record TimeSavedCandidate(RepairTask task, Set<Long> mains, List<TaskAssignment> assignments) {}

    /** Pays half the value of the book-hours the main technicians saved on a task that passed QC first time. */
    private void addTimeSavedBonus(
            List<OrderCommission> sink,
            RepairOrder order,
            TimeSavedCandidate candidate,
            Map<Long, Employee> employees,
            Instant earnedAt) {
        RepairTask task = candidate.task();
        if (task.getServiceId() == null) {
            return;
        }
        BigDecimal standardMinutes = serviceRepository.findById(task.getServiceId())
                .map(service -> BigDecimal.valueOf(service.getStandardMinutes()))
                .orElse(BigDecimal.ZERO);
        BigDecimal estimatedHours = standardMinutes.multiply(task.getQuantity())
                .divide(PayrollConstants.MINUTES_PER_HOUR, 4, RoundingMode.HALF_UP);
        List<Long> assignmentIds = candidate.assignments().stream().map(TaskAssignment::getId).toList();
        long actualSeconds = laborSessionRepository.findByAssignmentIdIn(assignmentIds).stream()
                .filter(session -> session.getEndsAt() != null)
                .mapToLong(session -> Duration.between(session.getStartsAt(), session.getEndsAt()).getSeconds())
                .sum();
        if (actualSeconds <= 0) {
            return;
        }
        BigDecimal actualHours = BigDecimal.valueOf(actualSeconds)
                .divide(PayrollConstants.SECONDS_PER_HOUR, 4, RoundingMode.HALF_UP);
        BigDecimal savedHours = estimatedHours.subtract(actualHours);
        if (savedHours.signum() <= 0) {
            return;
        }
        BigDecimal perMainHours = savedHours.divide(BigDecimal.valueOf(candidate.mains().size()), 4, RoundingMode.HALF_UP);
        for (Long employeeId : candidate.mains()) {
            Employee employee = employees.get(employeeId);
            if (employee == null) {
                continue;
            }
            BigDecimal base = perMainHours.multiply(employee.getHourlyCost()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal amount = base.multiply(PayrollConstants.TIME_SAVED_BONUS_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            sink.add(commission(order.getId(), task.getId(), employeeId, CommissionType.TIME_SAVED_EFFICIENCY, base,
                    PayrollConstants.TIME_SAVED_BONUS_RATE.multiply(PayrollConstants.HUNDRED), amount, earnedAt,
                    "Tiết kiệm " + perMainHours.setScale(2, RoundingMode.HALF_UP) + " giờ so với định mức"));
        }
    }

    private void addLaborShares(
            List<OrderCommission> sink,
            RepairOrder order,
            RepairTask task,
            BigDecimal labor,
            Set<Long> recipients,
            BigDecimal groupShare,
            Map<Long, Employee> employees,
            Instant earnedAt) {
        if (recipients.isEmpty()) {
            return;
        }
        BigDecimal perPerson = groupShare.divide(BigDecimal.valueOf(recipients.size()), 6, RoundingMode.HALF_UP);
        for (Long employeeId : recipients) {
            Employee employee = employees.get(employeeId);
            if (employee == null) {
                continue;
            }
            BigDecimal base = labor.multiply(perPerson).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal rate = employee.getDefaultCommissionRate();
            BigDecimal amount = base.multiply(rate).divide(PayrollConstants.HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
            sink.add(commission(order.getId(), task.getId(), employeeId, CommissionType.LABOR_SHARE,
                    base, rate, amount, earnedAt, null));
        }
    }

    private OrderCommission commission(
            Long orderId,
            Long taskId,
            Long employeeId,
            CommissionType type,
            BigDecimal base,
            BigDecimal rate,
            BigDecimal amount,
            Instant earnedAt,
            String notes) {
        return OrderCommission.builder()
                .repairOrderId(orderId)
                .taskId(taskId)
                .employeeId(employeeId)
                .commissionType(type)
                .baseAmount(base)
                .ratePercent(rate)
                .commissionAmount(amount)
                .status(CommissionStatus.APPROVED)
                .earnedAt(earnedAt)
                .notes(notes)
                .build();
    }

    private BigDecimal completionBonus(BigDecimal totalLabor) {
        if (totalLabor.compareTo(PayrollConstants.SMALL_ORDER_LABOR_LIMIT) < 0) {
            return PayrollConstants.SMALL_ORDER_BONUS;
        }
        if (totalLabor.compareTo(PayrollConstants.MEDIUM_ORDER_LABOR_LIMIT) < 0) {
            return PayrollConstants.MEDIUM_ORDER_BONUS;
        }
        return PayrollConstants.LARGE_ORDER_BONUS;
    }

    private Set<Long> distinctEmployees(Collection<TaskAssignment> assignments, AssignmentRole role) {
        return assignments.stream()
                .filter(assignment -> assignment.getAssignmentRole() == role)
                .map(TaskAssignment::getEmployeeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Splits timer sessions into regular and overtime hours and prices them with each session's rate snapshot. */
    private void applyLabor(PayrollItem item, Employee employee, List<LaborSession> sessions) {
        long regularSeconds = 0;
        long overtimeSeconds = 0;
        BigDecimal pay = BigDecimal.ZERO;
        for (LaborSession session : sessions) {
            long[] split = splitSeconds(session.getStartsAt(), session.getEndsAt());
            regularSeconds += split[0];
            overtimeSeconds += split[1];
            BigDecimal rate = session.getHourlyCostSnapshot();
            BigDecimal regularPay = rate.multiply(BigDecimal.valueOf(split[0]));
            BigDecimal overtimePay = rate.multiply(employee.getOvertimeMultiplier()).multiply(BigDecimal.valueOf(split[1]));
            pay = pay.add(regularPay.add(overtimePay).divide(PayrollConstants.SECONDS_PER_HOUR, 6, RoundingMode.HALF_UP));
        }
        item.setBaseSalarySnapshot(employee.getBaseSalary());
        item.setHourlyRateSnapshot(employee.getHourlyCost());
        item.setRegularHours(hours(regularSeconds));
        item.setOvertimeHours(hours(overtimeSeconds));
        item.setHourlyPayAmount(pay.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private long[] splitSeconds(Instant start, Instant end) {
        long total = Duration.between(start, end).getSeconds();
        long regular = 0;
        ZonedDateTime cursor = start.atZone(PayrollConstants.ZONE);
        ZonedDateTime stop = end.atZone(PayrollConstants.ZONE);
        while (cursor.isBefore(stop)) {
            LocalDate day = cursor.toLocalDate();
            ZonedDateTime dayEnd = day.plusDays(1).atStartOfDay(PayrollConstants.ZONE);
            ZonedDateTime segmentEnd = stop.isBefore(dayEnd) ? stop : dayEnd;
            ZonedDateTime windowStart = day.atTime(PayrollConstants.REGULAR_START).atZone(PayrollConstants.ZONE);
            ZonedDateTime windowEnd = day.atTime(PayrollConstants.REGULAR_END).atZone(PayrollConstants.ZONE);
            ZonedDateTime from = cursor.isAfter(windowStart) ? cursor : windowStart;
            ZonedDateTime to = segmentEnd.isBefore(windowEnd) ? segmentEnd : windowEnd;
            if (to.isAfter(from)) {
                regular += Duration.between(from, to).getSeconds();
            }
            cursor = segmentEnd;
        }
        return new long[] {regular, total - regular};
    }

    private List<PayslipResponse.LaborDayLine> laborDays(List<LaborSession> sessions) {
        Map<LocalDate, long[]> perDay = new TreeMap<>();
        for (LaborSession session : sessions) {
            long[] split = splitSeconds(session.getStartsAt(), session.getEndsAt());
            long[] bucket = perDay.computeIfAbsent(
                    session.getStartsAt().atZone(PayrollConstants.ZONE).toLocalDate(), key -> new long[2]);
            bucket[0] += split[0];
            bucket[1] += split[1];
        }
        return perDay.entrySet().stream()
                .map(entry -> new PayslipResponse.LaborDayLine(
                        entry.getKey(), hours(entry.getValue()[0]), hours(entry.getValue()[1])))
                .toList();
    }

    private BigDecimal hours(long seconds) {
        return BigDecimal.valueOf(seconds).divide(PayrollConstants.SECONDS_PER_HOUR, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void recompute(PayrollItem item) {
        BigDecimal gross = item.getBaseSalarySnapshot()
                .add(item.getHourlyPayAmount())
                .add(item.getCommissionAmount())
                .add(item.getAllowanceAmount());
        item.setGrossPay(gross);
        item.setNetPay(gross.subtract(item.getDeductionAmount()));
    }

    private void refreshTotals(PayrollPeriod period) {
        List<PayrollItem> items = itemRepository.findByPayrollPeriodIdOrderByEmployeeIdAsc(period.getId());
        period.setTotalEmployees(items.size());
        period.setTotalRegularHours(sum(items, PayrollItem::getRegularHours));
        period.setTotalOvertimeHours(sum(items, PayrollItem::getOvertimeHours));
        period.setTotalBasePay(sum(items, item -> item.getBaseSalarySnapshot().add(item.getHourlyPayAmount())));
        period.setTotalCommissions(sum(items, PayrollItem::getCommissionAmount));
        period.setTotalAllowances(sum(items, PayrollItem::getAllowanceAmount));
        period.setTotalDeductions(sum(items, PayrollItem::getDeductionAmount));
        period.setTotalNetPay(sum(items, PayrollItem::getNetPay));
    }

    private BigDecimal sum(List<PayrollItem> items, Function<PayrollItem, BigDecimal> field) {
        return items.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PayrollPeriod lockPeriod(Long periodId) {
        return periodRepository.findByIdForUpdate(periodId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND));
    }

    private PayrollPeriod lockEditable(Long periodId) {
        PayrollPeriod period = lockPeriod(periodId);
        if (!EnumSet.of(PayrollPeriodStatus.DRAFT, PayrollPeriodStatus.CALCULATED).contains(period.getStatus())) {
            throw new BusinessRuleException("Bảng lương đã được phê duyệt, không thể tính lại hoặc chỉnh sửa");
        }
        return period;
    }

    private PayrollPeriodResponse toPeriodResponse(PayrollPeriod period) {
        return mapper.toResponse(period, commissionRepository.countDistinctOrdersByPeriod(period.getId()));
    }

    private PayrollPeriodDetailResponse detail(PayrollPeriod period) {
        List<PayrollItem> items = itemRepository.findByPayrollPeriodIdOrderByEmployeeIdAsc(period.getId());
        Map<Long, Employee> employees = employeeRepository
                .findAllById(items.stream().map(PayrollItem::getEmployeeId).toList()).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> names = userRepository
                .findAllById(employees.values().stream().map(Employee::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
        List<PayrollItemResponse> responses = items.stream().map(item -> {
            Employee employee = employees.get(item.getEmployeeId());
            return mapper.toResponse(
                    item,
                    employee == null ? null : employee.getEmployeeCode(),
                    employee == null ? null : names.get(employee.getUserId()));
        }).toList();
        return new PayrollPeriodDetailResponse(toPeriodResponse(period), responses);
    }

    private PayrollItemResponse toItemResponse(PayrollItem item) {
        Employee employee = employeeRepository.findById(item.getEmployeeId()).orElse(null);
        String name = employee == null ? null
                : userRepository.findById(employee.getUserId()).map(User::getFullName).orElse(null);
        return mapper.toResponse(item, employee == null ? null : employee.getEmployeeCode(), name);
    }
}
