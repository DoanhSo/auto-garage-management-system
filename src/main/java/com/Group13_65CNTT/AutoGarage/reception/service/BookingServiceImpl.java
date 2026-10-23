package com.Group13_65CNTT.AutoGarage.reception.service;

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
import com.Group13_65CNTT.AutoGarage.shared.contracts.BayScheduling;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRepository serviceRepository;
    private final BookingMapper bookingMapper;
    private final Random random = new Random();
    private BayScheduling bayScheduling;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            CustomerRepository customerRepository,
            VehicleRepository vehicleRepository,
            ServiceRepository serviceRepository,
            BookingMapper bookingMapper,
            ObjectProvider<BayScheduling> baySchedulingProvider) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceRepository = serviceRepository;
        this.bookingMapper = bookingMapper;
        this.bayScheduling =
                baySchedulingProvider != null ? baySchedulingProvider.getIfAvailable() : null;
    }

    public void setBayScheduling(BayScheduling bayScheduling) {
        this.bayScheduling = bayScheduling;
    }

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        Customer customer =
                customerRepository
                        .findById(request.getCustomerId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy khách hàng với ID: "
                                                        + request.getCustomerId()));
        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new BusinessRuleException("Khách hàng đang ở trạng thái ngừng hoạt động");
        }

        Vehicle vehicle =
                vehicleRepository
                        .findById(request.getVehicleId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy phương tiện với ID: "
                                                        + request.getVehicleId()));
        if (!Boolean.TRUE.equals(vehicle.getIsActive())) {
            throw new BusinessRuleException("Phương tiện đang ở trạng thái ngừng hoạt động");
        }

        ServiceEntity service =
                serviceRepository
                        .findById(request.getServiceId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy dịch vụ danh mục với ID: "
                                                        + request.getServiceId()));
        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new BusinessRuleException("Dịch vụ đang ở trạng thái ngừng cung cấp");
        }

        if (request.getAppointmentAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Thời gian hẹn không được ở trong quá khứ");
        }

        int durationMinutes =
                service.getStandardMinutes() != null && service.getStandardMinutes() > 0
                        ? service.getStandardMinutes()
                        : 60;
        Instant expectedEndAt =
                request.getAppointmentAt().plus(Duration.ofMinutes(durationMinutes));

        if (bookingRepository.hasOverlappingBookingForVehicle(
                vehicle.getId(), request.getAppointmentAt(), expectedEndAt, null)) {
            throw new ConflictException(
                    "Phương tiện "
                            + vehicle.getLicensePlate()
                            + " đã có lịch hẹn khác trong khung giờ này");
        }

        Booking booking = bookingMapper.toEntity(request);
        booking.setBookingCode(generateBookingCode());
        booking.setExpectedEndAt(expectedEndAt);
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.save(booking);

        Long reservedBayId = null;
        if (request.getBayId() != null && bayScheduling != null) {
            long actorId = request.getCreatedBy() != null ? request.getCreatedBy() : 1L;
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

    @Override
    public BookingResponse confirmBooking(Long id, Long actorUserId) {
        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy lịch hẹn với ID: " + id));

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
    public BookingResponse rescheduleBooking(Long id, RescheduleBookingRequest request) {
        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy lịch hẹn với ID: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.RECEIVED
                || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new BusinessRuleException(
                    "Không thể dời lịch hẹn ở trạng thái: " + booking.getStatus());
        }

        if (request.getNewAppointmentAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Thời gian hẹn mới không được ở trong quá khứ");
        }

        ServiceEntity service =
                serviceRepository
                        .findById(booking.getServiceId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy dịch vụ của lịch hẹn"));

        int durationMinutes =
                service.getStandardMinutes() != null && service.getStandardMinutes() > 0
                        ? service.getStandardMinutes()
                        : 60;
        Instant newExpectedEndAt =
                request.getNewAppointmentAt().plus(Duration.ofMinutes(durationMinutes));

        if (bookingRepository.hasOverlappingBookingForVehicle(
                booking.getVehicleId(),
                request.getNewAppointmentAt(),
                newExpectedEndAt,
                booking.getId())) {
            throw new ConflictException("Phương tiện đã có lịch hẹn khác trong khung giờ hẹn mới");
        }

        long actor = request.getActorUserId() != null ? request.getActorUserId() : 1L;
        if (bayScheduling != null) {
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
    public BookingResponse cancelBooking(Long id, CancelBookingRequest request) {
        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy lịch hẹn với ID: " + id));

        if (booking.getStatus() == BookingStatus.RECEIVED) {
            throw new BusinessRuleException(
                    "Phương tiện đã được tiếp nhận vào xưởng, không thể hủy lịch hẹn");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Lịch hẹn này đã được hủy trước đó");
        }

        long actor = request.getActorUserId() != null ? request.getActorUserId() : 1L;
        if (bayScheduling != null) {
            bayScheduling.releaseForBooking(actor, booking.getId());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getCancellationReason().trim());
        Booking updated = bookingRepository.save(booking);
        return loadResponse(updated);
    }

    @Override
    public BookingResponse markNoShow(Long id, Long actorUserId) {
        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy lịch hẹn với ID: " + id));

        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "Không thể đánh dấu vắng mặt cho lịch hẹn ở trạng thái: "
                            + booking.getStatus());
        }

        long actor = actorUserId != null ? actorUserId : 1L;
        if (bayScheduling != null) {
            bayScheduling.releaseForBooking(actor, booking.getId());
        }

        booking.setStatus(BookingStatus.NO_SHOW);
        Booking updated = bookingRepository.save(booking);
        return loadResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy lịch hẹn với ID: " + id));
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
        return bookingRepository
                .searchBookings(customerId, vehicleId, status, fromTime, toTime)
                .stream()
                .map(this::loadResponse)
                .toList();
    }

    private BookingResponse loadResponse(Booking booking) {
        Customer customer = customerRepository.findById(booking.getCustomerId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(booking.getVehicleId()).orElse(null);
        ServiceEntity service = serviceRepository.findById(booking.getServiceId()).orElse(null);
        return bookingMapper.toResponse(booking, customer, vehicle, service, null);
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
