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
import com.Group13_65CNTT.AutoGarage.reception.repository.BookingRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.AttachmentRepository;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditService;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the isolated TEST_DB_* MySQL database, with Flyway and production isolation/constraints. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@RequiredArgsConstructor
class TV2MySqlIntegrationTest extends IntegrationTestSupport {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final org.springframework.transaction.PlatformTransactionManager transactions;
    private final java.util.List<Fixture> fixtures = new java.util.ArrayList<>();
    private final java.util.List<Long> serviceIds = new java.util.ArrayList<>();
    private final java.util.List<Long> bayIds = new java.util.ArrayList<>();
    private final MockMvc mvc;
    private final AuthProperties auth;
    private final WorkshopProperties workshop;
    private final UserRepository users;
    private final CustomerRepository customers;
    private final VehicleRepository vehicles;
    private final BookingRepository bookings;
    private final AttachmentRepository attachments;
    private final RepairOrderRepository orders;
    private final ServiceRepository services;
    private final ServiceBayRepository bays;
    private final ObjectMapper json;
    @MockitoSpyBean AuditService audit;
    @TempDir Path directory;

    @org.junit.jupiter.api.AfterEach
    void cleanCommittedFixtures() {
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
            for (Fixture activeFixture : fixtures) {
            long vehicleId = activeFixture.vehicle.getId();
            long actorId = activeFixture.actor.getId();
            jdbc.update("DELETE FROM attachments WHERE uploaded_by = ?", actorId);
            jdbc.update("DELETE FROM vehicle_checks WHERE checked_by = ?", actorId);
            jdbc.update("DELETE FROM bay_reservations WHERE booking_id IN (SELECT id FROM bookings WHERE vehicle_id = ?)", vehicleId);
            jdbc.update("DELETE FROM repair_orders WHERE vehicle_id = ?", vehicleId);
            jdbc.update("DELETE FROM bookings WHERE vehicle_id = ?", vehicleId);
            jdbc.update("DELETE FROM vehicles WHERE id = ?", vehicleId);
            jdbc.update("DELETE FROM customers WHERE id = ?", activeFixture.customer.getId());
            jdbc.update("DELETE FROM audit_events WHERE actor_user_id = ?", actorId);
            jdbc.update("DELETE FROM users WHERE id = ?", actorId);
            }
            serviceIds.forEach(id -> jdbc.update("DELETE FROM services WHERE id = ?", id));
            bayIds.forEach(id -> jdbc.update("DELETE FROM service_bays WHERE id = ?", id));
        });
    }

    @Test
    void concurrentBookingsForOneVehicleHaveExactlyOneWinner() throws Exception {
        Fixture fixture = fixture();
        String body = json.writeValueAsString(Map.of("customerId", fixture.customer.getId(),
                "vehicleId", fixture.vehicle.getId(), "appointmentAt", Instant.now().plusSeconds(3600).toString()));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var attempt = (java.util.concurrent.Callable<Integer>) () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Booking start timed out");
                return mvc.perform(post("/api/v1/bookings").header("Authorization", fixture.token)
                        .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
            };
            var first = executor.submit(attempt); var second = executor.submit(attempt);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(bookings.findAllByVehicleId(fixture.vehicle.getId())).hasSize(1);
    }

    @Test
    void concurrentCustomersCompetingForOneBayHaveOneWinner() throws Exception {
        Fixture first = fixture(); Fixture second = fixture();
        ServiceBay bay = bays.saveAndFlush(ServiceBay.builder().bayCode(key()).bayName("Shared bay")
                .bayType("STANDARD").build()); bayIds.add(bay.getId());
        Instant time = Instant.now().plusSeconds(3600);
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var attempts = java.util.List.of(first, second).stream().map(fixture -> executor.submit(() -> {
                String body = json.writeValueAsString(Map.of("customerId", fixture.customer.getId(),
                        "vehicleId", fixture.vehicle.getId(), "bayId", bay.getId(), "appointmentAt", time.toString()));
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Booking start timed out");
                return mvc.perform(post("/api/v1/bookings").header("Authorization", fixture.token)
                        .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(java.util.List.of(attempts.get(0).get(20, TimeUnit.SECONDS), attempts.get(1).get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
    }

    @Test
    void incompatibleBayRollsBackBookingAndRescheduleRestoresOriginalTime() throws Exception {
        Fixture fixture = fixture();
        ServiceEntity service = new ServiceEntity(); service.setServiceCode(key()); service.setServiceName("Lift job");
        service.setStandardMinutes(60); service.setRequiredBayType("LIFT"); services.saveAndFlush(service); serviceIds.add(service.getId());
        ServiceBay wrongBay = bays.saveAndFlush(ServiceBay.builder().bayCode(key()).bayName("Standard bay")
                .bayType("STANDARD").build());
        bayIds.add(wrongBay.getId());
        Instant time = Instant.now().plusSeconds(7200);
        var base = new java.util.HashMap<String,Object>(); base.put("customerId", fixture.customer.getId());
        base.put("vehicleId", fixture.vehicle.getId()); base.put("serviceId", service.getId());
        base.put("appointmentAt", time.toString()); base.put("bayId", wrongBay.getId());
        mvc.perform(post("/api/v1/bookings").header("Authorization", fixture.token).contentType("application/json")
                .content(json.writeValueAsString(base))).andExpect(status().isBadRequest());
        assertThat(bookings.findAllByVehicleId(fixture.vehicle.getId())).isEmpty();
        base.remove("bayId");
        var created = mvc.perform(post("/api/v1/bookings").header("Authorization", fixture.token).contentType("application/json")
                .content(json.writeValueAsString(base))).andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();
        Instant stored = bookings.findById(id).orElseThrow().getAppointmentAt();
        mvc.perform(post("/api/v1/bookings/{id}/reschedule", id).header("Authorization", fixture.token)
                .contentType("application/json").content(json.writeValueAsString(Map.of("newAppointmentAt", time.plusSeconds(3600).toString(),
                        "newBayId", wrongBay.getId())))).andExpect(status().isBadRequest());
        assertThat(bookings.findById(id).orElseThrow().getAppointmentAt()).isEqualTo(stored);
    }

    @Test
    void auditFailureRollsBackAttachmentRowAndRemovesStoredContent() throws Exception {
        Fixture fixture = fixture(); workshop.setAttachmentDirectory(directory);
        RepairOrder order = orders.saveAndFlush(RepairOrder.builder().orderCode(key())
                .customerId(fixture.customer.getId()).vehicleId(fixture.vehicle.getId())
                .receivedBy(fixture.actor.getId()).odometerIn(1000).build());
        AuditService auditTarget = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(audit);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("Test audit outage"))
                .when(auditTarget).recordEvent(anyLong(), eq(AuditConstants.ACTION_ATTACHMENT_UPLOADED),
                        eq(AuditConstants.ENTITY_ATTACHMENT), anyLong(), anyMap());
        mvc.perform(multipart("/api/v1/attachments/upload")
                .file(new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff,0}))
                .param("repairOrderId", order.getId().toString()).param("category", "DIAGNOSIS")
                .header("Authorization", fixture.token)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
        assertThat(attachments.findAllByRepairOrderId(order.getId())).isEmpty();
        try (var files = java.nio.file.Files.walk(directory)) {
            assertThat(files.filter(java.nio.file.Files::isRegularFile)).isEmpty();
        }
    }

    private Fixture fixture() {
        User actor = users.saveAndFlush(User.builder().identityId(UUID.randomUUID().toString()).username(key())
                .fullName("Test receptionist").role(Role.RECEPTIONIST).build());
        Customer customer = new Customer(); customer.setCustomerCode(key()); customer.setFullName("Test customer");
        customers.saveAndFlush(customer);
        Vehicle vehicle = new Vehicle(); vehicle.setCustomerId(customer.getId()); vehicle.setLicensePlate(key());
        vehicle.setBrand("Test"); vehicle.setModel("Test"); vehicles.saveAndFlush(vehicle);
        Fixture fixture = new Fixture(actor, customer, vehicle, "Bearer " + issueKeycloakToken(auth, actor));
        fixtures.add(fixture);
        return fixture;
    }
    private String key() { return "TV2-" + UUID.randomUUID().toString().substring(0, 12); }
    private record Fixture(User actor, Customer customer, Vehicle vehicle, String token) {}
}
