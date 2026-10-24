package com.Group13_65CNTT.AutoGarage.vehicle;

import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.TransferOwnershipRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.UpdateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.mapper.VehicleMapper;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.service.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {
    @Mock private com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Spy
    private VehicleMapper vehicleMapper = new VehicleMapper();

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Customer owner;
    private CreateVehicleRequest createReq;

    @BeforeEach
    void setUp() {
        owner = new Customer();
        owner.setId(10L);
        owner.setFullName("Tran Van Chu Xe");
        owner.setPhone("0912345678");

        createReq = new CreateVehicleRequest();
        createReq.setCustomerId(10L);
        createReq.setLicensePlate("51F-123.45");
        createReq.setBrand("Toyota");
        createReq.setModel("Corolla Cross");
        createReq.setOdometer(15000);
    }

    @Test
    void customerVehicleSearchBindsOwnScope() {
        when(customerScope.customerIdForQuery(null)).thenReturn(10L);
        when(vehicleRepository.searchVehicles(10L, null, true)).thenReturn(java.util.List.of());
        assertTrue(vehicleService.searchVehicles(null, null, true).isEmpty());
        verify(vehicleRepository).searchVehicles(10L, null, true);
    }

    @Test
    void forbiddenVehicleIsNotReturned() {
        doThrow(new com.Group13_65CNTT.AutoGarage.common.exception.BusinessException(
                com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.FORBIDDEN))
                .when(customerScope).requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.VEHICLE, 100L);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(100L);
        vehicle.setCustomerId(10L);
        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(vehicle));
        assertThrows(com.Group13_65CNTT.AutoGarage.common.exception.BusinessException.class,
                () -> vehicleService.getVehicleById(100L));
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("TV2-02: Tạo phương tiện thành công và chuẩn hóa biển số")
    void testCreateVehicle_Success() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(vehicleRepository.existsByLicensePlate("51F12345")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v = invocation.getArgument(0);
            v.setId(100L);
            return v;
        });

        VehicleResponse response = vehicleService.createVehicle(createReq);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("51F12345", response.getLicensePlate());
        assertEquals("Tran Van Chu Xe", response.getCustomerName());
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("TV2-02: Báo lỗi khi biển số xe đã tồn tại")
    void testCreateVehicle_DuplicatePlate() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(vehicleRepository.existsByLicensePlate("51F12345")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> vehicleService.createVehicle(createReq));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-02: Chặn cập nhật ODO giảm so với số hiện tại")
    void testUpdateVehicle_OdometerCannotDecrease() {
        Vehicle existing = new Vehicle();
        existing.setId(100L);
        existing.setCustomerId(10L);
        existing.setLicensePlate("51F12345");
        existing.setBrand("Toyota");
        existing.setModel("Corolla Cross");
        existing.setOdometer(20000);

        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(existing));

        UpdateVehicleRequest updateReq = new UpdateVehicleRequest();
        updateReq.setLicensePlate("51F12345");
        updateReq.setBrand("Toyota");
        updateReq.setModel("Corolla Cross");
        updateReq.setOdometer(18000); // Giảm từ 20000 xuống 18000

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> vehicleService.updateVehicle(100L, updateReq));
        assertTrue(ex.getMessage().contains("không được nhỏ hơn"));
    }

    @Test
    @DisplayName("TV2-03: Đổi chủ xe thành công sang khách hàng mới")
    void testTransferOwnership_Success() {
        Vehicle existing = new Vehicle();
        existing.setId(100L);
        existing.setCustomerId(10L);
        existing.setLicensePlate("51F12345");

        Customer newOwner = new Customer();
        newOwner.setId(20L);
        newOwner.setFullName("Le Thi Chu Moi");
        newOwner.setPhone("0988888888");

        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(20L)).thenReturn(Optional.of(newOwner));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferOwnershipRequest transferReq = new TransferOwnershipRequest();
        transferReq.setNewCustomerId(20L);
        transferReq.setTransferNote("Mua lại xe");

        VehicleResponse response = vehicleService.transferOwnership(100L, transferReq);

        assertNotNull(response);
        assertEquals(20L, response.getCustomerId());
        assertEquals("Le Thi Chu Moi", response.getCustomerName());
        verify(vehicleRepository, times(1)).save(existing);
        assertEquals(20L, existing.getCustomerId());
    }

    @Test
    @DisplayName("TV2-03: Từ chối đổi chủ xe khi chủ mới trùng với chủ hiện tại")
    void testTransferOwnership_SameCustomer() {
        Vehicle existing = new Vehicle();
        existing.setId(100L);
        existing.setCustomerId(10L);

        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(owner));

        TransferOwnershipRequest transferReq = new TransferOwnershipRequest();
        transferReq.setNewCustomerId(10L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> vehicleService.transferOwnership(100L, transferReq));
        assertTrue(ex.getMessage().contains("hiện đã thuộc về khách hàng"));
    }

    @Test
    @DisplayName("TV2-09: Tra cứu timeline phương tiện thành công với vai trò nhân viên")
    void testGetVehicleTimeline_StaffRole() {
        Vehicle existing = new Vehicle();
        existing.setId(100L);
        existing.setCustomerId(10L);
        existing.setLicensePlate("51F12345");
        existing.setBrand("Toyota");
        existing.setModel("Corolla Cross");
        existing.setOdometer(20000);

        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(owner));

        com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse timeline =
                vehicleService.getVehicleTimeline(100L, null, "STAFF");

        assertNotNull(timeline);
        assertEquals(100L, timeline.getVehicleId());
        assertEquals("51F12345", timeline.getLicensePlate());
        assertEquals("Tran Van Chu Xe", timeline.getCurrentCustomerName());
    }

    @Test
    @DisplayName("TV2-09: Chặn khách hàng xem lịch sử xe của người khác (BR02)")
    void testGetVehicleTimeline_DenyOtherCustomer() {
        Vehicle existing = new Vehicle();
        existing.setId(100L);
        existing.setCustomerId(10L);

        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(existing));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                vehicleService.getVehicleTimeline(100L, 999L, "CUSTOMER"));

        assertTrue(ex.getMessage().contains("không có quyền truy cập"));
    }
}
