package com.Group13_65CNTT.AutoGarage.reception.service;

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
@Transactional
public class VehicleCheckServiceImpl implements VehicleCheckService {

    private final VehicleCheckRepository vehicleCheckRepository;
    private final AttachmentRepository attachmentRepository;
    private final VehicleCheckMapper vehicleCheckMapper;
    private final CustomerScope customerScope;

    public VehicleCheckServiceImpl(VehicleCheckRepository vehicleCheckRepository,
                                  AttachmentRepository attachmentRepository,
                                  VehicleCheckMapper vehicleCheckMapper, CustomerScope customerScope) {
        this.vehicleCheckRepository = vehicleCheckRepository;
        this.attachmentRepository = attachmentRepository;
        this.vehicleCheckMapper = vehicleCheckMapper;
        this.customerScope = customerScope;
    }

    @Override
    public VehicleCheckResponse recordVehicleCheck(CreateVehicleCheckRequest request) {
        if (request.getRepairOrderId() == null) {
            throw new BusinessRuleException("ID phiếu sửa chữa không được để trống");
        }
        if (request.getCheckType() == null) {
            throw new BusinessRuleException("Loại kiểm tra không được để trống");
        }
        if (request.getChecklistJson() == null || request.getChecklistJson().isBlank()) {
            throw new BusinessRuleException("Nội dung checklist JSON không được để trống");
        }
        if (request.getCheckType() == CheckType.QC) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        customerScope.requireResource(AccessResource.REPAIR_ORDER, request.getRepairOrderId());

        // Validate result constraint
        if (request.getCheckType() == CheckType.CHECK_IN || request.getCheckType() == CheckType.HANDOVER) {
            request.setResult(CheckResult.RECORDED);
        }

        int nextAttemptNo = vehicleCheckRepository.findMaxAttemptNo(request.getRepairOrderId(), request.getCheckType()) + 1;

        VehicleCheck entity = vehicleCheckMapper.toEntity(request, nextAttemptNo);
        VehicleCheck saved = vehicleCheckRepository.save(entity);

        return vehicleCheckMapper.toResponse(saved, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleCheckResponse getVehicleCheckById(Long id) {
        VehicleCheck check = vehicleCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biên bản kiểm tra với ID: " + id));
        customerScope.requireResource(AccessResource.REPAIR_ORDER, check.getRepairOrderId());

        List<AttachmentResponse> attachments = attachmentRepository.findAllByVehicleCheckId(check.getId())
                .stream()
                .map(vehicleCheckMapper::toAttachmentResponse)
                .toList();

        return vehicleCheckMapper.toResponse(check, attachments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleCheckResponse> getChecksByRepairOrder(Long repairOrderId) {
        customerScope.requireResource(AccessResource.REPAIR_ORDER, repairOrderId);
        return vehicleCheckRepository.findAllByRepairOrderIdOrderByCheckedAtDesc(repairOrderId)
                .stream()
                .map(check -> {
                    List<AttachmentResponse> attachments = attachmentRepository.findAllByVehicleCheckId(check.getId())
                            .stream()
                            .map(vehicleCheckMapper::toAttachmentResponse)
                            .toList();
                    return vehicleCheckMapper.toResponse(check, attachments);
                })
                .toList();
    }
}
