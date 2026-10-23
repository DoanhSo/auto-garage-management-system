package com.Group13_65CNTT.AutoGarage.estimate.repository;

import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstimateItemRepository extends JpaRepository<EstimateItem, Long> {

    List<EstimateItem> findByEstimateIdOrderByLineNoAsc(Long estimateId);

    List<EstimateItem> findByEstimateIdIn(Collection<Long> estimateIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ei FROM EstimateItem ei WHERE ei.id = :id")
    Optional<EstimateItem> findByIdForUpdate(@Param("id") Long id);
}
