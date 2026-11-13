package com.Group13_65CNTT.AutoGarage.scheduling.repository;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, Long> {

    @Query("SELECT s FROM EmployeeShift s WHERE s.employeeId = :employeeId AND s.status = 'ACTIVE' " +
            "AND s.startsAt <= :at AND s.endsAt >= :at ORDER BY s.startsAt")
    List<EmployeeShift> findActiveAt(@Param("employeeId") Long employeeId, @Param("at") Instant at);

    @Query("SELECT s FROM EmployeeShift s WHERE s.status = 'ACTIVE' AND s.startsAt >= :from AND s.startsAt < :to " +
            "ORDER BY s.startsAt, s.employeeId")
    List<EmployeeShift> findActiveStartingBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT s FROM EmployeeShift s WHERE s.status = 'ACTIVE' AND s.startsAt <= :at AND s.endsAt >= :at " +
            "ORDER BY s.startsAt")
    List<EmployeeShift> findAllActiveAt(@Param("at") Instant at);

    @Query("SELECT COUNT(s) > 0 FROM EmployeeShift s WHERE s.employeeId = :employeeId AND s.status = 'ACTIVE' " +
            "AND s.startsAt <= :startsAt AND s.endsAt >= :endsAt")
    boolean coversAssignment(@Param("employeeId") Long employeeId, @Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);

    List<EmployeeShift> findByEmployeeIdAndStatus(Long employeeId, ShiftStatus status);

    long countByEmployeeIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
            Long employeeId, ShiftStatus status, java.time.Instant from, java.time.Instant to);

    Page<EmployeeShift> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<EmployeeShift> findByEmployeeIdAndStatus(Long employeeId, ShiftStatus status, Pageable pageable);

    Page<EmployeeShift> findByStatus(ShiftStatus status, Pageable pageable);

    /** Shifts overlapping [from, to), optionally narrowed by employee and status. */
    @Query("SELECT s FROM EmployeeShift s WHERE (:employeeId IS NULL OR s.employeeId = :employeeId) "
            + "AND (:status IS NULL OR s.status = :status) AND s.endsAt > :from AND s.startsAt < :to")
    Page<EmployeeShift> searchShifts(
            @Param("employeeId") Long employeeId,
            @Param("status") ShiftStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT s FROM EmployeeShift s WHERE s.serviceBayId = :bayId AND s.status = 'ACTIVE' " +
           "AND s.startsAt < :to AND s.endsAt > :from ORDER BY s.startsAt, s.employeeId")
    List<EmployeeShift> findActiveByBayBetween(
            @Param("bayId") Long bayId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT s FROM EmployeeShift s WHERE s.serviceBayId = :bayId AND s.status = 'ACTIVE' " +
           "AND s.startsAt < :endsAt AND s.endsAt > :startsAt")
    List<EmployeeShift> findOverlappingByBay(
            @Param("bayId") Long bayId, @Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);

    @Query("SELECT s FROM EmployeeShift s WHERE s.employeeId = :employeeId AND s.status = :status " +
           "AND s.startsAt < :endsAt AND s.endsAt > :startsAt")
    List<EmployeeShift> findOverlappingShifts(
            @Param("employeeId") Long employeeId,
            @Param("status") ShiftStatus status,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt);
}
