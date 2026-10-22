package com.Group13_65CNTT.AutoGarage.notification.repository;

import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;
import com.Group13_65CNTT.AutoGarage.notification.repository.projection.ReminderCandidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Notification, Long> {
    @Query(
            value =
                    """
SELECT c.user_id userId,c.id customerId,CONCAT('warranty:',w.id,':expiry') eventKey,
  JSON_OBJECT('type','WARRANTY_EXPIRY','vehicleId',v.id,'warrantyId',w.id,
    'expiresAt',w.expires_at,'endOdometer',w.end_odometer) payloadJson
FROM warranties w JOIN vehicles v ON v.id=w.vehicle_id
JOIN customers c ON c.id=w.customer_id AND c.id=v.customer_id
JOIN users u ON u.id=c.user_id AND u.is_active=TRUE
WHERE w.status='ACTIVE' AND
  ((w.expires_at>=:now AND w.expires_at<=:until)
   OR (w.end_odometer IS NOT NULL AND v.odometer>=w.end_odometer))
AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.event_key=CONCAT('warranty:',w.id,':expiry') AND n.channel='WEB' AND n.recipient_key=CONCAT('user:',c.user_id))
ORDER BY w.id LIMIT :batchSize
""",
            nativeQuery = true)
    List<ReminderCandidate> warrantyReminders(
            @Param("now") Instant now,
            @Param("until") Instant until,
            @Param("batchSize") int batchSize);

    @Query(
            value =
                    """
SELECT c.user_id userId,c.id customerId,CONCAT('maintenance:',ii.id,':',s.id) eventKey,
  JSON_OBJECT('type','MAINTENANCE','vehicleId',v.id,'serviceId',s.id,'serviceName',s.service_name,
    'dueAt',DATE_ADD(r.handed_over_at,INTERVAL s.interval_months MONTH),
    'dueOdometer',COALESCE(r.odometer_out,r.odometer_in)+s.interval_km) payloadJson
FROM invoice_items ii JOIN invoices i ON i.id=ii.invoice_id AND i.status<>'VOID'
JOIN repair_tasks t ON t.id=ii.repair_task_id
JOIN services s ON s.id=t.service_id AND s.is_active=TRUE
JOIN repair_orders r ON r.id=i.repair_order_id AND r.handed_over_at IS NOT NULL
JOIN vehicles v ON v.id=r.vehicle_id
JOIN customers c ON c.id=i.customer_id AND c.id=v.customer_id
JOIN users u ON u.id=c.user_id AND u.is_active=TRUE
WHERE ((s.interval_months IS NOT NULL AND DATE_ADD(r.handed_over_at,INTERVAL s.interval_months MONTH)<=:until)
  OR (s.interval_km IS NOT NULL AND v.odometer>=COALESCE(r.odometer_out,r.odometer_in)+s.interval_km))
AND NOT EXISTS (
  SELECT 1 FROM repair_tasks nt JOIN repair_orders nr ON nr.id=nt.repair_order_id
  JOIN invoice_items ni ON ni.repair_task_id=nt.id JOIN invoices nv ON nv.id=ni.invoice_id AND nv.status<>'VOID'
  WHERE nt.service_id=s.id AND nr.vehicle_id=v.id AND nr.handed_over_at IS NOT NULL
    AND (nr.handed_over_at>r.handed_over_at OR (nr.handed_over_at=r.handed_over_at AND ni.id>ii.id)))
AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.event_key=CONCAT('maintenance:',ii.id,':',s.id) AND n.channel='WEB' AND n.recipient_key=CONCAT('user:',c.user_id))
ORDER BY ii.id LIMIT :batchSize
""",
            nativeQuery = true)
    List<ReminderCandidate> maintenanceReminders(
            @Param("until") Instant until, @Param("batchSize") int batchSize);
}
