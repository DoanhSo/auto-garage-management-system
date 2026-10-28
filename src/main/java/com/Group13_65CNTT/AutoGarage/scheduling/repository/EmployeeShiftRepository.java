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

    @Query("SELECT COUNT(s) > 0 FROM EmployeeShift s WHERE s.employeeId = :employeeId AND s.status = 'ACTIVE' " +
            "AND s.startsAt <= :startsAt AND s.endsAt >= :endsAt")
    boolean coversAssignment(@Param("employeeId") Long employeeId, @Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);

    List<EmployeeShift> findByEmployeeIdAndStatus(Long employeeId, ShiftStatus status);

    Page<EmployeeShift> findByEmployeeId(Long employeeId, Pageable pageable);

    @Query("SELECT s FROM EmployeeShift s WHERE s.employeeId = :employeeId AND s.status = :status " +
           "AND s.startsAt < :endsAt AND s.endsAt > :startsAt")
    List<EmployeeShift> findOverlappingShifts(
            @Param("employeeId") Long employeeId,
            @Param("status") ShiftStatus status,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt);
}
