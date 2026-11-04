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
    private com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateRepository estimateRepository;
    @Mock
    private com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateItemRepository estimateItemRepository;
    @Mock
    private BayScheduling bayScheduling;

    @Mock
    private com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;
    @Mock
    private com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService objectAccess;
    @Mock
    private com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider currentUser;

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
        mockVehicle.setCustomerId(1L);
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
    @DisplayName("Không tạo task sửa chữa ngoài báo giá đã duyệt")
    void createTask_RequiresApprovedEstimate() {
        RepairTaskCreateRequest req = RepairTaskCreateRequest.builder()
                .taskName("Thay má phanh trước")
                .description("Thay má phanh gốm cao cấp")
                .quantity(new BigDecimal("1.000"))
                .unitPrice(new BigDecimal("500000.00"))
                .build();

        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        assertThatThrownBy(() -> repairService.createTask(2L, 100L, req)).isInstanceOf(BusinessRuleException.class);
        verify(repairTaskRepository, never()).save(any());
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

    @Test
    @DisplayName("Khách hàng chỉ tìm thấy phiếu sửa chữa của chính mình")
    void searchRepairOrders_Customer_ScopedToOwnCustomerId() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(pagination.page(anyInt(), anyInt(), anyString(), anyString(), anySet())).thenReturn((org.springframework.data.domain.PageRequest) pageable);
        when(currentUser.role()).thenReturn(com.Group13_65CNTT.AutoGarage.common.enums.Role.CUSTOMER);
        when(customerScope.currentCustomerId()).thenReturn(5L);
        when(repairOrderRepository.searchOrders(5L, null, null, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(mockOrder)));

        assertThat(repairService.searchRepairOrders(null, null, 0, 10, "createdAt", "desc").getContent()).hasSize(1);

        verify(repairOrderRepository).searchOrders(5L, null, null, pageable);
        verify(repairOrderRepository, never()).searchAssignedOrders(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("Nhân viên lễ tân xem toàn bộ phiếu, không bị giới hạn theo khách")
    void searchRepairOrders_Staff_NotScopedByCustomer() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(pagination.page(anyInt(), anyInt(), anyString(), anyString(), anySet())).thenReturn((org.springframework.data.domain.PageRequest) pageable);
        when(currentUser.role()).thenReturn(com.Group13_65CNTT.AutoGarage.common.enums.Role.RECEPTIONIST);
        when(repairOrderRepository.searchOrders(null, null, null, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(mockOrder)));

        assertThat(repairService.searchRepairOrders(null, null, 0, 10, "createdAt", "desc").getContent()).hasSize(1);

        verify(customerScope, never()).currentCustomerId();
    }

    @Test
    @DisplayName("Chẩn đoán đã chốt khi phiếu chờ khách duyệt báo giá")
    void updateDiagnosis_AfterEstimate_Rejected() {
        mockOrder.setStatus(RepairOrderStatus.WAITING_APPROVAL);
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> repairService.updateDiagnosis(2L, 100L,
                DiagnosisUpdateRequest.builder().diagnosis("Sửa lại").build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Chẩn đoán đã chốt");
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thông tin tiếp nhận không sửa được sau khi bắt đầu chẩn đoán")
    void updateRepairOrder_ReceptionAfterNew_Rejected() {
        mockOrder.setStatus(RepairOrderStatus.DIAGNOSING);
        when(repairOrderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> repairService.updateRepairOrder(2L, 100L,
                com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairOrderUpdateRequest.builder()
                        .customerComplaint("Đổi lời khai").build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Thông tin tiếp nhận");
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không chuyển tay sang chờ duyệt khi chưa có báo giá")
    void transitionStatus_ToWaitingApprovalManually_Rejected() {
        mockOrder.setStatus(RepairOrderStatus.DIAGNOSING);
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> repairService.transitionStatus(2L, 100L, RepairOrderStatus.WAITING_APPROVAL, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("lập báo giá");
    }

    @Test
    @DisplayName("Không chuyển sang chờ QC khi còn công việc chưa xong")
    void transitionStatus_ToWaitingQcWithOpenTask_Rejected() {
        mockOrder.setStatus(RepairOrderStatus.IN_PROGRESS);
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        when(repairTaskRepository.existsByRepairOrderIdAndStatusNot(100L, TaskStatus.COMPLETED)).thenReturn(true);

        assertThatThrownBy(() -> repairService.transitionStatus(2L, 100L, RepairOrderStatus.WAITING_QC, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa hoàn thành");
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Không mở lại chẩn đoán khi báo giá còn chờ khách")
    void transitionStatus_ReopenWhileAwaitingCustomer_Rejected() {
        mockOrder.setStatus(RepairOrderStatus.WAITING_APPROVAL);
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        var sent = com.Group13_65CNTT.AutoGarage.estimate.entity.Estimate.builder().id(1L)
                .status(com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus.SENT)
                .validUntil(java.time.Instant.now().plusSeconds(3600)).build();
        when(estimateRepository.findByRepairOrderIdAndStatusIn(eq(100L), anyCollection())).thenReturn(List.of(sent));

        assertThatThrownBy(() -> repairService.transitionStatus(2L, 100L, RepairOrderStatus.DIAGNOSING, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("từ chối toàn bộ");
        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.WAITING_APPROVAL);
    }

    @Test
    @DisplayName("Mở lại chẩn đoán khi khách từ chối hết, hủy các bản nháp")
    void transitionStatus_ReopenAfterFullRejection_CancelsDrafts() {
        mockOrder.setStatus(RepairOrderStatus.WAITING_APPROVAL);
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        var draft = com.Group13_65CNTT.AutoGarage.estimate.entity.Estimate.builder().id(2L)
                .status(com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus.DRAFT).build();
        when(estimateRepository.findByRepairOrderIdAndStatusIn(eq(100L), anyCollection()))
                .thenReturn(List.of())
                .thenReturn(List.of(draft));
        when(repairOrderRepository.save(mockOrder)).thenReturn(mockOrder);

        repairService.transitionStatus(2L, 100L, RepairOrderStatus.DIAGNOSING, null);

        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.DIAGNOSING);
        assertThat(draft.getStatus()).isEqualTo(com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus.CANCELLED);
    }

    @Test
    @DisplayName("Hủy phiếu thì hủy các báo giá còn mở")
    void transitionStatus_Cancel_CancelsOpenEstimates() {
        mockOrder.setStatus(RepairOrderStatus.WAITING_APPROVAL);
        when(repairOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockOrder));
        var sent = com.Group13_65CNTT.AutoGarage.estimate.entity.Estimate.builder().id(1L)
                .status(com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus.SENT).build();
        when(estimateRepository.findByRepairOrderIdAndStatusIn(eq(100L), anyCollection())).thenReturn(List.of(sent));
        when(repairOrderRepository.save(mockOrder)).thenReturn(mockOrder);

        repairService.transitionStatus(2L, 100L, RepairOrderStatus.CANCELLED, "Khách không sửa");

        assertThat(mockOrder.getStatus()).isEqualTo(RepairOrderStatus.CANCELLED);
        assertThat(sent.getStatus()).isEqualTo(com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus.CANCELLED);
    }
}
