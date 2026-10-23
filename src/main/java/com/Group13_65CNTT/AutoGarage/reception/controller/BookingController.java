package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.reception.dto.BookingResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CancelBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.RescheduleBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.service.BookingService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.BOOKINGS_BASE)
@RequiredArgsConstructor
@Tag(name = "Bookings")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {
        if (request.getCreatedBy() == null) {
            request.setCreatedBy(currentUserProvider.userIdOrNull());
        }
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    public ResponseEntity<List<BookingResponse>> getBookings(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant toTime) {
        List<BookingResponse> list =
                bookingService.searchBookings(customerId, vehicleId, status, fromTime, toTime);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    public ResponseEntity<BookingResponse> getBookingByCode(@PathVariable String code) {
        return ResponseEntity.ok(bookingService.getBookingByCode(code));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id, @RequestParam(required = false) Long actorUserId) {
        Long resolvedActor =
                currentUserProvider.userIdOrNull() != null
                        ? currentUserProvider.userIdOrNull()
                        : (actorUserId != null ? actorUserId : 1L);
        return ResponseEntity.ok(bookingService.confirmBooking(id, resolvedActor));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    public ResponseEntity<BookingResponse> rescheduleBooking(
            @PathVariable Long id, @Valid @RequestBody RescheduleBookingRequest request) {
        return ResponseEntity.ok(bookingService.rescheduleBooking(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id, @Valid @RequestBody CancelBookingRequest request) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, request));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    public ResponseEntity<BookingResponse> markNoShow(
            @PathVariable Long id, @RequestParam(required = false) Long actorUserId) {
        Long resolvedActor =
                currentUserProvider.userIdOrNull() != null
                        ? currentUserProvider.userIdOrNull()
                        : (actorUserId != null ? actorUserId : 1L);
        return ResponseEntity.ok(bookingService.markNoShow(id, resolvedActor));
    }
}
