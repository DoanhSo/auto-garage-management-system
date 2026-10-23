package com.Group13_65CNTT.AutoGarage.inventory.repository;

import com.Group13_65CNTT.AutoGarage.inventory.entity.Part;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findBySku(String sku);

    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Part p WHERE p.id = :id")
    Optional<Part> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Part p WHERE p.id IN :ids ORDER BY p.id ASC")
    List<Part> findAllByIdInOrderByIdAscWithLock(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Part p WHERE (:query IS NULL OR :query = '' " +
           "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.partName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:supplierId IS NULL OR p.preferredSupplier.id = :supplierId) " +
           "AND (:active IS NULL OR p.isActive = :active)")
    Page<Part> searchParts(
            @Param("query") String query,
            @Param("supplierId") Long supplierId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("SELECT p FROM Part p WHERE p.stockQuantity <= p.reorderLevel AND p.isActive = true")
    List<Part> findPartsBelowReorderLevel();
}
