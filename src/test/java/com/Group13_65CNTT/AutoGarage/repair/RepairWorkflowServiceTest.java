package com.Group13_65CNTT.AutoGarage.repair;

import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.reception.entity.*;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.*;
import com.Group13_65CNTT.AutoGarage.repair.mapper.RepairOrderMapper;
import com.Group13_65CNTT.AutoGarage.repair.mapper.RepairTaskMapper;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.service.impl.RepairServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BayScheduling;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations;
import com.Group13_65CNTT.AutoGarage.shared.contracts.InventoryOperations;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.warranty.entity.ClaimStatus;
import com.Group13_65CNTT.AutoGarage.warranty.entity.Warranty;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyClaim;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyClaimRepository;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairWorkflowServiceTest {

    @Mock
    private RepairOrderRepository repairOrderRepository;
    @Mock
    private RepairTaskRepository repairTaskRepository;
    @Mock
    private LaborSessionRepository laborSessionRepository;
    @Mock
    private RepairOrderMapper repairOrderMapper;
    @Mock
    private RepairTaskMapper repairTaskMapper;
    @Mock
    private PaginationPolicy pagination;

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private VehicleCheckRepository vehicleCheckRepository;
    @Mock
    private WarrantyClaimRepository warrantyClaimRepository;
    @Mock
    private WarrantyRepository warrantyRepository;
    @Mock
    private BayScheduling bayScheduling;
    @Mock
    private BillingOperations billingOperations;
    @Mock
    private InventoryOperations inventoryOperations;
    @Mock
    private NotificationAudit notificationAudit;

    @InjectMocks
    private RepairServiceImpl repairService;

    private Customer mockCustomer;
    private Vehicle mockVehicle;
    private Booking mockBooking;
    private RepairOrder mockOrder;

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setFullName("Tran Van B");
        mockCustomer.setIsActive(true);

        mockVehicle = new Vehicle();
        mockVehicle.setId(2L);
        mockVehicle.setLicensePlate("30E-99999");
        mockVehicle.setOdometer(20000);
        mockVehicle.setIsActive(true);

        mockBooking = new Booking();
        mockBooking.setId(3L);
        mockBooking.setCustomerId(1L);
        mockBooking.setVehicleId(2L);
        mockBooking.setStatus(BookingStatus.CONFIRMED);

        mockOrder = RepairOrder.builder()
                .id(50L)
                .orderCode("RO-20261023-11111")
                .customerId(1L)
                .vehicleId(2L)
                .orderType(OrderType.NORMAL)
                .status(RepairOrderStatus.IN_PROGRESS)
                .odometerIn(20500)
                .receivedBy(10L)
                .latestQcCheckId(300L)
                .build();

        ReflectionTestUtils.setField(repairService, "billingOperations", billingOperations);
        ReflectionTestUtils.setField(repairService, "inventoryOperations", inventoryOperations);
        ReflectionTestUtils.setField(repairService, "notificationAudit", notificationAudit);
    }

    // =========================================================================
    // I03.receive
    // =========================================================================

    @Test
    @DisplayName("I03.receive: Tiếp nhận xe thành công và tạo task chẩn đoán ban đầu")
    void receive_Success() {
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(mockVehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(mockBooking));
        when(repairOrderRepository.existsByBookingId(3L)).thenReturn(false);
        when(repairOrderRepository.save(any(RepairOrder.class))).thenAnswer(inv -> {
            RepairOrder ro = inv.getArgument(0);
            ro.setId(50L);
            return ro;
        });

        long orderId = repairService.receive(10L, 3L, 1L, 2L, 20500);

        assertThat(orderId).isEqualTo(50L);
        verify(repairTaskRepository).save(argThat(task ->
                task.getTaskType() == TaskType.DIAGNOSIS &&
                task.getRepairOrderId() == 50L &&
                task.getStatus() == TaskStatus.PENDING
        ));
        assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.RECEIVED);
    }

    // =========================================================================
    // I03.createWarrantyOrder
    // =========================================================================

    @Test
    @DisplayName("I03.createWarrantyOrder: Tạo phiếu loại WARRANTY từ yêu cầu bảo hành hợp lệ")
    void createWarrantyOrder_Success() {
        Warranty mockWarranty = Warranty.builder()
                .id(10L)
                .customerId(1L)
                .vehicleId(2L)
                .build();

        WarrantyClaim mockClaim = WarrantyClaim.builder()
                .id(20L)
                .warrantyId(10L)
                .currentOdometer(21000)
                .description("Hỏng thước lái sau bảo dưỡng")
                .status(ClaimStatus.APPROVED)
                .handlingOrderId(null)
                .build();

        when(warrantyClaimRepository.findById(20L)).thenReturn(Optional.of(mockClaim));
        when(warrantyRepository.findById(10L)).thenReturn(Optional.of(mockWarranty));
        when(repairOrderRepository.save(any(RepairOrder.class))).thenAnswer(inv -> {
            RepairOrder ro = inv.getArgument(0);
            ro.setId(88L);
            return ro;
        });

        long newOrderId = repairService.createWarrantyOrder(10L, 20L);

        assertThat(newOrderId).isEqualTo(88L);
        assertThat(mockClaim.getHandlingOrderId()).isEqualTo(88L);
        assertThat(mockClaim.getStatus()).isEqualTo(ClaimStatus.IN_PROGRESS);
        verify(warrantyClaimRepository).save(mockClaim);
        verify(repairTaskRepository).save(argThat(task -> task.getTaskType() == TaskType.DIAGNOSIS));
    }

    @Test
    @DisplayName("I03.createWarrantyOrder: Bị từ chối nếu yêu cầu bảo hành đã có phiếu xử lý trước đó")
    void createWarrantyOrder_AlreadyHandled_ThrowsConflict() {
        WarrantyClaim mockClaim = WarrantyClaim.builder()
                .id(20L)
                .warrantyId(10L)
                .handlingOrderId(77L)
                .build();

        when(warrantyClaimRepository.findById(20L)).thenReturn(Optional.of(mockClaim));

        assertThatThrownBy(() -> repairService.createWarrantyOrder(10L, 20L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã được gắn với phiếu sửa chữa xử lý");
    }

    // =========================================================================
    // I03.requireInvoiceReady
    // =========================================================================

    @Test
    @DisplayName("I03.requireInvoiceReady: Hợp lệ khi tất cả tasks hoàn thành, không timer mở, QC PASSED")
    void requireInvoiceReady_Success() {
        when(repairOrderRepository.findById(50L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(50L, TaskStatus.COMPLETED)).thenReturn(false);
        when(laborSessionRepository.countOpenSessionsByRepairOrderId(50L)).thenReturn(0L);

        VehicleCheck qcCheck = new VehicleCheck();
        qcCheck.setId(300L);
        qcCheck.setCheckType(CheckType.QC);
        qcCheck.setResult(CheckResult.PASSED);
        when(vehicleCheckRepository.findById(300L)).thenReturn(Optional.of(qcCheck));

        // Không ném exception nào
        repairService.requireInvoiceReady(10L, 50L);
    }

    @Test
    @DisplayName("I03.requireInvoiceReady: Thất bại khi vẫn còn task chưa COMPLETED")
    void requireInvoiceReady_UncompletedTasks_ThrowsException() {
        when(repairOrderRepository.findById(50L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(50L, TaskStatus.COMPLETED)).thenReturn(true);

        assertThatThrownBy(() -> repairService.requireInvoiceReady(10L, 50L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Tất cả các hạng mục công việc phải hoàn thành");
    }

    @Test
    @DisplayName("I03.requireInvoiceReady: Thất bại khi vẫn còn timer đang chạy")
    void requireInvoiceReady_OpenTimer_ThrowsException() {
        when(repairOrderRepository.findById(50L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(50L, TaskStatus.COMPLETED)).thenReturn(false);
        when(laborSessionRepository.countOpenSessionsByRepairOrderId(50L)).thenReturn(1L);

        assertThatThrownBy(() -> repairService.requireInvoiceReady(10L, 50L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Vẫn còn phiên làm việc (timer) của kỹ thuật viên chưa kết thúc");
    }

    @Test
    @DisplayName("I03.requireInvoiceReady: Thất bại khi QC FAILED")
    void requireInvoiceReady_QcFailed_ThrowsException() {
        when(repairOrderRepository.findById(50L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(50L, TaskStatus.COMPLETED)).thenReturn(false);
        when(laborSessionRepository.countOpenSessionsByRepairOrderId(50L)).thenReturn(0L);

        VehicleCheck qcCheck = new VehicleCheck();
        qcCheck.setId(300L);
        qcCheck.setCheckType(CheckType.QC);
        qcCheck.setResult(CheckResult.FAILED);
        when(vehicleCheckRepository.findById(300L)).thenReturn(Optional.of(qcCheck));

        assertThatThrownBy(() -> repairService.requireInvoiceReady(10L, 50L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Biên bản kiểm tra chất lượng gần nhất chưa đạt yêu cầu (PASSED)");
    }

    // =========================================================================
    // I03.handOver
    // =========================================================================

    @Test
    void handOverBayReleaseFailurePropagatesAndDoesNotRecordCompletion() {
        when(repairOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(mockOrder));
        var failure = new org.springframework.dao.DataAccessResourceFailureException("private database details");
        doThrow(failure).when(bayScheduling).releaseForRepair(10L, 50L);

        assertThatThrownBy(() -> repairService.handOver(10L, 50L, 20600, "KEY"))
                .isSameAs(failure);

        verify(inventoryOperations, never()).releaseForOrder(anyLong(), anyLong());
        verify(vehicleCheckRepository, never()).save(any());
        verify(notificationAudit, never()).recordCompletedRequest(anyLong(), any(), any(), anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("I03.handOver: Bàn giao xe hợp lệ, đóng phiếu, giải phóng khoang, kho, lưu biên bản")
    void handOver_Success() {
        when(repairOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(mockOrder));
        when(vehicleRepository.findById(2L)).thenReturn(Optional.of(mockVehicle));

        repairService.handOver(10L, 50L, 20600, "REQ-KEY-123");

        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.CLOSED);
        assertThat(mockOrder.getOdometerOut()).isEqualTo(20600);
        assertThat(mockOrder.getHandedOverBy()).isEqualTo(10L);
        assertThat(mockOrder.getClosedAt()).isNotNull();

        verify(billingOperations).requireHandoverAllowed(10L, 50L);
        verify(bayScheduling).releaseForRepair(10L, 50L);
        verify(inventoryOperations).releaseForOrder(10L, 50L);
        verify(vehicleCheckRepository).save(argThat(c -> c.getCheckType() == CheckType.HANDOVER && c.getOdometer() == 20600));
        verify(notificationAudit).recordCompletedRequest(eq(10L), eq("HANDOVER"), eq("REPAIR_ORDER"), eq(50L), any(), any(), any());
    }

    @Test
    @DisplayName("I03.handOver: Thất bại khi odometer bàn giao nhỏ hơn odometer vào xưởng")
    void handOver_OdometerLessThanIn_ThrowsException() {
        when(repairOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> repairService.handOver(10L, 50L, 20400, "REQ-KEY-123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không được nhỏ hơn số km vào xưởng");
    }
}
