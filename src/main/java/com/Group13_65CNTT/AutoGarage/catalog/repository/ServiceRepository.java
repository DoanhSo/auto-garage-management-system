package com.Group13_65CNTT.AutoGarage.catalog.repository;

import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    Optional<ServiceEntity> findByServiceCode(String serviceCode);

    boolean existsByServiceCode(String serviceCode);

    @Query("SELECT s FROM ServiceEntity s WHERE " +
           "(:search IS NULL OR LOWER(s.serviceName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.serviceCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:serviceType IS NULL OR s.serviceType = :serviceType) " +
           "AND (:isActive IS NULL OR s.isActive = :isActive) " +
           "ORDER BY s.id DESC")
    List<ServiceEntity> searchServices(
            @Param("search") String search,
            @Param("serviceType") ServiceType serviceType,
            @Param("isActive") Boolean isActive
    );
}
