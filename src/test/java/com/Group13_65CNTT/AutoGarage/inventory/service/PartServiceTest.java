package com.Group13_65CNTT.AutoGarage.inventory.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.common.config.PaginationProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.PartResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Part;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier;
import com.Group13_65CNTT.AutoGarage.inventory.mapper.PartMapper;
import com.Group13_65CNTT.AutoGarage.inventory.repository.PartRepository;
import com.Group13_65CNTT.AutoGarage.inventory.repository.SupplierRepository;
import com.Group13_65CNTT.AutoGarage.inventory.service.impl.PartServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartService unit tests")
class PartServiceTest {

    @Mock PartRepository partRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock PartMapper partMapper;
    @Spy PaginationPolicy pagination = new PaginationPolicy(new PaginationProperties());
    @InjectMocks PartServiceImpl service;

    @Test
    void createPart_Success() {
        PartCreateRequest request = PartCreateRequest.builder()
                .sku("OIL-5W30").partName("Castrol Magnatec 5W30").unit("cái")
                .purchasePrice(new BigDecimal("150000")).sellingPrice(new BigDecimal("220000"))
                .reorderLevel(new BigDecimal("5")).preferredSupplierId(1L)
                .build();
        Part part = Part.builder().id(10L).sku("OIL-5W30").partName("Castrol Magnatec 5W30").isActive(true).build();
        PartResponse expected = PartResponse.builder().id(10L).sku("OIL-5W30").partName("Castrol Magnatec 5W30").build();

        when(partRepository.existsBySku("OIL-5W30")).thenReturn(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(Supplier.builder().id(1L).build()));
        when(partMapper.toEntity(request)).thenReturn(part);
        when(partRepository.save(part)).thenReturn(part);
        when(partMapper.toResponse(part)).thenReturn(expected);

        PartResponse actual = service.createPart(request);

        assertThat(actual.getSku()).isEqualTo("OIL-5W30");
        verify(partRepository).save(part);
    }

    @Test
    void createPart_DuplicateSku_ThrowsConflict() {
        PartCreateRequest request = PartCreateRequest.builder()
                .sku("OIL-5W30").partName("Castrol Magnatec 5W30").unit("cái")
                .purchasePrice(new BigDecimal("150000")).sellingPrice(new BigDecimal("220000"))
                .reorderLevel(new BigDecimal("5")).preferredSupplierId(1L)
                .build();
        when(partRepository.existsBySku("OIL-5W30")).thenReturn(true);

        assertThatThrownBy(() -> service.createPart(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);
    }

    @Test
    void updatePart_Success() {
        Part part = Part.builder().id(10L).sku("OIL-5W30").partName("Castrol").version(1L).isActive(true).build();
        PartUpdateRequest request = PartUpdateRequest.builder()
                .partName("Castrol Magnatec Pro").unit("can")
                .purchasePrice(new BigDecimal("160000")).sellingPrice(new BigDecimal("230000"))
                .reorderLevel(new BigDecimal("5")).preferredSupplierId(1L).isActive(true).version(1L)
                .build();
        PartResponse response = PartResponse.builder().id(10L).sku("OIL-5W30").partName("Castrol Magnatec Pro").build();

        when(partRepository.findById(10L)).thenReturn(Optional.of(part));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(Supplier.builder().id(1L).build()));
        when(partRepository.save(part)).thenReturn(part);
        when(partMapper.toResponse(part)).thenReturn(response);

        PartResponse actual = service.updatePart(10L, request);

        assertThat(actual.getPartName()).isEqualTo("Castrol Magnatec Pro");
        verify(partMapper).updateEntityFromRequest(request, part);
    }

    @Test
    void listLowStockParts_ReturnsMatchingParts() {
        Part part = Part.builder().id(10L).sku("OIL-5W30").stockQuantity(new BigDecimal("2")).reorderLevel(new BigDecimal("5")).build();
        when(partRepository.findPartsBelowReorderLevel()).thenReturn(List.of(part));
        when(partMapper.toResponse(part)).thenReturn(PartResponse.builder().id(10L).sku("OIL-5W30").stockQuantity(new BigDecimal("2")).build());

        List<PartResponse> result = service.getLowStockParts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockQuantity()).isEqualTo(new BigDecimal("2"));
    }
}
