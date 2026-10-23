package com.Group13_65CNTT.AutoGarage.scheduling.repository;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.BayReservation;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface BayReservationRepository extends JpaRepository<BayReservation, Long> {

    List<BayReservation> findByBayId(Long bayId);

    Page<BayReservation> findByBayId(Long bayId, Pageable pageable);

    List<BayReservation> findByBookingIdAndStatusIn(Long bookingId, Collection<ReservationStatus> statuses);

    List<BayReservation> findByRepairOrderIdAndStatusIn(Long repairOrderId, Collection<ReservationStatus> statuses);

    @Query("SELECT r FROM BayReservation r WHERE r.bayId = :bayId AND r.status IN :statuses " +
           "AND r.startsAt < :endsAt AND r.endsAt > :startsAt")
    List<BayReservation> findOverlappingReservations(
            @Param("bayId") Long bayId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt);
}
