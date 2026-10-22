package com.Group13_65CNTT.AutoGarage.vehicle.repository;

import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    Optional<Vehicle> findByVin(String vin);

    List<Vehicle> findAllByCustomerId(Long customerId);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByVin(String vin);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);

    boolean existsByVinAndIdNot(String vin, Long id);

    @Query("SELECT v FROM Vehicle v WHERE " +
           "(:customerId IS NULL OR v.customerId = :customerId) " +
           "AND (:search IS NULL OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.brand) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.vin) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isActive IS NULL OR v.isActive = :isActive) " +
           "ORDER BY v.id DESC")
    List<Vehicle> searchVehicles(
            @Param("customerId") Long customerId,
            @Param("search") String search,
            @Param("isActive") Boolean isActive
    );
}
