package com.Group13_65CNTT.AutoGarage.reception.repository;

import com.Group13_65CNTT.AutoGarage.reception.entity.Attachment;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findAllByRepairOrderId(Long repairOrderId);

    List<Attachment> findAllByVehicleCheckId(Long vehicleCheckId);

    List<Attachment> findAllByRepairOrderIdAndCategory(Long repairOrderId, AttachmentCategory category);

    Optional<Attachment> findByStorageKey(String storageKey);
}
