package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.RepairWorkflow;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ReceptionServiceImpl implements ReceptionService {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private RepairWorkflow repairWorkflow;

    public ReceptionServiceImpl(
            VehicleRepository vehicleRepository,
            CustomerRepository customerRepository,
            BookingRepository bookingRepository,
            ObjectProvider<RepairWorkflow> repairWorkflowProvider) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.repairWorkflow =
                repairWorkflowProvider != null ? repairWorkflowProvider.getIfAvailable() : null;
    }

    public void setRepairWorkflow(RepairWorkflow repairWorkflow) {
        this.repairWorkflow = repairWorkflow;
    }

    @Override
    public CheckInVehicleResponse checkInVehicle(CheckInVehicleRequest request) {
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

        Customer customer =
                customerRepository
                        .findById(vehicle.getCustomerId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy chủ sở hữu của phương tiện"));

        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new BusinessRuleException(
                    "Khách hàng sở hữu xe đang ở trạng thái ngừng hoạt động");
        }

        if (request.getOdometerIn() == null || request.getOdometerIn() < 0) {
            throw new BusinessRuleException("Số km tiếp nhận không được âm");
        }

        if (vehicle.getOdometer() != null && request.getOdometerIn() < vehicle.getOdometer()) {
            throw new BusinessRuleException(
                    "Số km tiếp nhận ("
                            + request.getOdometerIn()
                            + ") không được nhỏ hơn số km đã ghi nhận trước đó ("
                            + vehicle.getOdometer()
                            + ")");
        }

        Long bookingId = request.getBookingId();
        if (bookingId != null) {
            Booking booking =
                    bookingRepository
                            .findById(bookingId)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Không tìm thấy lịch hẹn với ID: "
                                                            + bookingId));

            if (!booking.getVehicleId().equals(vehicle.getId())) {
                throw new BusinessRuleException(
                        "Lịch hẹn "
                                + booking.getBookingCode()
                                + " không thuộc về xe "
                                + vehicle.getLicensePlate());
            }

            if (booking.getStatus() == BookingStatus.CANCELLED
                    || booking.getStatus() == BookingStatus.NO_SHOW) {
                throw new BusinessRuleException(
                        "Không thể tiếp nhận xe với lịch hẹn đã bị hủy hoặc vắng mặt: "
                                + booking.getStatus());
            }

            if (booking.getStatus() == BookingStatus.RECEIVED) {
                throw new ConflictException("Lịch hẹn này đã được tiếp nhận xe trước đó");
            }

            booking.setStatus(BookingStatus.RECEIVED);
            bookingRepository.save(booking);
        }

        vehicle.setOdometer(request.getOdometerIn());
        vehicleRepository.save(vehicle);

        long actor = request.getActorUserId() != null ? request.getActorUserId() : 1L;
        long repairOrderId;
        if (repairWorkflow != null) {
            repairOrderId =
                    repairWorkflow.receive(
                            actor,
                            bookingId,
                            customer.getId(),
                            vehicle.getId(),
                            request.getOdometerIn());
        } else {
            repairOrderId = System.currentTimeMillis();
        }

        CheckInVehicleResponse response = new CheckInVehicleResponse();
        response.setRepairOrderId(repairOrderId);
        response.setBookingId(bookingId);
        response.setCustomerId(customer.getId());
        response.setCustomerName(customer.getFullName());
        response.setCustomerPhone(customer.getPhone());
        response.setVehicleId(vehicle.getId());
        response.setLicensePlate(vehicle.getLicensePlate());
        response.setVehicleModel(vehicle.getBrand() + " " + vehicle.getModel());
        response.setOdometerIn(request.getOdometerIn());
        response.setFuelLevelPercent(request.getFuelLevelPercent());
        response.setCustomerComplaint(request.getCustomerComplaint());
        response.setReceivedBy(actor);
        response.setReceivedAt(Instant.now());

        return response;
    }
}
