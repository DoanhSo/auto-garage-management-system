package com.Group13_65CNTT.AutoGarage.reception.repository;

import com.Group13_65CNTT.AutoGarage.reception.entity.Booking;
import com.Group13_65CNTT.AutoGarage.reception.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    List<Booking> findAllByCustomerId(Long customerId);

    List<Booking> findAllByVehicleId(Long vehicleId);

    @Query("SELECT b FROM Booking b WHERE " +
           "(:customerId IS NULL OR b.customerId = :customerId) " +
           "AND (:vehicleId IS NULL OR b.vehicleId = :vehicleId) " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:fromTime IS NULL OR b.appointmentAt >= :fromTime) " +
           "AND (:toTime IS NULL OR b.appointmentAt <= :toTime) " +
           "ORDER BY b.appointmentAt DESC")
    List<Booking> searchBookings(
            @Param("customerId") Long customerId,
            @Param("vehicleId") Long vehicleId,
            @Param("status") BookingStatus status,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE " +
           "b.vehicleId = :vehicleId " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND b.appointmentAt < :expectedEndAt " +
           "AND b.expectedEndAt > :appointmentAt " +
           "AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)")
    boolean hasOverlappingBookingForVehicle(
            @Param("vehicleId") Long vehicleId,
            @Param("appointmentAt") Instant appointmentAt,
            @Param("expectedEndAt") Instant expectedEndAt,
            @Param("excludeBookingId") Long excludeBookingId
    );
}
