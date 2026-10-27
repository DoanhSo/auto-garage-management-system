package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.reception.dto.BookingResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CancelBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.RescheduleBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.BOOKINGS_BASE)
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Customer service appointments, rescheduling and capacity planning")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    @Operation(summary = "Create service appointment booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            HttpServletRequest httpRequest) {
        request.setCreatedBy(currentUserProvider.userId());
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.created(URI.create(ApiPaths.BOOKINGS_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    @Operation(summary = "Search bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTime,
            HttpServletRequest httpRequest) {
        List<BookingResponse> list = bookingService.searchBookings(customerId, vehicleId, status, fromTime, toTime);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, list, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    @Operation(summary = "Fetch booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    @Operation(summary = "Fetch booking by code")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByCode(
            @PathVariable String code,
            HttpServletRequest httpRequest) {
        BookingResponse response = bookingService.getBookingByCode(code);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Confirm service booking")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Long id,
            @RequestParam(required = false) Long actorUserId,
            HttpServletRequest httpRequest) {
        BookingResponse response = bookingService.confirmBooking(id, currentUserProvider.userId());
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    @Operation(summary = "Reschedule booking time slot")
    public ResponseEntity<ApiResponse<BookingResponse>> rescheduleBooking(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleBookingRequest request,
            HttpServletRequest httpRequest) {
        request.setActorUserId(currentUserProvider.userId());
        BookingResponse response = bookingService.rescheduleBooking(id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(SecurityConstants.HAS_BOOKING_ACCESS)
    @Operation(summary = "Cancel booking appointment")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody CancelBookingRequest request,
            HttpServletRequest httpRequest) {
        request.setActorUserId(currentUserProvider.userId());
        BookingResponse response = bookingService.cancelBooking(id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Mark booking as no-show")
    public ResponseEntity<ApiResponse<BookingResponse>> markNoShow(
            @PathVariable Long id,
            @RequestParam(required = false) Long actorUserId,
            HttpServletRequest httpRequest) {
        BookingResponse response = bookingService.markNoShow(id, currentUserProvider.userId());
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }
}
