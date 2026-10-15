package com.Group13_65CNTT.AutoGarage.audit.repository;

import com.Group13_65CNTT.AutoGarage.audit.entity.AuditEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditEvent> findByRequestKey(String requestKey);

    @Query(
            "SELECT a FROM AuditEvent a WHERE (:actorId IS NULL OR a.actorUserId = :actorId) AND"
                    + " (:action IS NULL OR a.action = :action)")
    Page<AuditEvent> searchEvents(
            @Param("actorId") Long actorId, @Param("action") String action, Pageable pageable);
}
