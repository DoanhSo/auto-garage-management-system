package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.reception.dto.BookingResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CancelBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.RescheduleBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.mapper.BookingMapper;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.reception.service.BookingServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BayScheduling;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BayScheduling bayScheduling;

    @Spy
    private BookingMapper bookingMapper = new BookingMapper();

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Customer customer;
    private Vehicle vehicle;
    private ServiceEntity service;
    private CreateBookingRequest createReq;
    private Instant futureTime;

    @BeforeEach
    void setUp() {
        bookingService.setBayScheduling(bayScheduling);

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
        vehicle.setIsActive(true);

        service = new ServiceEntity();
        service.setId(100L);
        service.setServiceName("Bao duong 10.000km");
        service.setStandardMinutes(90);
        service.setIsActive(true);

        futureTime = Instant.now().plus(Duration.ofDays(1));

        createReq = new CreateBookingRequest();
        createReq.setCustomerId(1L);
        createReq.setVehicleId(10L);
        createReq.setServiceId(100L);
        createReq.setBayId(5L);
        createReq.setAppointmentAt(futureTime);
        createReq.setRequestDescription("Kiem tra dinh ky");
        createReq.setCreatedBy(2L);
    }

    @Test
    @DisplayName("TV2-05: Đặt lịch hẹn thành công, tính toán expectedEndAt và giữ khoang qua I02")
    void testCreateBooking_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(bookingRepository.hasOverlappingBookingForVehicle(eq(10L), eq(futureTime), any(Instant.class), isNull()))
                .thenReturn(false);
        when(bookingRepository.existsByBookingCode(any())).thenReturn(false);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(createReq);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals(futureTime, response.getAppointmentAt());
        assertEquals(futureTime.plus(Duration.ofMinutes(90)), response.getExpectedEndAt());
        assertEquals("51F12345", response.getLicensePlate());
        assertEquals(5L, response.getBayId());

        verify(bayScheduling, times(1)).reserveBooking(eq(2L), eq(500L), eq(5L), eq(futureTime), eq(futureTime.plus(Duration.ofMinutes(90))));
    }

    @Test
    @DisplayName("TV2-05: Từ chối đặt lịch khi thời gian hẹn ở quá khứ")
    void testCreateBooking_PastAppointment() {
        createReq.setAppointmentAt(Instant.now().minus(Duration.ofHours(2)));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> bookingService.createBooking(createReq));
        assertTrue(ex.getMessage().contains("quá khứ"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-05: Từ chối khi xe đã có lịch khác trùng khung giờ")
    void testCreateBooking_DuplicateVehicleBooking() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(bookingRepository.hasOverlappingBookingForVehicle(eq(10L), eq(futureTime), any(Instant.class), isNull()))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> bookingService.createBooking(createReq));
        assertTrue(ex.getMessage().contains("đã có lịch hẹn khác"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-06: Xác nhận lịch hẹn thành công (PENDING -> CONFIRMED)")
    void testConfirmBooking_Success() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setCustomerId(1L);
        booking.setVehicleId(10L);
        booking.setServiceId(100L);
        booking.setStatus(BookingStatus.PENDING);
        booking.setAppointmentAt(futureTime);
        booking.setExpectedEndAt(futureTime.plus(Duration.ofMinutes(90)));

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));

        BookingResponse response = bookingService.confirmBooking(500L, 1L);

        assertNotNull(response);
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
    }

    @Test
    @DisplayName("TV2-06: Dời lịch hẹn thành công, giải phóng khoang cũ và đặt khoang mới")
    void testRescheduleBooking_Success() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setCustomerId(1L);
        booking.setVehicleId(10L);
        booking.setServiceId(100L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAppointmentAt(futureTime);
        booking.setExpectedEndAt(futureTime.plus(Duration.ofMinutes(90)));

        Instant newTime = futureTime.plus(Duration.ofDays(2));
        RescheduleBookingRequest rescheduleReq = new RescheduleBookingRequest();
        rescheduleReq.setNewAppointmentAt(newTime);
        rescheduleReq.setNewBayId(8L);
        rescheduleReq.setActorUserId(2L);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(bookingRepository.hasOverlappingBookingForVehicle(eq(10L), eq(newTime), any(Instant.class), eq(500L)))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        BookingResponse response = bookingService.rescheduleBooking(500L, rescheduleReq);

        assertNotNull(response);
        assertEquals(newTime, response.getAppointmentAt());
        assertEquals(newTime.plus(Duration.ofMinutes(90)), response.getExpectedEndAt());

        verify(bayScheduling, times(1)).releaseForBooking(2L, 500L);
        verify(bayScheduling, times(1)).reserveBooking(eq(2L), eq(500L), eq(8L), eq(newTime), eq(newTime.plus(Duration.ofMinutes(90))));
    }

    @Test
    @DisplayName("TV2-06: Hủy lịch hẹn thành công, lưu lý do và giải phóng khoang")
    void testCancelBooking_Success() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setCustomerId(1L);
        booking.setVehicleId(10L);
        booking.setServiceId(100L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAppointmentAt(futureTime);
        booking.setExpectedEndAt(futureTime.plus(Duration.ofMinutes(90)));

        CancelBookingRequest cancelReq = new CancelBookingRequest();
        cancelReq.setCancellationReason("Khach ban cong tac");
        cancelReq.setActorUserId(1L);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));

        BookingResponse response = bookingService.cancelBooking(500L, cancelReq);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        assertEquals("Khach ban cong tac", response.getCancellationReason());
        verify(bayScheduling, times(1)).releaseForBooking(1L, 500L);
    }

    @Test
    @DisplayName("TV2-06: Từ chối hủy khi xe đã vào xưởng tiếp nhận (status = RECEIVED)")
    void testCancelBooking_AlreadyReceived() {
        Booking booking = new Booking();
        booking.setId(500L);
        booking.setStatus(BookingStatus.RECEIVED);

        CancelBookingRequest cancelReq = new CancelBookingRequest();
        cancelReq.setCancellationReason("Huy muon");

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> bookingService.cancelBooking(500L, cancelReq));
        assertTrue(ex.getMessage().contains("tiếp nhận"));
        verify(bayScheduling, never()).releaseForBooking(anyLong(), anyLong());
    }
}
