package com.Group13_65CNTT.AutoGarage.identity.repository;

import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.projection.ResourceOwnership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Read-only projections of module-owned resources for authorization. */
@Repository
public interface AccessRepository extends JpaRepository<User, Long> {
    @Query(
            value =
                    """
                    SELECT user_id ownerId,NULL orderId,NULL vehicleId FROM customers WHERE id=:id
                    """,
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipCustomer(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT c.user_id ownerId,NULL orderId,v.id vehicleId FROM vehicles v JOIN customers c ON c.id=v.customer_id WHERE v.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipVehicle(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT c.user_id ownerId,NULL orderId,b.vehicle_id FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE b.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipBooking(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT c.user_id ownerId,r.id orderId,r.vehicle_id FROM repair_orders r JOIN customers c ON c.id=r.customer_id WHERE r.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipRepairOrder(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT c.user_id ownerId,r.id orderId,r.vehicle_id FROM estimates e JOIN repair_orders r ON r.id=e.repair_order_id JOIN customers c ON c.id=r.customer_id WHERE e.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipEstimate(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT c.user_id ownerId,r.id orderId,r.vehicle_id FROM repair_tasks t JOIN repair_orders r ON r.id=t.repair_order_id JOIN customers c ON c.id=r.customer_id WHERE t.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipTask(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT c.user_id ownerId,i.repair_order_id orderId,r.vehicle_id FROM invoices i JOIN customers c ON c.id=i.customer_id JOIN repair_orders r ON r.id=i.repair_order_id WHERE i.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipInvoice(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT c.user_id ownerId,NULL orderId,w.vehicle_id FROM warranties w JOIN customers c ON c.id=w.customer_id WHERE w.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipWarranty(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT c.user_id ownerId,cl.handling_order_id orderId,w.vehicle_id FROM warranty_claims cl JOIN warranties w ON w.id=cl.warranty_id JOIN customers c ON c.id=w.customer_id WHERE cl.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipWarrantyClaim(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT COALESCE(n.user_id,c.user_id) ownerId,NULL orderId,NULL vehicleId FROM notifications n LEFT JOIN customers c ON c.id=n.customer_id WHERE n.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipNotification(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT COALESCE(c.user_id,wc.user_id) ownerId,
                       COALESCE(a.repair_order_id,cl.handling_order_id) orderId,r.vehicle_id
                FROM attachments a LEFT JOIN repair_orders r ON r.id=a.repair_order_id
                LEFT JOIN customers c ON c.id=r.customer_id
                LEFT JOIN warranty_claims cl ON cl.id=a.warranty_claim_id
                LEFT JOIN warranties w ON w.id=cl.warranty_id
                LEFT JOIN customers wc ON wc.id=w.customer_id WHERE a.id=:id
""",
            nativeQuery = true)
    Optional<ResourceOwnership> ownershipAttachment(@Param("id") Long id);

    @Query(
            value =
                    """
SELECT COUNT(*) FROM task_assignments a JOIN employees e ON e.id=a.employee_id JOIN repair_tasks t ON t.id=a.task_id WHERE e.user_id=:actorId AND e.is_active=TRUE AND a.is_active=TRUE AND t.repair_order_id=:resourceId
""",
            nativeQuery = true)
    long assignedToOrder(@Param("actorId") Long actorId, @Param("resourceId") Long resourceId);

    @Query(
            value =
                    """
SELECT COUNT(*) FROM task_assignments a JOIN employees e ON e.id=a.employee_id JOIN repair_tasks t ON t.id=a.task_id JOIN repair_orders r ON r.id=t.repair_order_id WHERE e.user_id=:actorId AND e.is_active=TRUE AND a.is_active=TRUE AND r.vehicle_id=:resourceId AND r.status NOT IN ('CLOSED','CANCELLED')
""",
            nativeQuery = true)
    long assignedToVehicle(@Param("actorId") Long actorId, @Param("resourceId") Long resourceId);

    @Query(
            value =
                    """
SELECT COUNT(*) FROM task_assignments a JOIN employees e ON e.id=a.employee_id JOIN repair_tasks t ON t.id=a.task_id WHERE e.user_id=:actorId AND e.is_active=TRUE AND e.can_qc=TRUE AND a.is_active=TRUE AND a.assignment_role='QC' AND t.repair_order_id=:resourceId
""",
            nativeQuery = true)
    long assignedToQc(@Param("actorId") Long actorId, @Param("resourceId") Long resourceId);

    @Query(value = "SELECT COUNT(*) FROM task_assignments a JOIN employees e ON e.id=a.employee_id " +
            "WHERE e.user_id=:actorId AND e.is_active=TRUE AND a.is_active=TRUE AND a.assignment_role <> 'QC' AND a.task_id=:taskId", nativeQuery = true)
    long assignedToTask(@Param("actorId") long actorId, @Param("taskId") long taskId);

    @Query(value = "SELECT COUNT(*) FROM task_assignments a JOIN employees e ON e.id=a.employee_id " +
            "WHERE e.user_id=:actorId AND e.is_active=TRUE AND a.is_active=TRUE AND a.assignment_role <> 'QC' AND a.id=:assignmentId", nativeQuery = true)
    long ownsAssignment(@Param("actorId") long actorId, @Param("assignmentId") long assignmentId);
}
