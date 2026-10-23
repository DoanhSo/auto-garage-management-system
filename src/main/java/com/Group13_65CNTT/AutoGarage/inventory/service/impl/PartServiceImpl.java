package com.Group13_65CNTT.AutoGarage.inventory.service.impl;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.PartResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Part;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier;
import com.Group13_65CNTT.AutoGarage.inventory.mapper.PartMapper;
import com.Group13_65CNTT.AutoGarage.inventory.repository.PartRepository;
import com.Group13_65CNTT.AutoGarage.inventory.repository.SupplierRepository;
import com.Group13_65CNTT.AutoGarage.inventory.service.PartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartServiceImpl implements PartService {
    private final PartRepository partRepository;
    private final SupplierRepository supplierRepository;
    private final PartMapper partMapper;

    @Override
    @Transactional
    public PartResponse createPart(PartCreateRequest request) {
        String sku = request.getSku().trim();
        if (partRepository.existsBySku(sku)) {
            log.warn("Part SKU already exists: {}", sku);
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }

        Part part = partMapper.toEntity(request);
        part.setSku(sku);
        part.setPartName(request.getPartName().trim());
        part.setStockQuantity(BigDecimal.ZERO);
        part.setReservedQuantity(BigDecimal.ZERO);

        if (request.getPreferredSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getPreferredSupplierId())
                    .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
            part.setPreferredSupplier(supplier);
        }

        Part saved = partRepository.save(part);
        log.info("Created part with id: {}, SKU: {}", saved.getId(), saved.getSku());
        return partMapper.toResponse(saved);
    }

    @Override
    public PartResponse getPartById(Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return partMapper.toResponse(part);
    }

    @Override
    public PartResponse getPartBySku(String sku) {
        Part part = partRepository.findBySku(sku.trim())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return partMapper.toResponse(part);
    }

    @Override
    public Page<PartResponse> searchParts(String query, Long supplierId, Boolean active, Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return partRepository.searchParts(cleanQuery, supplierId, active, pageable)
                .map(partMapper::toResponse);
    }

    @Override
    @Transactional
    public PartResponse updatePart(Long id, PartUpdateRequest request) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (!Objects.equals(part.getVersion(), request.getVersion())) {
            log.warn("Optimistic lock conflict on part {}: entity version {}, request version {}",
                    id, part.getVersion(), request.getVersion());
            throw new BusinessException(ResponseCode.CONCURRENT_MODIFICATION);
        }

        partMapper.updateEntityFromRequest(request, part);
        part.setPartName(request.getPartName().trim());

        if (request.getPreferredSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getPreferredSupplierId())
                    .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
            part.setPreferredSupplier(supplier);
        } else {
            part.setPreferredSupplier(null);
        }

        // Maintain stock and reserved invariants - do not mutate stock directly through CRUD
        Part updated = partRepository.save(part);
        log.info("Updated part id: {}", updated.getId());
        return partMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deactivatePart(Long id) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        part.setIsActive(false);
        partRepository.save(part);
        log.info("Deactivated part id: {}", id);
    }

    @Override
    public List<PartResponse> getLowStockParts() {
        return partRepository.findPartsBelowReorderLevel().stream()
                .map(partMapper::toResponse)
                .toList();
    }
}
