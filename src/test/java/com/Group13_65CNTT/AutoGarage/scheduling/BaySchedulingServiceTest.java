package com.Group13_65CNTT.AutoGarage.scheduling;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.BayReservation;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationType;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.BayReservationRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.impl.SchedulingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaySchedulingServiceTest {
    @org.mockito.Spy private com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties workshop = new com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties();

    @Mock
    private ServiceBayRepository serviceBayRepository;

    @Mock
    private BayReservationRepository bayReservationRepository;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    private ServiceBay bay;
    private Instant now;
    private Instant startsAt;
    private Instant endsAt;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        startsAt = now.plus(Duration.ofHours(1));
        endsAt = now.plus(Duration.ofHours(3));

        bay = ServiceBay.builder()
                .id(1L)
                .bayCode("BAY-01")
                .bayName("Khoang 01")
                .bayType("GENERAL")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("I02: Giữ khoang cho lịch hẹn (reserveBooking) thành công")
    void reserveBooking_success() {
        workshop.setReservationHold(Duration.ofMinutes(5));
        when(serviceBayRepository.findByIdWithLock(1L)).thenReturn(Optional.of(bay));
        when(bayReservationRepository.findOverlappingReservations(eq(1L), anyCollection(), eq(startsAt), eq(endsAt)))
                .thenReturn(Collections.emptyList());

        BayReservation saved = BayReservation.builder()
                .id(100L)
                .bayId(1L)
                .bookingId(10L)
                .reservationType(ReservationType.BOOKING)
                .status(ReservationStatus.HELD)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        when(bayReservationRepository.save(any(BayReservation.class))).thenReturn(saved);

        long reservationId = schedulingService.reserveBooking(1L, 10L, 1L, startsAt, endsAt);

        assertThat(reservationId).isEqualTo(100L);
        verify(bayReservationRepository).save(argThat(r ->
                r.getBayId().equals(1L) &&
                r.getBookingId().equals(10L) &&
                r.getReservationType() == ReservationType.BOOKING &&
                r.getStatus() == ReservationStatus.HELD
                && r.getExpiresAt().isAfter(now.plus(Duration.ofMinutes(4)))
                && r.getExpiresAt().isBefore(Instant.now().plus(Duration.ofMinutes(6)))
        ));
    }

    @Test
    @DisplayName("I02: Giữ khoang thất bại khi khoảng thời gian không hợp lệ (endsAt <= startsAt)")
    void reserveBooking_endsBeforeStarts_throwsBadRequest() {
        assertThatThrownBy(() -> schedulingService.reserveBooking(1L, 10L, 1L, endsAt, startsAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.BAD_REQUEST);

        verify(serviceBayRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("I02: Hai yêu cầu tranh một khoang (UC07/UC10) - Yêu cầu sau bị từ chối khi khoang đang HELD")
    void reserveBooking_overlappingHeld_throwsDuplicate() {
        BayReservation existingHeld = BayReservation.builder()
                .id(99L)
                .bayId(1L)
                .bookingId(5L)
                .status(ReservationStatus.HELD)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .expiresAt(now.plus(Duration.ofMinutes(15)))
                .build();

        when(serviceBayRepository.findByIdWithLock(1L)).thenReturn(Optional.of(bay));
        when(bayReservationRepository.findOverlappingReservations(eq(1L), anyCollection(), eq(startsAt), eq(endsAt)))
                .thenReturn(List.of(existingHeld));

        assertThatThrownBy(() -> schedulingService.reserveBooking(1L, 10L, 1L, startsAt, endsAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);

        verify(bayReservationRepository, never()).save(argThat(r -> r.getBookingId() != null && r.getBookingId().equals(10L)));
    }

    @Test
    @DisplayName("I02: Giữ khoang thành công nếu bản giữ trước đó đã quá hạn (expiresAt < now)")
    void reserveBooking_expiredHeld_allowsNewReservation() {
        BayReservation expiredHeld = BayReservation.builder()
                .id(99L)
                .bayId(1L)
                .bookingId(5L)
                .status(ReservationStatus.HELD)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .expiresAt(now.minus(Duration.ofMinutes(5)))
                .build();

        when(serviceBayRepository.findByIdWithLock(1L)).thenReturn(Optional.of(bay));
        when(bayReservationRepository.findOverlappingReservations(eq(1L), anyCollection(), eq(startsAt), eq(endsAt)))
                .thenReturn(List.of(expiredHeld));

        BayReservation saved = BayReservation.builder()
                .id(101L)
                .bayId(1L)
                .bookingId(10L)
                .reservationType(ReservationType.BOOKING)
                .status(ReservationStatus.HELD)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        when(bayReservationRepository.save(any(BayReservation.class))).thenReturn(saved);

        long reservationId = schedulingService.reserveBooking(1L, 10L, 1L, startsAt, endsAt);

        assertThat(reservationId).isEqualTo(101L);
        assertThat(expiredHeld.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(bayReservationRepository).save(expiredHeld);
    }

    @Test
    @DisplayName("I02: Giữ khoang cho phiếu sửa chữa (reserveRepair) thành công")
    void reserveRepair_success() {
        when(serviceBayRepository.findByIdWithLock(1L)).thenReturn(Optional.of(bay));
        when(bayReservationRepository.findOverlappingReservations(eq(1L), anyCollection(), eq(startsAt), eq(endsAt)))
                .thenReturn(Collections.emptyList());

        BayReservation saved = BayReservation.builder()
                .id(200L)
                .bayId(1L)
                .repairOrderId(50L)
                .reservationType(ReservationType.REPAIR)
                .status(ReservationStatus.IN_USE)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        when(bayReservationRepository.save(any(BayReservation.class))).thenReturn(saved);

        long reservationId = schedulingService.reserveRepair(1L, 50L, 1L, startsAt, endsAt);

        assertThat(reservationId).isEqualTo(200L);
        verify(bayReservationRepository).save(argThat(r ->
                r.getBayId().equals(1L) &&
                r.getRepairOrderId().equals(50L) &&
                r.getReservationType() == ReservationType.REPAIR &&
                r.getStatus() == ReservationStatus.IN_USE
        ));
    }

    @Test
    @DisplayName("I02: Giải phóng khoang cho lịch hẹn (releaseForBooking) thành công")
    void releaseForBooking_success() {
        BayReservation reservation = BayReservation.builder()
                .id(100L)
                .bookingId(10L)
                .status(ReservationStatus.HELD)
                .build();

        List<BayReservation> list = new ArrayList<>(List.of(reservation));
        when(bayReservationRepository.findByBookingIdAndStatusIn(eq(10L), anyCollection())).thenReturn(list);

        schedulingService.releaseForBooking(1L, 10L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(reservation.getReleasedAt()).isNotNull();
        verify(bayReservationRepository).save(reservation);
    }

    @Test
    @DisplayName("I02: Giải phóng khoang cho phiếu sửa chữa (releaseForRepair) thành công")
    void releaseForRepair_success() {
        BayReservation reservation = BayReservation.builder()
                .id(200L)
                .repairOrderId(50L)
                .status(ReservationStatus.IN_USE)
                .build();

        List<BayReservation> list = new ArrayList<>(List.of(reservation));
        when(bayReservationRepository.findByRepairOrderIdAndStatusIn(eq(50L), anyCollection())).thenReturn(list);

        schedulingService.releaseForRepair(1L, 50L);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(reservation.getReleasedAt()).isNotNull();
        verify(bayReservationRepository).save(reservation);
    }
}
