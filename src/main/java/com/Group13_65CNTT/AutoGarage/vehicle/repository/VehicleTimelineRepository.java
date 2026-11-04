package com.Group13_65CNTT.AutoGarage.vehicle.repository;

import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.billing.entity.Invoice;
import com.Group13_65CNTT.AutoGarage.reception.entity.Attachment;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.response.VehicleWarrantyResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Read-only, batched queries across the source modules; no historical snapshots are mutated. */
@org.springframework.stereotype.Repository
public interface VehicleTimelineRepository extends Repository<Vehicle, Long> {
    @Query("SELECT ro FROM RepairOrder ro WHERE ro.vehicleId = :vehicleId " +
            "AND (:technicianId IS NULL OR EXISTS (SELECT a.id FROM TaskAssignment a " +
            "JOIN RepairTask t ON t.id = a.taskId JOIN Employee e ON e.id = a.employeeId " +
            "WHERE t.repairOrderId = ro.id AND a.isActive = true AND e.isActive = true AND e.userId = :technicianId)) " +
            "ORDER BY ro.receivedAt DESC, ro.id DESC")
    List<RepairOrder> orders(@Param("vehicleId") Long vehicleId, @Param("technicianId") Long technicianId);

    @Query("SELECT t FROM RepairTask t WHERE t.repairOrderId IN :ids AND t.status = 'COMPLETED' ORDER BY t.id")
    List<RepairTask> completedTasks(@Param("ids") List<Long> ids);

    @Query("SELECT i FROM Invoice i WHERE i.repairOrderId IN :ids AND i.status <> 'VOID'")
    List<Invoice> invoices(@Param("ids") List<Long> ids);

    @Query("SELECT a FROM Attachment a WHERE a.repairOrderId IN :ids ORDER BY a.uploadedAt, a.id")
    List<Attachment> attachments(@Param("ids") List<Long> ids);

    @Query("SELECT new com.Group13_65CNTT.AutoGarage.vehicle.dto.response.VehicleWarrantyResponse(" +
            "i.repairOrderId, w.id, w.warrantyCode, w.startsAt, w.expiresAt, w.endOdometer, w.status) " +
            "FROM Warranty w JOIN InvoiceItem item ON item.id = w.invoiceItemId " +
            "JOIN item.invoice i WHERE i.repairOrderId IN :ids ORDER BY w.id")
    List<VehicleWarrantyResponse> warranties(@Param("ids") List<Long> ids);

    @Query("SELECT item.document.repairOrderId AS orderId, item.part.partName AS name FROM InventoryDocumentItem item " +
            "WHERE item.document.repairOrderId IN :ids AND item.document.status = 'POSTED' " +
            "AND item.document.documentType IN ('OUT', 'RETURN') " +
            "GROUP BY item.document.repairOrderId, item.part.id, item.part.partName " +
            "HAVING SUM(CASE WHEN item.document.documentType = 'OUT' THEN item.quantity ELSE -item.quantity END) > 0")
    List<ReplacedPart> parts(@Param("ids") List<Long> ids);

    interface ReplacedPart {
        Long getOrderId();
        String getName();
    }
}
