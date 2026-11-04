package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;

import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateVehicleCheckRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.VehicleCheckResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import com.Group13_65CNTT.AutoGarage.reception.mapper.VehicleCheckMapper;
import com.Group13_65CNTT.AutoGarage.reception.repository.AttachmentRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CustomerScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class VehicleCheckServiceImpl implements VehicleCheckService {

    private final VehicleCheckRepository vehicleCheckRepository;
    private final AttachmentRepository attachmentRepository;
    private final VehicleCheckMapper vehicleCheckMapper;
    private final CustomerScope customerScope;

    private final com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository repairOrders;
    private final tools.jackson.databind.ObjectMapper json;

    @Override
    @Audited(action = AuditConstants.ACTION_VEHICLE_CHECK_RECORDED, entity = AuditConstants.ENTITY_VEHICLE_CHECK)
    public VehicleCheckResponse recordVehicleCheck(CreateVehicleCheckRequest request) {
        customerScope.requireFrontDesk();
        customerScope.requireActor(request.getCheckedBy());
        if (request.getRepairOrderId() == null) {
            throw new BusinessRuleException("ID phiếu sửa chữa không được để trống");
        }
        if (request.getCheckType() == null) {
            throw new BusinessRuleException("Loại kiểm tra không được để trống");
        }
        if (request.getChecklistJson() == null || request.getChecklistJson().isBlank()) {
            throw new BusinessRuleException("Nội dung checklist JSON không được để trống");
        }
        if (request.getCheckType() != CheckType.CHECK_IN) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        customerScope.requireResource(AccessResource.REPAIR_ORDER, request.getRepairOrderId());
        validateChecklist(request);
        var order = repairOrders.findByIdForUpdate(request.getRepairOrderId())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        if (order.getStatus() != com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus.NEW) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Chỉ ghi nhận hiện trạng khi xe vừa được tiếp nhận");
        }


        // Validate result constraint
        if (request.getCheckType() == CheckType.CHECK_IN || request.getCheckType() == CheckType.HANDOVER) {
            request.setResult(CheckResult.RECORDED);
        }

        int nextAttemptNo = vehicleCheckRepository.findMaxAttemptNo(request.getRepairOrderId(), request.getCheckType()) + 1;

        VehicleCheck entity = vehicleCheckMapper.toEntity(request, nextAttemptNo);
        VehicleCheck saved = vehicleCheckRepository.save(entity);

        return vehicleCheckMapper.toResponse(saved, List.of());
    }

    private void validateChecklist(CreateVehicleCheckRequest request) {
        if (!Integer.valueOf(1).equals(request.getSchemaVersion())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Phiên bản biên bản không được hỗ trợ");
        }
        try {
            var node = json.readTree(request.getChecklistJson());
            if (node == null || !node.isObject() || node.isEmpty()) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "Nội dung biên bản phải có các mục kiểm tra");
            }
        } catch (tools.jackson.core.JacksonException failure) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, failure);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleCheckResponse getVehicleCheckById(Long id) {
        VehicleCheck check = vehicleCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biên bản kiểm tra với ID: " + id));
        customerScope.requireEvidenceAccess(check.getRepairOrderId());

        List<AttachmentResponse> attachments = attachmentRepository.findAllByVehicleCheckId(check.getId())
                .stream()
                .map(vehicleCheckMapper::toAttachmentResponse)
                .toList();

        return vehicleCheckMapper.toResponse(check, attachments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleCheckResponse> getChecksByRepairOrder(Long repairOrderId) {
        customerScope.requireEvidenceAccess(repairOrderId);
        var checks = vehicleCheckRepository.findAllByRepairOrderIdOrderByCheckedAtDesc(repairOrderId);
        if (checks.isEmpty()) return List.of();
        var attachments = attachmentRepository.findAllByVehicleCheckIdIn(checks.stream().map(VehicleCheck::getId).toList())
                .stream().collect(java.util.stream.Collectors.groupingBy(
                        com.Group13_65CNTT.AutoGarage.reception.entity.Attachment::getVehicleCheckId));
        return checks.stream().map(check -> vehicleCheckMapper.toResponse(check,
                attachments.getOrDefault(check.getId(), List.of()).stream()
                        .map(vehicleCheckMapper::toAttachmentResponse).toList())).toList();
    }
}
