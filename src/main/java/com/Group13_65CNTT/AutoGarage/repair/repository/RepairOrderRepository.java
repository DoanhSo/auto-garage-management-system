package com.Group13_65CNTT.AutoGarage.repair.repository;

import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    Optional<RepairOrder> findByOrderCode(String orderCode);

    Optional<RepairOrder> findByBookingId(Long bookingId);

    boolean existsByOrderCode(String orderCode);

    boolean existsByBookingId(Long bookingId);

    Page<RepairOrder> findByCustomerId(Long customerId, Pageable pageable);

    Page<RepairOrder> findByVehicleId(Long vehicleId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ro FROM RepairOrder ro WHERE ro.id = :id")
    Optional<RepairOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT ro FROM RepairOrder ro WHERE " +
           "(:status IS NULL OR ro.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(ro.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(ro.customerComplaint) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<RepairOrder> searchOrders(@Param("keyword") String keyword,
                                   @Param("status") RepairOrderStatus status,
                                   Pageable pageable);
}
