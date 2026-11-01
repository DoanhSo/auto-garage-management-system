package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.customer.service.CustomerService;
import com.Group13_65CNTT.AutoGarage.reception.dto.BookingResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CancelBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.NewBookingCustomerRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.NewBookingVehicleRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.RescheduleBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.mapper.BookingMapper;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.reception.service.BookingServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BayScheduling;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @org.mockito.Spy private com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties workshop = new com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties();
    @Mock private com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;

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

    @Mock
    private CustomerService customerService;

    @Mock
    private VehicleService vehicleService;

    @Spy
    private BookingMapper bookingMapper = new BookingMapper();

    private static final Long ACTOR_USER_ID = 2L;

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
    }

    @Test
    void customerSearchUsesOwnCustomerScope() {
        when(customerScope.customerIdForQuery(null)).thenReturn(10L);
        when(bookingRepository.searchBookings(10L, null, null, null, null)).thenReturn(java.util.List.of());
        assertTrue(bookingService.searchBookings(null, null, null, null, null).isEmpty());
        verify(bookingRepository).searchBookings(10L, null, null, null, null);
    }

    @Test
    void defaultDurationUsesConfiguredMinutes() {
        workshop.setDefaultServiceMinutes(45);
        service.setStandardMinutes(null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(500L);
            return booking;
        });

        BookingResponse response = bookingService.createBooking(createReq, ACTOR_USER_ID);
        assertEquals(futureTime.plus(Duration.ofMinutes(45)), response.getExpectedEndAt());
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

        BookingResponse response = bookingService.createBooking(createReq, ACTOR_USER_ID);

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

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
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

        ConflictException ex = assertThrows(ConflictException.class, () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
        assertTrue(ex.getMessage().contains("đã có lịch hẹn khác"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-05: Đặt lịch cho khách mới + xe mới trong cùng một request")
    void testCreateBooking_NewCustomerAndNewVehicle() {
        createReq.setCustomerId(null);
        createReq.setVehicleId(null);
        createReq.setNewCustomer(newCustomerRequest());
        createReq.setNewVehicle(newVehicleRequest());
        stubCreatedCustomerAndVehicle();
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(501L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(createReq, ACTOR_USER_ID);

        assertEquals(501L, response.getId());
        assertEquals(1L, response.getCustomerId());
        assertEquals(10L, response.getVehicleId());
        verify(customerScope).requireFrontDesk();

        ArgumentCaptor<CreateCustomerRequest> customerCaptor = ArgumentCaptor.forClass(CreateCustomerRequest.class);
        verify(customerService).createCustomer(customerCaptor.capture());
        assertEquals("Tran Thi B", customerCaptor.getValue().getFullName());
        assertEquals("0911222333", customerCaptor.getValue().getPhone());
        assertNull(customerCaptor.getValue().getEmail());

        ArgumentCaptor<CreateVehicleRequest> vehicleCaptor = ArgumentCaptor.forClass(CreateVehicleRequest.class);
        verify(vehicleService).createVehicle(vehicleCaptor.capture());
        assertEquals(1L, vehicleCaptor.getValue().getCustomerId());
        assertEquals("30A-999.99", vehicleCaptor.getValue().getLicensePlate());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals(ACTOR_USER_ID, bookingCaptor.getValue().getCreatedBy());
    }

    @Test
    @DisplayName("TV2-05: serviceId null dùng thời lượng mặc định của xưởng")
    void testCreateBooking_WithoutServiceUsesDefaultDuration() {
        workshop.setDefaultServiceMinutes(60);
        createReq.setServiceId(null);
        createReq.setBayId(null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(502L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(createReq, ACTOR_USER_ID);

        assertNull(response.getServiceId());
        assertNull(response.getServiceName());
        assertEquals(futureTime.plus(Duration.ofMinutes(60)), response.getExpectedEndAt());
        verify(serviceRepository, never()).findById(any());
    }

    @Test
    @DisplayName("TV2-05: Gửi cả customerId và newCustomer bị từ chối")
    void testCreateBooking_BothCustomerSources() {
        createReq.setNewCustomer(newCustomerRequest());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
        assertEquals(ResponseCode.BOOKING_CUSTOMER_SOURCE_INVALID, ex.getResponseCode());
        verifyNoInteractions(customerService, vehicleService);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-05: Thiếu cả customerId và newCustomer bị từ chối")
    void testCreateBooking_MissingCustomerSource() {
        createReq.setCustomerId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
        assertEquals(ResponseCode.BOOKING_CUSTOMER_SOURCE_INVALID, ex.getResponseCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-05: Gửi cả vehicleId và newVehicle bị từ chối")
    void testCreateBooking_BothVehicleSources() {
        createReq.setNewVehicle(newVehicleRequest());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
        assertEquals(ResponseCode.BOOKING_VEHICLE_SOURCE_INVALID, ex.getResponseCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-05: Khách mới nhưng chọn xe có sẵn bị từ chối")
    void testCreateBooking_NewCustomerWithExistingVehicle() {
        createReq.setCustomerId(null);
        createReq.setNewCustomer(newCustomerRequest());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
        assertEquals(ResponseCode.BOOKING_VEHICLE_SOURCE_INVALID, ex.getResponseCode());
        verifyNoInteractions(customerService, vehicleService);
    }

    @Test
    @DisplayName("TV2-05: Xe không thuộc khách đã chọn bị từ chối")
    void testCreateBooking_VehicleOwnerMismatch() {
        vehicle.setCustomerId(99L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
        assertEquals(ResponseCode.BOOKING_VEHICLE_OWNER_MISMATCH, ex.getResponseCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-05: Trùng giờ hẹn khi tạo khách mới thì không lưu lịch (transaction rollback)")
    void testCreateBooking_NewCustomerOverlapRejected() {
        createReq.setCustomerId(null);
        createReq.setVehicleId(null);
        createReq.setNewCustomer(newCustomerRequest());
        createReq.setNewVehicle(newVehicleRequest());
        stubCreatedCustomerAndVehicle();
        when(serviceRepository.findById(100L)).thenReturn(Optional.of(service));
        when(bookingRepository.hasOverlappingBookingForVehicle(eq(10L), eq(futureTime), any(Instant.class), isNull()))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(bayScheduling);
    }

    @Test
    @DisplayName("TV2-05: Vai trò CUSTOMER không được tạo khách mới khi đặt lịch")
    void testCreateBooking_CustomerRoleCannotCreateNewCustomer() {
        createReq.setCustomerId(null);
        createReq.setVehicleId(null);
        createReq.setNewCustomer(newCustomerRequest());
        createReq.setNewVehicle(newVehicleRequest());
        doThrow(new BusinessException(ResponseCode.FORBIDDEN)).when(customerScope).requireFrontDesk();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.createBooking(createReq, ACTOR_USER_ID));
        assertEquals(ResponseCode.FORBIDDEN, ex.getResponseCode());
        verifyNoInteractions(customerService, vehicleService);
        verify(bookingRepository, never()).save(any());
    }

    private void stubCreatedCustomerAndVehicle() {
        CustomerResponse createdCustomer = new CustomerResponse();
        createdCustomer.setId(1L);
        VehicleResponse createdVehicle = new VehicleResponse();
        createdVehicle.setId(10L);
        when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(createdCustomer);
        when(vehicleService.createVehicle(any(CreateVehicleRequest.class))).thenReturn(createdVehicle);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
    }

    private NewBookingCustomerRequest newCustomerRequest() {
        NewBookingCustomerRequest request = new NewBookingCustomerRequest();
        request.setFullName("  Tran Thi B ");
        request.setPhone("0911222333");
        request.setEmail("   ");
        return request;
    }

    private NewBookingVehicleRequest newVehicleRequest() {
        NewBookingVehicleRequest request = new NewBookingVehicleRequest();
        request.setLicensePlate("30A-999.99");
        request.setBrand("Honda");
        request.setModel("City");
        return request;
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
