package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.reception.dto.BookingResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CancelBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.RescheduleBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;

import java.time.Instant;
import java.util.List;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse confirmBooking(Long id, Long actorUserId);

    BookingResponse rescheduleBooking(Long id, RescheduleBookingRequest request);

    BookingResponse cancelBooking(Long id, CancelBookingRequest request);

    BookingResponse markNoShow(Long id, Long actorUserId);

    BookingResponse getBookingById(Long id);

    BookingResponse getBookingByCode(String code);

    List<BookingResponse> searchBookings(Long customerId, Long vehicleId, BookingStatus status, Instant fromTime, Instant toTime);
}
