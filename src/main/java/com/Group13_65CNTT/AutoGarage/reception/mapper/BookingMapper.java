package com.Group13_65CNTT.AutoGarage.reception.mapper;

import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.reception.dto.BookingResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateBookingRequest;
import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public Booking toEntity(CreateBookingRequest request) {
        if (request == null) {
            return null;
        }
        Booking booking = new Booking();
        booking.setCustomerId(request.getCustomerId());
        booking.setVehicleId(request.getVehicleId());
        booking.setServiceId(request.getServiceId());
        booking.setAppointmentAt(request.getAppointmentAt());
        booking.setRequestDescription(request.getRequestDescription() != null ? request.getRequestDescription().trim() : null);
        booking.setCreatedBy(request.getCreatedBy());
        return booking;
    }

    public BookingResponse toResponse(Booking booking, Customer customer, Vehicle vehicle, ServiceEntity service, Long bayId) {
        if (booking == null) {
            return null;
        }
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setBookingCode(booking.getBookingCode());
        response.setCustomerId(booking.getCustomerId());
        if (customer != null) {
            response.setCustomerName(customer.getFullName());
            response.setCustomerPhone(customer.getPhone());
        }
        response.setVehicleId(booking.getVehicleId());
        if (vehicle != null) {
            response.setLicensePlate(vehicle.getLicensePlate());
            response.setVehicleModel(vehicle.getBrand() + " " + vehicle.getModel());
        }
        response.setServiceId(booking.getServiceId());
        if (service != null) {
            response.setServiceName(service.getServiceName());
            response.setStandardMinutes(service.getStandardMinutes());
        }
        response.setBayId(bayId);
        response.setAppointmentAt(booking.getAppointmentAt());
        response.setExpectedEndAt(booking.getExpectedEndAt());
        response.setRequestDescription(booking.getRequestDescription());
        response.setStatus(booking.getStatus());
        response.setCancellationReason(booking.getCancellationReason());
        response.setCreatedBy(booking.getCreatedBy());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        response.setVersion(booking.getVersion());
        return response;
    }
}
