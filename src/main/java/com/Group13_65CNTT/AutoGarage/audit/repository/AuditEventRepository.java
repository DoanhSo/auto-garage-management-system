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

    /**
     * Filters by actor and exact action, then (when {@code hasQuery}) keeps events whose action or entity
     * type contains the keyword, whose entity id equals it, whose action is one of {@code actions}
     * (labels resolved by the client) or whose actor's name or username contains it.
     * {@code pattern} is a lower-cased LIKE pattern escaped with {@code !}; {@code actions} must not be empty.
     */
    @Query(
            "SELECT a FROM AuditEvent a WHERE (:actorId IS NULL OR a.actorUserId = :actorId) AND"
                    + " (:action IS NULL OR a.action = :action) AND (:hasQuery = false"
                    + " OR LOWER(a.action) LIKE :pattern ESCAPE '!'"
                    + " OR LOWER(a.entityType) LIKE :pattern ESCAPE '!'"
                    + " OR (:entityId IS NOT NULL AND a.entityId = :entityId)"
                    + " OR a.action IN :actions"
                    + " OR a.actorUserId IN (SELECT u.id FROM User u WHERE LOWER(u.fullName) LIKE :pattern"
                    + " ESCAPE '!' OR LOWER(u.username) LIKE :pattern ESCAPE '!'))")
    Page<AuditEvent> searchEvents(
            @Param("actorId") Long actorId,
            @Param("action") String action,
            @Param("hasQuery") boolean hasQuery,
            @Param("pattern") String pattern,
            @Param("entityId") Long entityId,
            @Param("actions") java.util.Collection<String> actions,
            Pageable pageable);
}
