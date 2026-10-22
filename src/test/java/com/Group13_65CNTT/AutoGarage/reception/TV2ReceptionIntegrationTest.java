package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.catalog.dto.CreateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;
import com.Group13_65CNTT.AutoGarage.catalog.service.CatalogService;
import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.entity.CustomerType;
import com.Group13_65CNTT.AutoGarage.customer.service.CustomerService;
import com.Group13_65CNTT.AutoGarage.reception.dto.*;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.service.AttachmentService;
import com.Group13_65CNTT.AutoGarage.reception.service.BookingService;
import com.Group13_65CNTT.AutoGarage.reception.service.ReceptionService;
import com.Group13_65CNTT.AutoGarage.reception.service.VehicleCheckService;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.TransferOwnershipRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleTimelineResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TV2ReceptionIntegrationTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private VehicleService vehicleService;

    @Mock
    private CatalogService catalogService;

    @Mock
    private BookingService bookingService;

    @Mock
    private ReceptionService receptionService;

    @Mock
    private VehicleCheckService vehicleCheckService;

    @Mock
    private AttachmentService attachmentService;

    @Test
    @DisplayName("TV2-10: Kiểm thử liên thông nghiệp vụ trọn vẹn từ Khách -> Xe -> Đặt lịch -> Nhận xe -> Checklist -> Đổi chủ")
    void testFullReceptionWorkflowIntegration() {
        // 1. Tạo Khách hàng
        CustomerResponse customer = new CustomerResponse();
        customer.setId(1L);
        customer.setCustomerCode("CUS-001");
        customer.setFullName("Nguyen Van A");
        customer.setPhone("0901234567");
        when(customerService.createCustomer(any())).thenReturn(customer);

        CustomerResponse createdCustomer = customerService.createCustomer(new CreateCustomerRequest());
        assertNotNull(createdCustomer);
        assertEquals(1L, createdCustomer.getId());

        // 2. Tạo Phương tiện
        VehicleResponse vehicle = new VehicleResponse();
        vehicle.setId(10L);
        vehicle.setCustomerId(createdCustomer.getId());
        vehicle.setLicensePlate("51H99999");
        vehicle.setBrand("Honda");
        vehicle.setModel("CR-V");
        vehicle.setOdometer(10000);
        when(vehicleService.createVehicle(any())).thenReturn(vehicle);

        VehicleResponse createdVehicle = vehicleService.createVehicle(new CreateVehicleRequest());
        assertNotNull(createdVehicle);
        assertEquals("51H99999", createdVehicle.getLicensePlate());

        // 3. Tạo Dịch vụ danh mục
        ServiceResponse service = new ServiceResponse();
        service.setId(100L);
        service.setServiceCode("SRV-001");
        service.setServiceName("Bao duong toan dien");
        service.setStandardMinutes(120);
        when(catalogService.createService(any())).thenReturn(service);

        ServiceResponse createdService = catalogService.createService(new CreateServiceRequest());
        assertNotNull(createdService);

        // 4. Đặt lịch hẹn
        Instant appointmentTime = Instant.now().plus(Duration.ofDays(1));
        BookingResponse booking = new BookingResponse();
        booking.setId(500L);
        booking.setBookingCode("BKG-001");
        booking.setCustomerId(createdCustomer.getId());
        booking.setVehicleId(createdVehicle.getId());
        booking.setServiceId(createdService.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAppointmentAt(appointmentTime);
        booking.setExpectedEndAt(appointmentTime.plus(Duration.ofMinutes(120)));
        when(bookingService.createBooking(any())).thenReturn(booking);

        BookingResponse createdBooking = bookingService.createBooking(new CreateBookingRequest());
        assertEquals(BookingStatus.CONFIRMED, createdBooking.getStatus());

        // 5. Tiếp nhận xe vào xưởng
        CheckInVehicleResponse checkIn = new CheckInVehicleResponse();
        checkIn.setRepairOrderId(800L);
        checkIn.setBookingId(createdBooking.getId());
        checkIn.setVehicleId(createdVehicle.getId());
        checkIn.setOdometerIn(10200);
        when(receptionService.checkInVehicle(any())).thenReturn(checkIn);

        CheckInVehicleResponse checkInResult = receptionService.checkInVehicle(new CheckInVehicleRequest());
        assertEquals(800L, checkInResult.getRepairOrderId());
        assertEquals(10200, checkInResult.getOdometerIn());

        // 6. Ghi biên bản kiểm tra tiếp nhận & Upload ảnh
        VehicleCheckResponse checkResponse = new VehicleCheckResponse();
        checkResponse.setId(900L);
        checkResponse.setRepairOrderId(checkInResult.getRepairOrderId());
        checkResponse.setCheckType(CheckType.CHECK_IN);
        checkResponse.setAttemptNo(1);
        when(vehicleCheckService.recordVehicleCheck(any())).thenReturn(checkResponse);

        VehicleCheckResponse recordedCheck = vehicleCheckService.recordVehicleCheck(new CreateVehicleCheckRequest());
        assertEquals(1, recordedCheck.getAttemptNo());

        AttachmentResponse attachResponse = new AttachmentResponse();
        attachResponse.setId(950L);
        attachResponse.setStorageKey("attachments/check_in/2026/10/photo1.jpg");
        attachResponse.setSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        when(attachmentService.uploadAttachment(any(), any(), any(), any(), any(), any())).thenReturn(attachResponse);

        AttachmentResponse uploaded = attachmentService.uploadAttachment(
                checkInResult.getRepairOrderId(), recordedCheck.getId(), null,
                AttachmentCategory.CHECK_IN, 1L, new MockMultipartFile("f", "test.jpg", "image/jpeg", new byte[]{1, 2})
        );
        assertNotNull(uploaded.getStorageKey());

        // 7. Đổi chủ xe và tra cứu timeline
        VehicleResponse transferred = new VehicleResponse();
        transferred.setId(createdVehicle.getId());
        transferred.setCustomerId(2L); // Chuyển sang chủ mới
        when(vehicleService.transferOwnership(eq(10L), any())).thenReturn(transferred);

        VehicleResponse transferResult = vehicleService.transferOwnership(10L, new TransferOwnershipRequest());
        assertEquals(2L, transferResult.getCustomerId());

        VehicleTimelineResponse timeline = new VehicleTimelineResponse();
        timeline.setVehicleId(10L);
        timeline.setLicensePlate("51H99999");
        timeline.setCurrentCustomerId(2L);
        when(vehicleService.getVehicleTimeline(eq(10L), any(), any())).thenReturn(timeline);

        VehicleTimelineResponse timelineResult = vehicleService.getVehicleTimeline(10L, 2L, "CUSTOMER");
        assertEquals(2L, timelineResult.getCurrentCustomerId());
    }
}
