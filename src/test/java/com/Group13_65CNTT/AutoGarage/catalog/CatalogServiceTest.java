package com.Group13_65CNTT.AutoGarage.catalog;

import com.Group13_65CNTT.AutoGarage.catalog.dto.CreateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;
import com.Group13_65CNTT.AutoGarage.catalog.mapper.ServiceMapper;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
import com.Group13_65CNTT.AutoGarage.catalog.service.CatalogServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @InjectMocks
    private CatalogServiceImpl catalogService;

    private CreateServiceRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateServiceRequest();
        validRequest.setServiceName("Thay dầu động cơ tổng hợp");
        validRequest.setServiceType(ServiceType.SINGLE);
        validRequest.setUnitPrice(BigDecimal.valueOf(350000));
        validRequest.setStandardMinutes(45);
        validRequest.setIntervalKm(5000);
        validRequest.setIntervalMonths(6);
    }

    @Test
    @DisplayName("TV2-04: Tạo dịch vụ thành công với các thông số hợp lệ")
    void testCreateService_Success() {
        when(serviceRepository.existsByServiceCode(any())).thenReturn(false);
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> {
            ServiceEntity s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        ServiceResponse response = catalogService.createService(validRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Thay dầu động cơ tổng hợp", response.getServiceName());
        assertEquals(BigDecimal.valueOf(350000), response.getUnitPrice());
        assertEquals(45, response.getStandardMinutes());
        verify(serviceRepository, times(1)).save(any(ServiceEntity.class));
    }

    @Test
    @DisplayName("TV2-04: Từ chối dịch vụ khi thời gian định mức <= 0")
    void testCreateService_InvalidStandardMinutes() {
        validRequest.setStandardMinutes(0);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> catalogService.createService(validRequest));
        assertTrue(ex.getMessage().contains("phải lớn hơn 0 phút"));
        verify(serviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-04: Từ chối đơn giá âm")
    void testCreateService_NegativePrice() {
        validRequest.setUnitPrice(BigDecimal.valueOf(-1));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> catalogService.createService(validRequest));
        assertTrue(ex.getMessage().contains("không được âm"));
        verify(serviceRepository, never()).save(any());
    }
}
