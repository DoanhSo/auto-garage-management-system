package com.Group13_65CNTT.AutoGarage.notification.repository;

import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query(
            "SELECT n FROM Notification n WHERE n.userId = :userId AND n.channel = 'WEB' AND"
                    + " (:unread = false OR n.readAt IS NULL)")
    Page<Notification> searchInbox(
            @Param("userId") long userId, @Param("unread") boolean unread, Pageable pageable);

    @Query(
            """
            SELECT n FROM Notification n
            WHERE (:channel IS NULL OR n.channel = :channel)
              AND (:status IS NULL OR n.status = :status)
              AND (:keyword IS NULL OR LOWER(n.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(n.eventKey) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(n.destination) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Notification> searchAdminNotifications(
            @Param("channel") String channel,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n WHERE n.id = :id")
    Optional<Notification> findLockedById(@Param("id") Long id);

    Optional<Notification> findByEventKeyAndChannelAndRecipientKey(
            String eventKey, String channel, String recipientKey);

    @Query(value = "SELECT COUNT(*) FROM customers WHERE id=:id", nativeQuery = true)
    long customerExists(@Param("id") Long id);

    @Query(value = "SELECT user_id FROM customers WHERE id=:id", nativeQuery = true)
    Long customerUserId(@Param("id") Long id);

    @Modifying
    @Query(
            value =
                    """
INSERT INTO notifications(user_id,customer_id,recipient_key,destination,event_key,channel,subject,payload_json,created_by,updated_by)
VALUES(:userId,:customerId,:recipientKey,:destination,:eventKey,:channel,:subject,CAST(:payload AS JSON),:actorId,:actorId)
ON DUPLICATE KEY UPDATE id=id
""",
            nativeQuery = true)
    void enqueueOnce(
            @Param("userId") Long userId,
            @Param("customerId") Long customerId,
            @Param("recipientKey") String recipientKey,
            @Param("destination") String destination,
            @Param("eventKey") String eventKey,
            @Param("channel") String channel,
            @Param("subject") String subject,
            @Param("payload") String payload,
            @Param("actorId") Long actorId);

    @Query(
            value =
                    """
                    SELECT * FROM notifications WHERE
                      (status='PENDING' AND next_attempt_at<=:now)
                      OR (status='PROCESSING' AND locked_until<=:now)
                    ORDER BY id LIMIT :batchSize FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<Notification> lockDue(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
