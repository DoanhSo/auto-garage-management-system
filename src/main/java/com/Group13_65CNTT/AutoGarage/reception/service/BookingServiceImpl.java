package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;

import com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CustomerScope;
import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.security.ActorIds;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
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
import com.Group13_65CNTT.AutoGarage.shared.contracts.BayScheduling;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.service.VehicleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private final com.Group13_65CNTT.AutoGarage.scheduling.repository.BayReservationRepository reservations;
    private final WorkshopProperties workshop;
    private final CustomerScope customerScope;

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRepository serviceRepository;
    private final BookingMapper bookingMapper;
    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final Random random = new Random();
    private final BayScheduling bayScheduling;

    @Override
    @Audited(action = AuditConstants.ACTION_BOOKING_CREATED, entity = AuditConstants.ENTITY_BOOKING)
    public BookingResponse createBooking(CreateBookingRequest request, Long actorUserId) {
        customerScope.requireActor(actorUserId);
        customerScope.requireBookingAccess();
        validateBookingSources(request);
        if (request.getNewCustomer() != null || request.getNewVehicle() != null) {
            customerScope.requireFrontDesk();
        }

        Customer customer = resolveCustomer(request);
        Vehicle vehicle = resolveVehicle(request, customer);
        ServiceEntity service = resolveActiveService(request.getServiceId());

        if (request.getAppointmentAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Thời gian hẹn không được ở trong quá khứ");
        }

        int durationMinutes = resolveDurationMinutes(service);
        Instant expectedEndAt =
                request.getAppointmentAt().plus(Duration.ofMinutes(durationMinutes));

        if (!bookingRepository.findOverlappingForUpdate(
                vehicle.getId(), request.getAppointmentAt(), expectedEndAt, null).isEmpty()) {
            throw new ConflictException(
                    "Phương tiện "
                            + vehicle.getLicensePlate()
                            + " đã có lịch hẹn khác trong khung giờ này");
        }

        Booking booking =
                bookingMapper.toEntity(request, customer.getId(), vehicle.getId(), actorUserId);
        booking.setBookingCode(generateBookingCode());
        booking.setExpectedEndAt(expectedEndAt);
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.save(booking);

        Long reservedBayId = null;
        if (request.getBayId() != null) {
            long actorId = ActorIds.require(actorUserId);
            bayScheduling.reserveBooking(
                    actorId,
                    saved.getId(),
                    request.getBayId(),
                    request.getAppointmentAt(),
                    expectedEndAt);
            reservedBayId = request.getBayId();
        }

        return bookingMapper.toResponse(saved, customer, vehicle, service, reservedBayId);
    }

    private void validateBookingSources(CreateBookingRequest request) {
        if ((request.getCustomerId() == null) == (request.getNewCustomer() == null)) {
            throw new BusinessException(
                    ResponseCode.BOOKING_CUSTOMER_SOURCE_INVALID,
                    "Phải chọn khách hàng có sẵn hoặc nhập thông tin khách hàng mới");
        }
        if ((request.getVehicleId() == null) == (request.getNewVehicle() == null)) {
            throw new BusinessException(
                    ResponseCode.BOOKING_VEHICLE_SOURCE_INVALID,
                    "Phải chọn phương tiện có sẵn hoặc nhập thông tin phương tiện mới");
        }
        if (request.getNewCustomer() != null && request.getVehicleId() != null) {
            throw new BusinessException(
                    ResponseCode.BOOKING_VEHICLE_SOURCE_INVALID,
                    "Khách hàng mới chưa có phương tiện đã đăng ký trên hệ thống");
        }
    }

    private Customer resolveCustomer(CreateBookingRequest request) {
        Long customerId = request.getCustomerId();
        if (customerId != null) {
            customerScope.requireResource(AccessResource.CUSTOMER, customerId);
            Customer customer = findCustomer(customerId);
            if (!Boolean.TRUE.equals(customer.getIsActive())) {
                throw new BusinessRuleException("Khách hàng đang ở trạng thái ngừng hoạt động");
            }
            return customer;
        }

        NewBookingCustomerRequest newCustomer = request.getNewCustomer();
        CreateCustomerRequest createCustomerRequest = new CreateCustomerRequest();
        createCustomerRequest.setFullName(trimToNull(newCustomer.getFullName()));
        createCustomerRequest.setPhone(trimToNull(newCustomer.getPhone()));
        createCustomerRequest.setEmail(trimToNull(newCustomer.getEmail()));
        CustomerResponse createdCustomer = customerService.createCustomer(createCustomerRequest);
        return findCustomer(createdCustomer.getId());
    }

    private Vehicle resolveVehicle(CreateBookingRequest request, Customer customer) {
        Long vehicleId = request.getVehicleId();
        if (vehicleId != null) {
            customerScope.requireResource(AccessResource.VEHICLE, vehicleId);
            Vehicle vehicle = findVehicle(vehicleId);
            if (!Boolean.TRUE.equals(vehicle.getIsActive())) {
                throw new BusinessRuleException("Phương tiện đang ở trạng thái ngừng hoạt động");
            }
            if (!customer.getId().equals(vehicle.getCustomerId())) {
                throw new BusinessException(
                        ResponseCode.BOOKING_VEHICLE_OWNER_MISMATCH,
                        "Phương tiện không thuộc sở hữu của khách hàng đã chọn");
            }
            return vehicle;
        }

        NewBookingVehicleRequest newVehicle = request.getNewVehicle();
        CreateVehicleRequest createVehicleRequest = new CreateVehicleRequest();
        createVehicleRequest.setCustomerId(customer.getId());
        createVehicleRequest.setLicensePlate(trimToNull(newVehicle.getLicensePlate()));
        createVehicleRequest.setBrand(trimToNull(newVehicle.getBrand()));
        createVehicleRequest.setModel(trimToNull(newVehicle.getModel()));
        createVehicleRequest.setManufactureYear(newVehicle.getManufactureYear());
        createVehicleRequest.setColor(trimToNull(newVehicle.getColor()));
        VehicleResponse createdVehicle = vehicleService.createVehicle(createVehicleRequest);
        return findVehicle(createdVehicle.getId());
    }

    private ServiceEntity resolveActiveService(Long serviceId) {
        if (serviceId == null) {
            return null;
        }
        ServiceEntity service =
                serviceRepository
                        .findById(serviceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy dịch vụ danh mục với ID: "
                                                        + serviceId));
        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new BusinessRuleException("Dịch vụ đang ở trạng thái ngừng cung cấp");
        }
        return service;
    }

    private int resolveDurationMinutes(ServiceEntity service) {
        return service != null
                        && service.getStandardMinutes() != null
                        && service.getStandardMinutes() > 0
                ? service.getStandardMinutes()
                : workshop.getDefaultServiceMinutes();
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy khách hàng với ID: " + customerId));
    }

    private Vehicle findVehicle(Long vehicleId) {
        return vehicleRepository
                .findByIdForUpdate(vehicleId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy phương tiện với ID: " + vehicleId));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Audited(action = AuditConstants.ACTION_BOOKING_CONFIRMED, entity = AuditConstants.ENTITY_BOOKING, idArgument = 0)
    public BookingResponse confirmBooking(Long id, Long actorUserId) {
        customerScope.requireActor(actorUserId);
        customerScope.requireFrontDesk();
        Booking booking = lockBooking(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException(
                    "Chỉ có thể xác nhận lịch hẹn đang ở trạng thái PENDING. Trạng thái hiện tại: "
                            + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking updated = bookingRepository.save(booking);
        return loadResponse(updated);
    }

    @Override
    @Audited(action = AuditConstants.ACTION_BOOKING_RESCHEDULED, entity = AuditConstants.ENTITY_BOOKING, idArgument = 0)
    public BookingResponse rescheduleBooking(Long id, RescheduleBookingRequest request) {
        customerScope.requireBookingAccess();
        customerScope.requireActor(request.getActorUserId());
        customerScope.requireResource(AccessResource.BOOKING, id);
        Booking booking = lockBooking(id);

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.RECEIVED
                || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new BusinessRuleException(
                    "Không thể dời lịch hẹn ở trạng thái: " + booking.getStatus());
        }

        if (request.getNewAppointmentAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Thời gian hẹn mới không được ở trong quá khứ");
        }

        ServiceEntity service = null;
        if (booking.getServiceId() != null) {
            service =
                    serviceRepository
                            .findById(booking.getServiceId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Không tìm thấy dịch vụ của lịch hẹn"));
        }

        int durationMinutes = resolveDurationMinutes(service);
        Instant newExpectedEndAt =
                request.getNewAppointmentAt().plus(Duration.ofMinutes(durationMinutes));

        if (!bookingRepository.findOverlappingForUpdate(
                booking.getVehicleId(),
                request.getNewAppointmentAt(),
                newExpectedEndAt,
                booking.getId()).isEmpty()) {
            throw new ConflictException("Phương tiện đã có lịch hẹn khác trong khung giờ hẹn mới");
        }

        long actor = ActorIds.require(request.getActorUserId());
        {
            bayScheduling.releaseForBooking(actor, booking.getId());
            if (request.getNewBayId() != null) {
                bayScheduling.reserveBooking(
                        actor,
                        booking.getId(),
                        request.getNewBayId(),
                        request.getNewAppointmentAt(),
                        newExpectedEndAt);
            }
        }

        booking.setAppointmentAt(request.getNewAppointmentAt());
        booking.setExpectedEndAt(newExpectedEndAt);
        Booking updated = bookingRepository.save(booking);
        return loadResponse(updated);
    }

    @Override
    @Audited(action = AuditConstants.ACTION_BOOKING_CANCELLED, entity = AuditConstants.ENTITY_BOOKING, idArgument = 0)
    public BookingResponse cancelBooking(Long id, CancelBookingRequest request) {
        customerScope.requireBookingAccess();
        customerScope.requireActor(request.getActorUserId());
        customerScope.requireResource(AccessResource.BOOKING, id);
        Booking booking = lockBooking(id);

        if (booking.getStatus() == BookingStatus.RECEIVED) {
            throw new BusinessRuleException(
                    "Phương tiện đã được tiếp nhận vào xưởng, không thể hủy lịch hẹn");
        }
        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new BusinessRuleException("Không thể hủy lịch đã được đánh dấu vắng mặt");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Lịch hẹn này đã được hủy trước đó");
        }

        long actor = ActorIds.require(request.getActorUserId());
        {
            bayScheduling.releaseForBooking(actor, booking.getId());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getCancellationReason().trim());
        Booking updated = bookingRepository.save(booking);
        return loadResponse(updated);
    }

    @Override
    @Audited(action = AuditConstants.ACTION_BOOKING_NO_SHOW, entity = AuditConstants.ENTITY_BOOKING, idArgument = 0)
    public BookingResponse markNoShow(Long id, Long actorUserId) {
        customerScope.requireActor(actorUserId);
        customerScope.requireFrontDesk();
        Booking booking = lockBooking(id);

        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "Không thể đánh dấu vắng mặt cho lịch hẹn ở trạng thái: "
                            + booking.getStatus());
        }

        if (booking.getAppointmentAt().isAfter(Instant.now())) {
            throw new BusinessRuleException("Chưa đến giờ hẹn, không thể đánh dấu vắng mặt");
        }

        long actor = ActorIds.require(actorUserId);
        {
            bayScheduling.releaseForBooking(actor, booking.getId());
        }

        booking.setStatus(BookingStatus.NO_SHOW);
        Booking updated = bookingRepository.save(booking);
        return loadResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        customerScope.requireResource(AccessResource.BOOKING, id);
        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy lịch hẹn với ID: " + id));
        customerScope.requireResource(AccessResource.BOOKING, booking.getId());
        return loadResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByCode(String code) {
        Booking booking =
                bookingRepository
                        .findByBookingCode(code.trim().toUpperCase())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy lịch hẹn với mã: " + code));
        customerScope.requireResource(AccessResource.BOOKING, booking.getId());
        return loadResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> searchBookings(
            Long customerId,
            Long vehicleId,
            BookingStatus status,
            Instant fromTime,
            Instant toTime) {
        Long scopedCustomerId = customerScope.customerIdForQuery(customerId);
        return loadResponses(bookingRepository.searchBookings(scopedCustomerId, vehicleId, status, fromTime, toTime));
    }

    private List<BookingResponse> loadResponses(List<Booking> bookings) {
        if (bookings.isEmpty()) return List.of();
        var customers = customerRepository.findAllById(bookings.stream().map(Booking::getCustomerId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Customer::getId, java.util.function.Function.identity()));
        var vehicles = vehicleRepository.findAllById(bookings.stream().map(Booking::getVehicleId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Vehicle::getId, java.util.function.Function.identity()));
        var services = serviceRepository.findAllById(bookings.stream().map(Booking::getServiceId)
                .filter(java.util.Objects::nonNull).distinct().toList()).stream().collect(java.util.stream.Collectors.toMap(
                        ServiceEntity::getId, java.util.function.Function.identity()));
        var bays = reservedBays(bookings.stream().map(Booking::getId).toList());
        return bookings.stream().map(booking -> bookingMapper.toResponse(booking, customers.get(booking.getCustomerId()),
                vehicles.get(booking.getVehicleId()), services.get(booking.getServiceId()), bays.get(booking.getId()))).toList();
    }

    private java.util.Map<Long, Long> reservedBays(List<Long> ids) {
        var now = Instant.now();
        return reservations.findByBookingIdInAndStatusIn(ids, java.util.Set.of(
                com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationStatus.HELD,
                com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationStatus.IN_USE)).stream()
                .filter(reservation -> reservation.getExpiresAt() == null || reservation.getExpiresAt().isAfter(now))
                .collect(java.util.stream.Collectors.toMap(
                        com.Group13_65CNTT.AutoGarage.scheduling.entity.BayReservation::getBookingId,
                        com.Group13_65CNTT.AutoGarage.scheduling.entity.BayReservation::getBayId, (first, second) -> first));
    }


    private Booking lockBooking(Long id) {
        Long vehicleId = bookingRepository.findVehicleIdById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        findVehicle(vehicleId);
        return bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
    }

    private BookingResponse loadResponse(Booking booking) {
        Customer customer = customerRepository.findById(booking.getCustomerId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(booking.getVehicleId()).orElse(null);
        ServiceEntity service =
                booking.getServiceId() != null
                        ? serviceRepository.findById(booking.getServiceId()).orElse(null)
                        : null;
        return bookingMapper.toResponse(booking, customer, vehicle, service, reservedBays(List.of(booking.getId())).get(booking.getId()));
    }

    private String generateBookingCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            int randomPart = 1000 + random.nextInt(9000);
            code = "BKG-" + datePart + "-" + randomPart;
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }
}
