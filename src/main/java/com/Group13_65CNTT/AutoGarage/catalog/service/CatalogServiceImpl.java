package com.Group13_65CNTT.AutoGarage.catalog.service;

import com.Group13_65CNTT.AutoGarage.catalog.dto.CreateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse;
import com.Group13_65CNTT.AutoGarage.catalog.dto.UpdateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;
import com.Group13_65CNTT.AutoGarage.catalog.mapper.ServiceMapper;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class CatalogServiceImpl implements CatalogService {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final Random random = new Random();

    public CatalogServiceImpl(ServiceRepository serviceRepository, ServiceMapper serviceMapper) {
        this.serviceRepository = serviceRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public ServiceResponse createService(CreateServiceRequest request) {
        validateServiceRules(request.getUnitPrice(), request.getStandardMinutes(),
                request.getIntervalKm(), request.getIntervalMonths(),
                request.getWarrantyDays(), request.getWarrantyKm());

        if (request.getServiceCode() == null || request.getServiceCode().isBlank()) {
            request.setServiceCode(generateServiceCode());
        } else {
            String code = request.getServiceCode().trim().toUpperCase();
            if (serviceRepository.existsByServiceCode(code)) {
                throw new ConflictException("Mã dịch vụ " + code + " đã tồn tại trong danh mục");
            }
            request.setServiceCode(code);
        }

        ServiceEntity entity = serviceMapper.toEntity(request);
        ServiceEntity saved = serviceRepository.save(entity);
        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ với ID: " + id));
        return serviceMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceByCode(String code) {
        ServiceEntity entity = serviceRepository.findByServiceCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ với mã: " + code));
        return serviceMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> searchServices(String search, ServiceType type, Boolean isActive) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return serviceRepository.searchServices(cleanSearch, type, isActive)
                .stream()
                .map(serviceMapper::toResponse)
                .toList();
    }

    @Override
    public ServiceResponse updateService(Long id, UpdateServiceRequest request) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ với ID: " + id));

        BigDecimal price = request.getUnitPrice() != null ? request.getUnitPrice() : entity.getUnitPrice();
        Integer standardMin = request.getStandardMinutes() != null ? request.getStandardMinutes() : entity.getStandardMinutes();
        Integer intKm = request.getIntervalKm() != null ? request.getIntervalKm() : entity.getIntervalKm();
        Integer intMonths = request.getIntervalMonths() != null ? request.getIntervalMonths() : entity.getIntervalMonths();
        Integer wDays = request.getWarrantyDays() != null ? request.getWarrantyDays() : entity.getWarrantyDays();
        Integer wKm = request.getWarrantyKm() != null ? request.getWarrantyKm() : entity.getWarrantyKm();

        validateServiceRules(price, standardMin, intKm, intMonths, wDays, wKm);

        serviceMapper.updateEntity(entity, request);
        ServiceEntity updated = serviceRepository.save(entity);
        return serviceMapper.toResponse(updated);
    }

    @Override
    public void deactivateService(Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ với ID: " + id));
        entity.setIsActive(false);
        serviceRepository.save(entity);
    }

    private void validateServiceRules(BigDecimal unitPrice, Integer standardMinutes,
                                      Integer intervalKm, Integer intervalMonths,
                                      Integer warrantyDays, Integer warrantyKm) {
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Đơn giá dịch vụ không được âm");
        }
        if (standardMinutes != null && standardMinutes <= 0) {
            throw new BusinessRuleException("Thời gian định mức phải lớn hơn 0 phút");
        }
        if (intervalKm != null && intervalKm <= 0) {
            throw new BusinessRuleException("Chu kỳ số km bảo dưỡng định kỳ phải lớn hơn 0");
        }
        if (intervalMonths != null && intervalMonths <= 0) {
            throw new BusinessRuleException("Chu kỳ số tháng bảo dưỡng định kỳ phải lớn hơn 0");
        }
        if (warrantyDays != null && warrantyDays < 0) {
            throw new BusinessRuleException("Số ngày bảo hành không được âm");
        }
        if (warrantyKm != null && warrantyKm < 0) {
            throw new BusinessRuleException("Số km bảo hành không được âm");
        }
    }

    private String generateServiceCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            int randomPart = 1000 + random.nextInt(9000);
            code = "SRV-" + datePart + "-" + randomPart;
        } while (serviceRepository.existsByServiceCode(code));
        return code;
    }
}
