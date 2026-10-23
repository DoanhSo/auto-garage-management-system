package com.Group13_65CNTT.AutoGarage.repair;

import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.DiagnosisUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairOrderCreateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairOrderUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairTaskCreateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairTaskUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairOrderResponse;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairTaskResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.*;
import com.Group13_65CNTT.AutoGarage.repair.mapper.RepairOrderMapper;
import com.Group13_65CNTT.AutoGarage.repair.mapper.RepairTaskMapper;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.service.impl.RepairServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BayScheduling;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyClaimRepository;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairOrderServiceTest {

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

    @InjectMocks
    private RepairServiceImpl repairService;

    private Customer mockCustomer;
    private Vehicle mockVehicle;
    private Booking mockBooking;
    private RepairOrder mockOrder;
    private RepairTask mockTask;

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setFullName("Nguyen Van A");
        mockCustomer.setIsActive(true);

        mockVehicle = new Vehicle();
        mockVehicle.setId(1L);
        mockVehicle.setLicensePlate("29A-12345");
        mockVehicle.setOdometer(50000);
        mockVehicle.setIsActive(true);

        mockBooking = new Booking();
        mockBooking.setId(1L);
        mockBooking.setCustomerId(1L);
        mockBooking.setVehicleId(1L);
        mockBooking.setStatus(BookingStatus.CONFIRMED);

        mockOrder = RepairOrder.builder()
                .id(100L)
                .orderCode("RO-20261023-00001")
                .customerId(1L)
                .vehicleId(1L)
                .orderType(OrderType.NORMAL)
                .status(RepairOrderStatus.NEW)
                .odometerIn(52000)
                .receivedBy(2L)
                .receivedAt(Instant.now())
                .build();

        mockTask = RepairTask.builder()
                .id(200L)
                .repairOrderId(100L)
                .taskType(TaskType.DIAGNOSIS)
                .taskName("Khám xe và chẩn đoán ban đầu")
                .status(TaskStatus.PENDING)
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Tạo phiếu sửa chữa thành công kèm task chẩn đoán tự động")
    void createRepairOrder_Success() {
        RepairOrderCreateRequest request = RepairOrderCreateRequest.builder()
                .customerId(1L)
                .vehicleId(1L)
                .bookingId(1L)
                .odometerIn(52000)
                .fuelLevelPercent(new BigDecimal("75.50"))
                .customerComplaint("Tiếng kêu lạ ở phanh")
                .build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(mockVehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(repairOrderRepository.existsByBookingId(1L)).thenReturn(false);
        when(repairOrderRepository.existsByOrderCode(any())).thenReturn(false);
        when(repairOrderRepository.save(any(RepairOrder.class))).thenReturn(mockOrder);
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));

        RepairOrderResponse responseDto = RepairOrderResponse.builder()
                .id(100L)
                .orderCode("RO-20261023-00001")
                .status(RepairOrderStatus.NEW)
                .odometerIn(52000)
                .build();
        when(repairOrderMapper.toResponse(any(RepairOrder.class))).thenReturn(responseDto);

        RepairOrderResponse result = repairService.createRepairOrder(2L, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getOrderCode()).isEqualTo("RO-20261023-00001");
        verify(repairTaskRepository).save(any(RepairTask.class));
        verify(bookingRepository).save(mockBooking);
        assertThat(mockBooking.getStatus()).isEqualTo(BookingStatus.RECEIVED);
    }

    @Test
    @DisplayName("Tạo phiếu thất bại khi odometer âm")
    void createRepairOrder_NegativeOdometer_ThrowsException() {
        RepairOrderCreateRequest request = RepairOrderCreateRequest.builder()
                .customerId(1L)
                .vehicleId(1L)
                .odometerIn(-10)
                .build();

        assertThatThrownBy(() -> repairService.createRepairOrder(2L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Số km khi vào xưởng phải >= 0");
    }

    @Test
    @DisplayName("Tạo phiếu thất bại khi xe không tồn tại")
    void createRepairOrder_VehicleNotFound_ThrowsException() {
        RepairOrderCreateRequest request = RepairOrderCreateRequest.builder()
                .customerId(1L)
                .vehicleId(999L)
                .odometerIn(1000)
                .build();

        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repairService.createRepairOrder(2L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tạo phiếu thất bại khi lịch hẹn đã có phiếu sửa chữa gắn với nó")
    void createRepairOrder_BookingAlreadyReceived_ThrowsConflict() {
        RepairOrderCreateRequest request = RepairOrderCreateRequest.builder()
                .customerId(1L)
                .vehicleId(1L)
                .bookingId(1L)
                .odometerIn(52000)
                .build();

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(mockVehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(repairOrderRepository.existsByBookingId(1L)).thenReturn(true);

        assertThatThrownBy(() -> repairService.createRepairOrder(2L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Lịch hẹn này đã được tiếp nhận tạo phiếu sửa chữa trước đó");
    }

    @Test
    @DisplayName("Lấy chi tiết phiếu theo ID")
    void getRepairOrderById_Success() {
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        RepairOrderResponse responseDto = RepairOrderResponse.builder().id(100L).build();
        when(repairOrderMapper.toResponse(mockOrder)).thenReturn(responseDto);

        RepairOrderResponse response = repairService.getRepairOrderById(100L);
        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Cập nhật kết quả chẩn đoán chuyển status từ NEW sang DIAGNOSING")
    void updateDiagnosis_Success() {
        DiagnosisUpdateRequest req = DiagnosisUpdateRequest.builder()
                .diagnosis("Phát hiện mòn má phanh trước bên trái")
                .build();

        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(repairOrderRepository.save(mockOrder)).thenReturn(mockOrder);
        RepairOrderResponse res = RepairOrderResponse.builder()
                .id(100L)
                .diagnosis(req.getDiagnosis())
                .status(RepairOrderStatus.DIAGNOSING)
                .build();
        when(repairOrderMapper.toResponse(mockOrder)).thenReturn(res);

        RepairOrderResponse result = repairService.updateDiagnosis(2L, 100L, req);

        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.DIAGNOSING);
        assertThat(mockOrder.getDiagnosis()).isEqualTo(req.getDiagnosis());
        assertThat(result.getStatus()).isEqualTo(RepairOrderStatus.DIAGNOSING);
    }

    @Test
    @DisplayName("Chuyển trạng thái phiếu hợp lệ theo State Machine")
    void transitionStatus_Valid() {
        mockOrder.setStatus(RepairOrderStatus.NEW);
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(repairOrderRepository.save(mockOrder)).thenReturn(mockOrder);
        RepairOrderResponse res = RepairOrderResponse.builder().id(100L).status(RepairOrderStatus.DIAGNOSING).build();
        when(repairOrderMapper.toResponse(mockOrder)).thenReturn(res);

        RepairOrderResponse result = repairService.transitionStatus(2L, 100L, RepairOrderStatus.DIAGNOSING, "Bắt đầu khám xe");
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.DIAGNOSING);
    }

    @Test
    @DisplayName("Chuyển trạng thái phiếu không hợp lệ bị từ chối")
    void transitionStatus_Invalid_ThrowsException() {
        mockOrder.setStatus(RepairOrderStatus.NEW);
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> repairService.transitionStatus(2L, 100L, RepairOrderStatus.COMPLETED, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể chuyển trạng thái phiếu từ NEW sang COMPLETED");
    }

    @Test
    @DisplayName("Thêm task vào phiếu sửa chữa")
    void createTask_Success() {
        RepairTaskCreateRequest req = RepairTaskCreateRequest.builder()
                .taskName("Thay má phanh trước")
                .description("Thay má phanh gốm cao cấp")
                .quantity(new BigDecimal("1.000"))
                .unitPrice(new BigDecimal("500000.00"))
                .build();

        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskMapper.toEntity(req)).thenReturn(mockTask);
        when(repairTaskRepository.save(any(RepairTask.class))).thenReturn(mockTask);
        RepairTaskResponse res = RepairTaskResponse.builder().id(200L).taskName(req.getTaskName()).build();
        when(repairTaskMapper.toResponse(mockTask)).thenReturn(res);

        RepairTaskResponse created = repairService.createTask(2L, 100L, req);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Chặn xóa task đang thực hiện")
    void deleteTask_InProgress_ThrowsException() {
        mockTask.setStatus(TaskStatus.IN_PROGRESS);
        when(repairTaskRepository.findById(200L)).thenReturn(Optional.of(mockTask));
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> repairService.deleteTask(2L, 200L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể xóa công việc đang thực hiện hoặc đã hoàn thành");
    }
}
