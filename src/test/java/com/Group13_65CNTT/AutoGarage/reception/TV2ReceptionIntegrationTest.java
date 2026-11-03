package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.vehicle.repository.VehicleRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.AttachmentRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.billing.entity.Invoice;
import com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@Transactional
@RequiredArgsConstructor
class TV2ReceptionIntegrationTest extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final AuthProperties auth;
    private final WorkshopProperties workshop;
    private final UserRepository users;
    private final CustomerRepository customers;
    private final VehicleRepository vehicles;
    private final RepairOrderRepository orders;
    private final AttachmentRepository attachments;
    private final BookingRepository bookings;
    private final InvoiceRepository invoices;
    private final ObjectMapper json;
    @TempDir Path directory;
    private static final byte[] JPEG = {(byte)0xff, (byte)0xd8, (byte)0xff, 0x00};

    @Test
    void bookingCheckInChecklistUploadAndDownloadRunThroughRealServices() throws Exception {
        workshop.setAttachmentDirectory(directory);
        User receptionist = user(Role.RECEPTIONIST);
        String token = token(receptionist);
        long customer = create("/customers", Map.of("fullName", "Walk in", "phone", "0912345678"), token).path("id").asLong();
        long vehicle = create("/vehicles", Map.of("customerId", customer, "licensePlate", "51H-123.45",
                "brand", "Toyota", "model", "Vios", "odometer", 1000), token).path("id").asLong();
        long booking = create("/bookings", Map.of("customerId", customer, "vehicleId", vehicle,
                "appointmentAt", Instant.now().plusSeconds(3600).toString()), token).path("id").asLong();
        long order = create("/reception/check-in", Map.of("bookingId", booking, "vehicleId", vehicle,
                "odometerIn", 1100), token).path("repairOrderId").asLong();
        assertThat(bookings.findById(booking).orElseThrow().getStatus().name()).isEqualTo("RECEIVED");
        assertThat(orders.findById(order)).isPresent();
        assertThat(vehicles.findById(vehicle).orElseThrow().getOdometer()).isEqualTo(1100);
        long check = create("/vehicle-checks", Map.of("repairOrderId", order, "checkType", "CHECK_IN",
                "checklistJson", "{\"exterior\":\"Normal\"}"), token).path("id").asLong();
        var result = mvc.perform(multipart("/api/v1/attachments/upload")
                .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG))
                .param("repairOrderId", Long.toString(order)).param("vehicleCheckId", Long.toString(check))
                .param("category", "CHECK_IN").header("Authorization", token))
                .andExpect(status().isCreated()).andReturn();
        long attachment = json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(get("/api/v1/attachments/{id}/content", attachment).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(content().bytes(JPEG));
        assertThat(attachments.findById(attachment).orElseThrow().getUploadedBy()).isEqualTo(receptionist.getId());
        mvc.perform(post("/api/v1/reception/check-in").header("Authorization", token)
                .contentType("application/json").content(json.writeValueAsString(Map.of("bookingId", booking,
                        "vehicleId", vehicle, "odometerIn", 1100))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unassignedTechnicianCannotAttachEvidenceToAnotherOrder() throws Exception {
        workshop.setAttachmentDirectory(directory);
        User receptionist = user(Role.RECEPTIONIST);
        User technician = user(Role.TECHNICIAN);
        Customer customer = customer(null);
        Vehicle vehicle = vehicle(customer);
        RepairOrder order = order(customer, vehicle, receptionist);
        mvc.perform(multipart("/api/v1/attachments/upload")
                .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG))
                .param("repairOrderId", order.getId().toString()).param("category", "DIAGNOSIS")
                .header("Authorization", token(technician)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(3002))
                .andExpect(jsonPath("$.success").value(false));
        assertThat(attachments.findAllByRepairOrderId(order.getId())).isEmpty();
        try (var files = java.nio.file.Files.list(directory)) { assertThat(files).isEmpty(); }
    }

    @Test
    void transferredVehicleShowsTechnicalHistoryButHidesPreviousOwnersFinance() throws Exception {
        User receptionist = user(Role.RECEPTIONIST);
        User buyer = user(Role.CUSTOMER);
        Customer seller = customer(null);
        Customer newOwner = customer(buyer);
        Vehicle vehicle = vehicle(seller);
        RepairOrder order = order(seller, vehicle, receptionist);
        order.setDiagnosis("Brake inspection"); orders.saveAndFlush(order);
        invoices.saveAndFlush(Invoice.builder().invoiceCode("INV-" + UUID.randomUUID().toString().substring(0, 8))
                .repairOrderId(order.getId()).customerId(seller.getId()).customerSnapshot("{}")
                .vehicleSnapshot("{}").totalAmount(new BigDecimal("123456")).issuedBy(receptionist.getId()).build());
        mvc.perform(post("/api/v1/vehicles/{id}/transfer", vehicle.getId()).header("Authorization", token(receptionist))
                .contentType("application/json").content(json.writeValueAsString(Map.of("newCustomerId", newOwner.getId()))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/vehicles/{id}/timeline", vehicle.getId()).param("role", "ADMIN")
                .param("requestingCustomerId", seller.getId().toString()).header("Authorization", token(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.historyEntries.length()").value(1))
                .andExpect(jsonPath("$.data.historyEntries[0].diagnosis").value("Brake inspection"))
                .andExpect(jsonPath("$.data.historyEntries[0].totalAmount").isEmpty())
                .andExpect(jsonPath("$.data.historyEntries[0].orderCustomerId").isEmpty())
                .andExpect(jsonPath("$.data.historyEntries[0].technicalAttachments").isEmpty());
        assertThat(orders.findById(order.getId()).orElseThrow().getCustomerId()).isEqualTo(seller.getId());
        mvc.perform(get("/api/v1/vehicles/{id}/timeline", vehicle.getId()).header("Authorization", token(receptionist)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.historyEntries[0].totalAmount").value(123456));
    }

    @Test
    void invalidChecklistAndFutureNoShowReturnStandardErrors() throws Exception {
        User receptionist = user(Role.RECEPTIONIST);
        Customer customer = customer(null); Vehicle vehicle = vehicle(customer);
        RepairOrder order = order(customer, vehicle, receptionist);
        mvc.perform(post("/api/v1/vehicle-checks").header("Authorization", token(receptionist))
                .contentType("application/json").content(json.writeValueAsString(Map.of("repairOrderId", order.getId(),
                        "checkType", "CHECK_IN", "checklistJson", "[1]"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        long id = create("/bookings", Map.of("customerId", customer.getId(), "vehicleId", vehicle.getId(),
                "appointmentAt", Instant.now().plusSeconds(3600).toString()), token(receptionist)).path("id").asLong();
        mvc.perform(post("/api/v1/bookings/{id}/no-show", id).header("Authorization", token(receptionist)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        assertThat(bookings.findById(id).orElseThrow().getStatus().name()).isEqualTo("PENDING");
    }

    private JsonNode create(String path, Object body, String token) throws Exception {
        var response = mvc.perform(post("/api/v1" + path).header("Authorization", token)
                .contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).path("data");
    }
    private User user(Role role) {
        String key = UUID.randomUUID().toString();
        return users.saveAndFlush(User.builder().identityId(key).username("test-" + key)
                .fullName("Test user").role(role).build());
    }
    private String token(User user) { return "Bearer " + issueKeycloakToken(auth, user); }
    private Customer customer(User user) {
        Customer customer = new Customer(); customer.setCustomerCode("C-" + UUID.randomUUID().toString().substring(0, 8));
        customer.setFullName("Customer"); if (user != null) customer.setUserId(user.getId());
        return customers.saveAndFlush(customer);
    }
    private Vehicle vehicle(Customer owner) {
        Vehicle vehicle = new Vehicle(); vehicle.setCustomerId(owner.getId());
        vehicle.setLicensePlate(UUID.randomUUID().toString().substring(0, 10)); vehicle.setBrand("Toyota"); vehicle.setModel("Vios");
        return vehicles.saveAndFlush(vehicle);
    }
    private RepairOrder order(Customer owner, Vehicle vehicle, User actor) {
        return orders.saveAndFlush(RepairOrder.builder().orderCode("RO-" + UUID.randomUUID().toString().substring(0, 8))
                .customerId(owner.getId()).vehicleId(vehicle.getId()).receivedBy(actor.getId()).odometerIn(1000).build());
    }
}
