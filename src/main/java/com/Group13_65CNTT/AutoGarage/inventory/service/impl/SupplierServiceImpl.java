package com.Group13_65CNTT.AutoGarage.inventory.service.impl;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.SupplierResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier;
import com.Group13_65CNTT.AutoGarage.inventory.mapper.SupplierMapper;
import com.Group13_65CNTT.AutoGarage.inventory.repository.SupplierRepository;
import com.Group13_65CNTT.AutoGarage.inventory.service.SupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {
    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierCreateRequest request) {
        String code = request.getSupplierCode().trim();
        if (supplierRepository.existsBySupplierCode(code)) {
            log.warn("Supplier code already exists: {}", code);
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }

        Supplier entity = supplierMapper.toEntity(request);
        entity.setSupplierCode(code);
        entity.setSupplierName(request.getSupplierName().trim());
        Supplier saved = supplierRepository.save(entity);
        log.info("Created supplier with id: {}, code: {}", saved.getId(), saved.getSupplierCode());
        return supplierMapper.toResponse(saved);
    }

    @Override
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return supplierMapper.toResponse(supplier);
    }

    @Override
    public SupplierResponse getSupplierByCode(String code) {
        Supplier supplier = supplierRepository.findBySupplierCode(code.trim())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return supplierMapper.toResponse(supplier);
    }

    @Override
    public Page<SupplierResponse> searchSuppliers(String query, Boolean active, Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return supplierRepository.searchSuppliers(cleanQuery, active, pageable)
                .map(supplierMapper::toResponse);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierUpdateRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (!Objects.equals(supplier.getVersion(), request.getVersion())) {
            log.warn("Optimistic lock conflict on supplier {}: entity version {}, request version {}",
                    id, supplier.getVersion(), request.getVersion());
            throw new BusinessException(ResponseCode.CONCURRENT_MODIFICATION);
        }

        supplierMapper.updateEntityFromRequest(request, supplier);
        supplier.setSupplierName(request.getSupplierName().trim());
        Supplier updated = supplierRepository.save(supplier);
        log.info("Updated supplier id: {}", updated.getId());
        return supplierMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deactivateSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
        log.info("Deactivated supplier id: {}", id);
    }
}
