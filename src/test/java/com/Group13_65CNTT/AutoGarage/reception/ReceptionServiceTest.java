package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.reception.service.ReceptionServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.contracts.RepairWorkflow;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceptionServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RepairWorkflow repairWorkflow;

    @InjectMocks
    private ReceptionServiceImpl receptionService;

    private Vehicle vehicle;
    private Customer customer;
    private Booking booking;

    @BeforeEach
    void setUp() {
        receptionService.setRepairWorkflow(repairWorkflow);

        customer = new Customer();
        customer.setId(1L);
        customer.setFullName("Nguyen Van A");
        customer.setPhone("0987654321");
        customer.setIsActive(true);

        vehicle = new Vehicle();
        vehicle.setId(10L);
        vehicle.setCustomerId(1L);
        vehicle.setLicensePlate("51F12345");
        vehicle.setBrand("Toyota");
        vehicle.setModel("Vios");
        vehicle.setOdometer(20000);
        vehicle.setIsActive(true);

        booking = new Booking();
        booking.setId(100L);
        booking.setBookingCode("BKG-001");
        booking.setVehicleId(10L);
        booking.setCustomerId(1L);
        booking.setStatus(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("TV2-07: Tiếp nhận xe có lịch hẹn thành công (booking -> RECEIVED, gọi I03)")
    void testCheckInWithBooking_Success() {
        CheckInVehicleRequest req = new CheckInVehicleRequest();
        req.setBookingId(100L);
        req.setVehicleId(10L);
        req.setOdometerIn(20500);
        req.setFuelLevelPercent(BigDecimal.valueOf(50));
        req.setCustomerComplaint("Xe keu o banh truoc");
        req.setActorUserId(2L);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(repairWorkflow.receive(eq(2L), eq(100L), eq(1L), eq(10L), eq(20500))).thenReturn(999L);

        CheckInVehicleResponse response = receptionService.checkInVehicle(req);

        assertNotNull(response);
        assertEquals(999L, response.getRepairOrderId());
        assertEquals(100L, response.getBookingId());
        assertEquals(10L, response.getVehicleId());
        assertEquals("51F12345", response.getLicensePlate());
        assertEquals("Nguyen Van A", response.getCustomerName());
        assertEquals(20500, response.getOdometerIn());
        assertEquals(BookingStatus.RECEIVED, booking.getStatus());

        verify(bookingRepository, times(1)).save(booking);
        verify(vehicleRepository, times(1)).save(vehicle);
        assertEquals(20500, vehicle.getOdometer());
    }

    @Test
    @DisplayName("TV2-07: Tiếp nhận xe vãng lai walk-in thành công (booking_id = null)")
    void testCheckInWalkIn_Success() {
        CheckInVehicleRequest req = new CheckInVehicleRequest();
        req.setBookingId(null);
        req.setVehicleId(10L);
        req.setOdometerIn(20000);
        req.setCustomerComplaint("Khach vang lai can thay dau");
        req.setActorUserId(2L);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(repairWorkflow.receive(eq(2L), isNull(), eq(1L), eq(10L), eq(20000))).thenReturn(888L);

        CheckInVehicleResponse response = receptionService.checkInVehicle(req);

        assertNotNull(response);
        assertEquals(888L, response.getRepairOrderId());
        assertNull(response.getBookingId());
        verify(repairWorkflow, times(1)).receive(2L, null, 1L, 10L, 20000);
    }

    @Test
    @DisplayName("TV2-07: Báo lỗi khi lịch hẹn không khớp với phương tiện tiếp nhận")
    void testCheckIn_MismatchedBookingVehicle() {
        booking.setVehicleId(999L); // Lịch hẹn của xe khác

        CheckInVehicleRequest req = new CheckInVehicleRequest();
        req.setBookingId(100L);
        req.setVehicleId(10L);
        req.setOdometerIn(20000);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> receptionService.checkInVehicle(req));
        assertTrue(ex.getMessage().contains("không thuộc về xe"));
        verify(repairWorkflow, never()).receive(anyLong(), any(), anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("TV2-07: Báo lỗi Conflict khi lịch hẹn đã tiếp nhận xe trước đó")
    void testCheckIn_AlreadyReceivedBooking() {
        booking.setStatus(BookingStatus.RECEIVED);

        CheckInVehicleRequest req = new CheckInVehicleRequest();
        req.setBookingId(100L);
        req.setVehicleId(10L);
        req.setOdometerIn(20000);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        ConflictException ex = assertThrows(ConflictException.class, () -> receptionService.checkInVehicle(req));
        assertTrue(ex.getMessage().contains("đã được tiếp nhận"));
        verify(repairWorkflow, never()).receive(anyLong(), any(), anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("TV2-07: Chặn tiếp nhận khi ODO nhỏ hơn số km đã ghi nhận trước đó")
    void testCheckIn_OdometerLowerThanCurrent() {
        CheckInVehicleRequest req = new CheckInVehicleRequest();
        req.setVehicleId(10L);
        req.setOdometerIn(19000); // Nhỏ hơn ODO hiện tại là 20000

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> receptionService.checkInVehicle(req));
        assertTrue(ex.getMessage().contains("không được nhỏ hơn"));
        verify(repairWorkflow, never()).receive(anyLong(), any(), anyLong(), anyLong(), anyInt());
    }
}
