package com.Group13_65CNTT.AutoGarage.inventory.repository;

import com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findBySupplierCode(String supplierCode);

    boolean existsBySupplierCode(String supplierCode);

    @Query("SELECT s FROM Supplier s WHERE (:query IS NULL OR :query = '' " +
           "OR LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.taxCode) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:active IS NULL OR s.isActive = :active)")
    Page<Supplier> searchSuppliers(
            @Param("query") String query,
            @Param("active") Boolean active,
            Pageable pageable);
}
