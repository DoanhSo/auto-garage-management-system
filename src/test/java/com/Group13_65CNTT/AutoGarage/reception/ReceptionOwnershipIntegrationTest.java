package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.reception.entity.Attachment;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import com.Group13_65CNTT.AutoGarage.reception.repository.AttachmentRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentRole;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.TaskAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@Transactional
@RequiredArgsConstructor
class ReceptionOwnershipIntegrationTest extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final AuthProperties authProperties;
    private final UserRepository users;
    private final CustomerRepository customers;
    private final VehicleRepository vehicles;
    private final BookingRepository bookings;
    private final RepairOrderRepository orders;
    private final RepairTaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final EmployeeRepository employees;
    private final VehicleCheckRepository checks;
    private final AttachmentRepository attachments;

    @Test
    void customersSeeOnlyOwnProfileVehiclesAndBookings() throws Exception {
        User user = users.saveAndFlush(User.builder()
                .identityId("550e8400-e29b-41d4-a716-446655990001")
                .username("ownership.customer").fullName("Own Account").role(Role.CUSTOMER).build());
        Customer own = customer("SCOPE-OWN", user.getId());
        Customer other = customer("SCOPE-OTHER", null);
        Vehicle ownVehicle = vehicle("SCOPE001", own.getId());
        Vehicle otherVehicle = vehicle("SCOPE002", other.getId());
        Booking ownBooking = booking("SCOPE-BOOK-OWN", own, ownVehicle);
        Booking otherBooking = booking("SCOPE-BOOK-OTHER", other, otherVehicle);
        RepairOrder ownOrder = order("SCOPE-ORDER-OWN", own, ownVehicle, user.getId());
        RepairOrder otherOrder = order("SCOPE-ORDER-OTHER", other, otherVehicle, user.getId());
        VehicleCheck ownCheck = check(ownOrder.getId(), user.getId());
        VehicleCheck otherCheck = check(otherOrder.getId(), user.getId());
        Attachment ownAttachment = attachment(ownOrder.getId(), ownCheck.getId(), user.getId());
        Attachment otherAttachment = attachment(otherOrder.getId(), otherCheck.getId(), user.getId());
        String authorization = "Bearer " + issueKeycloakToken(authProperties, user);

        mvc.perform(get("/api/v1/customers/me").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(own.getId()));
        mvc.perform(get("/api/v1/customers").header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/customers/" + other.getId()).header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/vehicles").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ownVehicle.getId()));
        mvc.perform(get("/api/v1/vehicles").param("customerId", other.getId().toString())
                        .header("Authorization", authorization)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/vehicles/" + otherVehicle.getId()).header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/vehicles/" + otherVehicle.getId() + "/timeline")
                        .param("role", "ADMIN").param("requestingCustomerId", other.getId().toString())
                        .header("Authorization", authorization)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/bookings").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ownBooking.getId()));
        mvc.perform(get("/api/v1/bookings/" + otherBooking.getId()).header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/bookings/" + otherBooking.getId() + "/cancel")
                        .header("Authorization", authorization).contentType("application/json")
                        .content("{\"cancellationReason\":\"forged request\",\"actorUserId\":1}"))
                .andExpect(status().isForbidden());
        org.assertj.core.api.Assertions.assertThat(bookings.findById(otherBooking.getId()).orElseThrow().getStatus())
                .isEqualTo(com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus.PENDING);

        for (String path : java.util.List.of(
                "/api/v1/attachments/" + ownAttachment.getId(),
                "/api/v1/attachments/order/" + ownOrder.getId(),
                "/api/v1/attachments/check/" + ownCheck.getId(),
                "/api/v1/vehicle-checks/" + ownCheck.getId(),
                "/api/v1/vehicle-checks/order/" + ownOrder.getId())) {
            mvc.perform(get(path).header("Authorization", authorization)).andExpect(status().isOk());
        }
        for (String path : java.util.List.of(
                "/api/v1/attachments/" + otherAttachment.getId(),
                "/api/v1/attachments/order/" + otherOrder.getId(),
                "/api/v1/attachments/check/" + otherCheck.getId(),
                "/api/v1/vehicle-checks/" + otherCheck.getId(),
                "/api/v1/vehicle-checks/order/" + otherOrder.getId())) {
            mvc.perform(get(path).header("Authorization", authorization)).andExpect(status().isForbidden());
        }
    }

    @Test
    void onlyActiveQcCapableAssignedTechnicianCanRecordQc() throws Exception {
        User technician = users.saveAndFlush(User.builder()
                .identityId("550e8400-e29b-41d4-a716-446655990002")
                .username("ownership.technician").fullName("Inspector").role(Role.TECHNICIAN).build());
        Customer customer = customer("QC-OWNER", null);
        Vehicle vehicle = vehicle("QCSCOPE001", customer.getId());
        RepairOrder order = order("QC-ORDER", customer, vehicle, technician.getId());
        RepairTask task = tasks.saveAndFlush(RepairTask.builder()
                .repairOrderId(order.getId()).taskName("Completed repair").status(TaskStatus.COMPLETED).build());
        String authorization = "Bearer " + issueKeycloakToken(authProperties, technician);
        String body = "{\"repairOrderId\":" + order.getId() + ",\"result\":\"PASSED\",\"checklistJson\":\"{}\"}";

        mvc.perform(post("/api/v1/quality-checks").header("Authorization", authorization)
                        .contentType("application/json").content(body)).andExpect(status().isForbidden());

        Employee employee = employees.saveAndFlush(Employee.builder()
                .userId(technician.getId()).employeeCode("QC-INSPECTOR").canQc(false).build());
        assignments.saveAndFlush(TaskAssignment.builder().taskId(task.getId()).employeeId(employee.getId())
                .assignmentRole(AssignmentRole.QC).startsAt(Instant.now().minusSeconds(60))
                .endsAt(Instant.now().plusSeconds(3600)).assignedBy(technician.getId()).build());
        mvc.perform(post("/api/v1/quality-checks").header("Authorization", authorization)
                        .contentType("application/json").content(body)).andExpect(status().isForbidden());
        org.assertj.core.api.Assertions.assertThat(checks.findAllByRepairOrderIdOrderByCheckedAtDesc(order.getId())).isEmpty();

        employee.setCanQc(true);
        employees.saveAndFlush(employee);
        mvc.perform(post("/api/v1/quality-checks").header("Authorization", authorization)
                        .contentType("application/json").content(body)).andExpect(status().isCreated());
        org.assertj.core.api.Assertions.assertThat(checks.findAllByRepairOrderIdOrderByCheckedAtDesc(order.getId()))
                .hasSize(1);
    }

    private Customer customer(String code, Long userId) {
        Customer customer = new Customer();
        customer.setCustomerCode(code);
        customer.setFullName(code);
        customer.setUserId(userId);
        return customers.saveAndFlush(customer);
    }

    private Vehicle vehicle(String plate, Long customerId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(plate);
        vehicle.setCustomerId(customerId);
        vehicle.setBrand("Toyota");
        vehicle.setModel("Test");
        return vehicles.saveAndFlush(vehicle);
    }

    private Booking booking(String code, Customer customer, Vehicle vehicle) {
        Booking booking = new Booking();
        booking.setBookingCode(code);
        booking.setCustomerId(customer.getId());
        booking.setVehicleId(vehicle.getId());
        booking.setServiceId(1L);
        booking.setAppointmentAt(Instant.now().plusSeconds(3600));
        booking.setExpectedEndAt(Instant.now().plusSeconds(7200));
        return bookings.saveAndFlush(booking);
    }

    private RepairOrder order(String code, Customer customer, Vehicle vehicle, long actorId) {
        return orders.saveAndFlush(RepairOrder.builder().orderCode(code).customerId(customer.getId())
                .vehicleId(vehicle.getId()).odometerIn(1000).receivedBy(actorId).build());
    }

    private VehicleCheck check(long orderId, long actorId) {
        VehicleCheck check = new VehicleCheck();
        check.setRepairOrderId(orderId);
        check.setCheckType(CheckType.CHECK_IN);
        check.setCheckedBy(actorId);
        check.setChecklistJson("{}");
        return checks.saveAndFlush(check);
    }

    private Attachment attachment(long orderId, long checkId, long actorId) {
        Attachment attachment = new Attachment();
        attachment.setRepairOrderId(orderId);
        attachment.setVehicleCheckId(checkId);
        attachment.setStorageKey("scope/attachment/" + orderId);
        attachment.setOriginalFilename("vehicle.jpg");
        attachment.setContentType("image/jpeg");
        attachment.setCategory(AttachmentCategory.CHECK_IN);
        attachment.setUploadedBy(actorId);
        attachment.setSizeBytes(10L);
        return attachments.saveAndFlush(attachment);
    }
}
