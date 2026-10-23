package com.Group13_65CNTT.AutoGarage.scheduling.repository;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
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
public interface ServiceBayRepository extends JpaRepository<ServiceBay, Long> {

    Optional<ServiceBay> findByBayCode(String bayCode);

    boolean existsByBayCode(String bayCode);

    Page<ServiceBay> findByIsActive(Boolean isActive, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ServiceBay b WHERE b.id = :id")
    Optional<ServiceBay> findByIdWithLock(@Param("id") Long id);
}
