package com.Group13_65CNTT.AutoGarage.payroll;

import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollItemResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodDetailResponse;
import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionStatus;
import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionType;
import com.Group13_65CNTT.AutoGarage.payroll.entity.OrderCommission;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollItem;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriod;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriodStatus;
import com.Group13_65CNTT.AutoGarage.payroll.mapper.PayrollMapper;
import com.Group13_65CNTT.AutoGarage.payroll.repository.OrderCommissionRepository;
import com.Group13_65CNTT.AutoGarage.payroll.repository.PayrollItemRepository;
import com.Group13_65CNTT.AutoGarage.payroll.repository.PayrollPeriodRepository;
import com.Group13_65CNTT.AutoGarage.payroll.service.impl.PayrollServiceImpl;
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
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private PayrollPeriodRepository periodRepository;
    @Mock private PayrollItemRepository itemRepository;
    @Mock private OrderCommissionRepository commissionRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeShiftRepository shiftRepository;
    @Mock private UserRepository userRepository;
    @Mock private LaborSessionRepository laborSessionRepository;
    @Mock private RepairOrderRepository repairOrderRepository;
    @Mock private RepairTaskRepository repairTaskRepository;
    @Mock private TaskAssignmentRepository assignmentRepository;
    @Mock private VehicleCheckRepository vehicleCheckRepository;
    @Mock private BillingOperations billingOperations;
    @Mock private com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository serviceRepository;
    @Mock private com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository serviceBayRepository;
    @Mock private com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository vehicleRepository;

    @InjectMocks private PayrollServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mapper", new PayrollMapper());
        ReflectionTestUtils.setField(service, "billingOperations", billingOperations);
    }

    private static Employee employee(long id, String hourlyCost, String baseSalary) {
        return Employee.builder()
                .id(id)
                .userId(id + 100)
                .employeeCode("KTV-" + id)
                .hourlyCost(new BigDecimal(hourlyCost))
                .baseSalary(new BigDecimal(baseSalary))
                .overtimeMultiplier(new BigDecimal("1.50"))
                .defaultCommissionRate(new BigDecimal("10.00"))
                .isActive(true)
                .build();
    }

    private RepairOrder closedOrder(Long leadTechnicianId) {
        return RepairOrder.builder()
                .id(7L)
                .status(RepairOrderStatus.CLOSED)
                .orderType(OrderType.NORMAL)
                .leadTechnicianId(leadTechnicianId)
                .closedAt(Instant.parse("2026-10-20T03:00:00Z"))
                .build();
    }

    private static TaskAssignment assignment(long employeeId, AssignmentRole role) {
        return TaskAssignment.builder().taskId(70L).employeeId(employeeId).assignmentRole(role).build();
    }

    private static VehicleCheck qcCheck(CheckResult result) {
        VehicleCheck check = new VehicleCheck();
        check.setCheckType(CheckType.QC);
        check.setResult(result);
        return check;
    }

    @Test
    @DisplayName("Hoa hồng chia 70/30 cho thợ chính và thợ phụ, kèm thưởng hoàn thành và thưởng QC")
    void commissionsAreSplitBetweenMainAssistantAndQc() {
        RepairOrder order = closedOrder(1L);
        when(repairOrderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(commissionRepository.existsByRepairOrderId(7L)).thenReturn(false);
        when(repairTaskRepository.findByRepairOrderId(7L)).thenReturn(List.of(RepairTask.builder()
                .id(70L)
                .taskType(TaskType.REPAIR)
                .status(TaskStatus.COMPLETED)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("1000000"))
                .build()));
        when(assignmentRepository.findByTaskId(70L)).thenReturn(List.of(
                assignment(1L, AssignmentRole.MAIN),
                assignment(2L, AssignmentRole.ASSISTANT),
                assignment(3L, AssignmentRole.QC)));
        when(employeeRepository.findAllById(anyCollection())).thenReturn(List.of(
                employee(1L, "50000", "0"), employee(2L, "40000", "0"), employee(3L, "45000", "0")));
        when(vehicleCheckRepository.findAllByRepairOrderIdAndCheckTypeOrderByAttemptNoAsc(7L, CheckType.QC))
                .thenReturn(List.of(qcCheck(CheckResult.PASSED)));

        int created = service.recordOrderCommissions(1L, 7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderCommission>> captor = ArgumentCaptor.forClass(List.class);
        verify(commissionRepository).saveAll(captor.capture());
        List<OrderCommission> saved = captor.getValue();
        assertThat(created).isEqualTo(5);
        assertThat(amountOf(saved, 1L, CommissionType.LABOR_SHARE)).isEqualByComparingTo("70000");
        assertThat(amountOf(saved, 2L, CommissionType.LABOR_SHARE)).isEqualByComparingTo("30000");
        // Đơn đầu tiên trong ca: thưởng 50.000, thợ chính 70% và thợ phụ 30%.
        assertThat(amountOf(saved, 1L, CommissionType.ORDER_COMPLETION)).isEqualByComparingTo("35000");
        assertThat(amountOf(saved, 2L, CommissionType.ORDER_COMPLETION)).isEqualByComparingTo("15000");
        assertThat(amountOf(saved, 3L, CommissionType.QC_EXCELLENCE)).isEqualByComparingTo("30000");
        assertThat(saved).allMatch(commission -> commission.getStatus() == CommissionStatus.APPROVED);
    }

    @Test
    @DisplayName("Không trả thưởng hoàn thành khi QC không đạt ngay lần đầu")
    void noCompletionBonusWhenFirstQcAttemptFailed() {
        RepairOrder order = closedOrder(1L);
        when(repairOrderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(commissionRepository.existsByRepairOrderId(7L)).thenReturn(false);
        when(repairTaskRepository.findByRepairOrderId(7L)).thenReturn(List.of(RepairTask.builder()
                .id(70L)
                .taskType(TaskType.REPAIR)
                .status(TaskStatus.COMPLETED)
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("1000000"))
                .build()));
        when(assignmentRepository.findByTaskId(70L)).thenReturn(List.of(assignment(1L, AssignmentRole.MAIN)));
        when(employeeRepository.findAllById(anyCollection())).thenReturn(List.of(employee(1L, "50000", "0")));
        when(vehicleCheckRepository.findAllByRepairOrderIdAndCheckTypeOrderByAttemptNoAsc(7L, CheckType.QC))
                .thenReturn(List.of(qcCheck(CheckResult.FAILED), qcCheck(CheckResult.PASSED)));

        service.recordOrderCommissions(1L, 7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderCommission>> captor = ArgumentCaptor.forClass(List.class);
        verify(commissionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(OrderCommission::getCommissionType)
                .containsExactly(CommissionType.LABOR_SHARE);
        assertThat(captor.getValue().get(0).getCommissionAmount()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Thưởng đơn theo bậc trong ca: đơn thứ 3 trong cùng ca được 80.000, thợ làm một mình hưởng 100%")
    void orderBonusUsesTheTierOfThePositionInTheShift() {
        RepairOrder order = closedOrder(1L);
        when(repairOrderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(commissionRepository.existsByRepairOrderId(7L)).thenReturn(false);
        when(repairTaskRepository.findByRepairOrderId(7L)).thenReturn(List.of(RepairTask.builder()
                .id(70L).taskType(TaskType.REPAIR).status(TaskStatus.COMPLETED)
                .quantity(BigDecimal.ONE).unitPrice(new BigDecimal("1000000")).build()));
        when(assignmentRepository.findByTaskId(70L)).thenReturn(List.of(assignment(1L, AssignmentRole.MAIN)));
        when(employeeRepository.findAllById(anyCollection())).thenReturn(List.of(employee(1L, "50000", "0")));
        when(vehicleCheckRepository.findAllByRepairOrderIdAndCheckTypeOrderByAttemptNoAsc(7L, CheckType.QC))
                .thenReturn(List.of(qcCheck(CheckResult.PASSED)));
        com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift shift =
                com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift.builder()
                        .id(40L).employeeId(1L)
                        .shiftType(com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType.MORNING)
                        .serviceBayId(2L).build();
        when(shiftRepository.findActiveAt(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(List.of(shift));
        when(commissionRepository.countByEmployeeIdAndShiftIdAndCommissionTypeAndStatusNot(
                1L, 40L, CommissionType.ORDER_COMPLETION, CommissionStatus.CANCELLED)).thenReturn(2L);
        com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay bay =
                com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay.builder().id(2L).bayCode("BAY-02").build();
        when(serviceBayRepository.findById(2L)).thenReturn(Optional.of(bay));
        com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle vehicle = new com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle();
        vehicle.setLicensePlate("30A-123.45");
        order.setVehicleId(9L);
        when(vehicleRepository.findById(9L)).thenReturn(Optional.of(vehicle));

        service.recordOrderCommissions(1L, 7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderCommission>> captor = ArgumentCaptor.forClass(List.class);
        verify(commissionRepository).saveAll(captor.capture());
        OrderCommission bonus = captor.getValue().stream()
                .filter(row -> row.getCommissionType() == CommissionType.ORDER_COMPLETION).findFirst().orElseThrow();
        assertThat(bonus.getCommissionAmount()).isEqualByComparingTo("80000");
        assertThat(bonus.getShiftId()).isEqualTo(40L);
        assertThat(bonus.getBayCode()).isEqualTo("BAY-02");
        assertThat(bonus.getLicensePlate()).isEqualTo("30A-123.45");
        assertThat(bonus.getNotes()).isEqualTo("Thưởng đơn xe 30A-123.45 ca Sáng tại khoang BAY-02");
    }

    @Test
    @DisplayName("Thưởng tiết kiệm giờ: 50% giá trị giờ dôi so với định mức khi QC đạt lần 1")
    void timeSavedBonusPaysHalfTheSavedBookHours() {
        RepairOrder order = closedOrder(1L);
        when(repairOrderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(commissionRepository.existsByRepairOrderId(7L)).thenReturn(false);
        when(repairTaskRepository.findByRepairOrderId(7L)).thenReturn(List.of(RepairTask.builder()
                .id(70L).serviceId(5L).taskType(TaskType.REPAIR).status(TaskStatus.COMPLETED)
                .quantity(BigDecimal.ONE).unitPrice(new BigDecimal("1000000")).build()));
        when(assignmentRepository.findByTaskId(70L)).thenReturn(List.of(
                TaskAssignment.builder().id(9L).taskId(70L).employeeId(1L).assignmentRole(AssignmentRole.MAIN).build()));
        when(employeeRepository.findAllById(anyCollection())).thenReturn(List.of(employee(1L, "50000", "0")));
        when(vehicleCheckRepository.findAllByRepairOrderIdAndCheckTypeOrderByAttemptNoAsc(7L, CheckType.QC))
                .thenReturn(List.of(qcCheck(CheckResult.PASSED)));
        com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity service =
                new com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity();
        service.setStandardMinutes(120);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(laborSessionRepository.findByAssignmentIdIn(anyCollection())).thenReturn(List.of(LaborSession.builder()
                .assignmentId(9L).employeeId(1L)
                .startsAt(Instant.parse("2026-10-05T02:00:00Z")).endsAt(Instant.parse("2026-10-05T03:00:00Z")).build()));

        service(7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderCommission>> captor = ArgumentCaptor.forClass(List.class);
        verify(commissionRepository).saveAll(captor.capture());
        // 2h book - 1h actual = 1h saved x 50.000 x 50% = 25.000
        assertThat(amountOf(captor.getValue(), 1L, CommissionType.TIME_SAVED_EFFICIENCY)).isEqualByComparingTo("25000");
    }

    private void service(Long orderId) {
        this.service.recordOrderCommissions(1L, orderId);
    }

    @Test
    @DisplayName("Ghi nhận hoa hồng idempotent và bỏ qua lệnh bảo hành")
    void commissionsAreIdempotentAndSkipWarrantyOrders() {
        when(repairOrderRepository.findById(7L)).thenReturn(Optional.of(closedOrder(1L)));
        when(commissionRepository.existsByRepairOrderId(7L)).thenReturn(true);
        assertThat(service.recordOrderCommissions(1L, 7L)).isZero();

        RepairOrder warranty = closedOrder(1L);
        warranty.setOrderType(OrderType.WARRANTY);
        when(repairOrderRepository.findById(8L)).thenReturn(Optional.of(warranty));
        assertThat(service.recordOrderCommissions(1L, 8L)).isZero();

        verify(commissionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Tính lương: tách giờ thường/tăng ca, cộng thưởng đơn và lương cứng")
    void calculatePeriodSplitsOvertimeAndAddsCommissionAndBaseSalary() {
        PayrollPeriod period = PayrollPeriod.builder()
                .id(5L)
                .periodCode("PR-2026-10")
                .periodMonth("2026-10")
                .fromDate(LocalDate.of(2026, 10, 1))
                .toDate(LocalDate.of(2026, 10, 31))
                .status(PayrollPeriodStatus.DRAFT)
                .build();
        Employee worker = employee(1L, "50000", "5000000");
        // 16:00-18:00 giờ Việt Nam: 1 giờ trong khung chuẩn, 1 giờ tăng ca.
        LaborSession session = LaborSession.builder()
                .assignmentId(1L)
                .employeeId(1L)
                .startsAt(Instant.parse("2026-10-05T09:00:00Z"))
                .endsAt(Instant.parse("2026-10-05T11:00:00Z"))
                .hourlyCostSnapshot(new BigDecimal("50000"))
                .build();
        OrderCommission commission = OrderCommission.builder()
                .id(9L)
                .repairOrderId(7L)
                .employeeId(1L)
                .commissionType(CommissionType.LABOR_SHARE)
                .commissionAmount(new BigDecimal("200000"))
                .status(CommissionStatus.APPROVED)
                .earnedAt(Instant.parse("2026-10-20T03:00:00Z"))
                .build();
        List<PayrollItem> stored = new ArrayList<>();

        when(periodRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(period));
        when(laborSessionRepository.findClosedBetween(any(), any())).thenReturn(List.of(session));
        when(commissionRepository.findUnlinked(eq(CommissionStatus.APPROVED), any(), any()))
                .thenReturn(List.of(commission));
        when(employeeRepository.findByIsActiveTrue()).thenReturn(List.of(worker));
        when(employeeRepository.findAllById(anyCollection())).thenReturn(List.of(worker));
        when(shiftRepository.countByEmployeeIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                anyLong(), any(), any(), any())).thenReturn(2L);
        lenient().when(itemRepository.findByPayrollPeriodIdOrderByEmployeeIdAsc(5L)).thenAnswer(call -> stored);
        when(itemRepository.save(any(PayrollItem.class))).thenAnswer(call -> {
            PayrollItem item = call.getArgument(0);
            item.setId(11L);
            if (!stored.contains(item)) {
                stored.add(item);
            }
            return item;
        });
        lenient().when(commissionRepository.countDistinctOrdersByPeriod(5L)).thenReturn(1L);
        lenient().when(periodRepository.save(any(PayrollPeriod.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(userRepository.findAllById(anyCollection())).thenReturn(List.of());

        PayrollPeriodDetailResponse result = service.calculatePeriod(1L, 5L);

        PayrollItemResponse item = result.items().get(0);
        assertThat(item.regularHours()).isEqualByComparingTo("1.00");
        assertThat(item.overtimeHours()).isEqualByComparingTo("1.00");
        assertThat(item.hourlyPayAmount()).isEqualByComparingTo("125000");
        assertThat(item.commissionAmount()).isEqualByComparingTo("200000");
        assertThat(item.shiftCount()).isEqualTo(2);
        assertThat(item.netPay()).isEqualByComparingTo("5325000");
        assertThat(commission.getPayrollItemId()).isEqualTo(11L);
        assertThat(result.period().status()).isEqualTo(PayrollPeriodStatus.CALCULATED);
        assertThat(result.period().totalNetPay()).isEqualByComparingTo("5325000");
    }

    @Test
    @DisplayName("Bảng lương đã duyệt không được tính lại")
    void approvedPeriodCannotBeRecalculated() {
        PayrollPeriod period = PayrollPeriod.builder().id(5L).status(PayrollPeriodStatus.APPROVED).build();
        when(periodRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.calculatePeriod(1L, 5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Chi trả lương ghi phiếu chi đúng tổng thực chi và đánh dấu kỳ đã chi")
    void disbursementRecordsPaymentAndMarksPeriodPaid() {
        PayrollPeriod period = PayrollPeriod.builder()
                .id(5L)
                .periodCode("PR-2026-10")
                .status(PayrollPeriodStatus.APPROVED)
                .totalNetPay(new BigDecimal("5325000"))
                .build();
        when(periodRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(period));
        when(billingOperations.recordPayrollDisbursement(
                        eq(1L), eq(5L), eq(new BigDecimal("5325000")), eq("Chi trả lương kỳ PR-2026-10")))
                .thenReturn(99L);
        when(itemRepository.findByPayrollPeriodIdOrderByEmployeeIdAsc(5L)).thenReturn(List.of());
        when(periodRepository.save(any(PayrollPeriod.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.disbursePeriod(1L, 5L);

        assertThat(response.status()).isEqualTo(PayrollPeriodStatus.PAID);
        assertThat(response.paymentId()).isEqualTo(99L);
        verify(commissionRepository)
                .markPaidForPeriod(5L, CommissionStatus.APPROVED, CommissionStatus.PAID_OUT);
    }

    private static BigDecimal amountOf(List<OrderCommission> rows, Long employeeId, CommissionType type) {
        return rows.stream()
                .filter(row -> row.getEmployeeId().equals(employeeId) && row.getCommissionType() == type)
                .map(OrderCommission::getCommissionAmount)
                .findFirst()
                .orElseThrow();
    }
}
