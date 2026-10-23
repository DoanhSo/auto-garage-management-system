package com.Group13_65CNTT.AutoGarage.inventory.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.common.config.PaginationProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.SupplierResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier;
import com.Group13_65CNTT.AutoGarage.inventory.mapper.SupplierMapper;
import com.Group13_65CNTT.AutoGarage.inventory.repository.SupplierRepository;
import com.Group13_65CNTT.AutoGarage.inventory.service.impl.SupplierServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService unit tests")
class SupplierServiceTest {

    @Mock SupplierRepository supplierRepository;
    @Mock SupplierMapper supplierMapper;
    @Spy PaginationPolicy pagination = new PaginationPolicy(new PaginationProperties());
    @InjectMocks SupplierServiceImpl service;

    @Test
    void createSupplier_Success() {
        SupplierCreateRequest request = new SupplierCreateRequest("SUP-01", "Bosch Vietnam", "0901234567", "bosch@vietnam.com", "Hanoi", "01010101", 30);
        Supplier supplier = Supplier.builder().id(1L).supplierCode("SUP-01").supplierName("Bosch Vietnam").isActive(true).build();
        SupplierResponse expectedResponse = SupplierResponse.builder().id(1L).supplierCode("SUP-01").supplierName("Bosch Vietnam").isActive(true).build();

        when(supplierRepository.existsBySupplierCode("SUP-01")).thenReturn(false);
        when(supplierMapper.toEntity(request)).thenReturn(supplier);
        when(supplierRepository.save(supplier)).thenReturn(supplier);
        when(supplierMapper.toResponse(supplier)).thenReturn(expectedResponse);

        SupplierResponse actual = service.createSupplier(request);

        assertThat(actual).isNotNull();
        assertThat(actual.getSupplierCode()).isEqualTo("SUP-01");
        verify(supplierRepository).save(supplier);
    }

    @Test
    void createSupplier_DuplicateCode_ThrowsConflict() {
        SupplierCreateRequest request = new SupplierCreateRequest("SUP-01", "Bosch Vietnam", null, null, null, null, 0);
        when(supplierRepository.existsBySupplierCode("SUP-01")).thenReturn(true);

        assertThatThrownBy(() -> service.createSupplier(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);
    }

    @Test
    void updateSupplier_Success() {
        Supplier supplier = Supplier.builder().id(1L).supplierCode("SUP-01").supplierName("Old Name").version(0L).isActive(true).build();
        SupplierUpdateRequest request = new SupplierUpdateRequest("New Name", "0988888888", "new@bosch.com", "HCMC", "01010101", 15, true, 0L);
        SupplierResponse response = SupplierResponse.builder().id(1L).supplierCode("SUP-01").supplierName("New Name").isActive(true).build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(supplier)).thenReturn(supplier);
        when(supplierMapper.toResponse(supplier)).thenReturn(response);

        SupplierResponse actual = service.updateSupplier(1L, request);

        assertThat(actual.getSupplierName()).isEqualTo("New Name");
        verify(supplierMapper).updateEntityFromRequest(request, supplier);
    }

    @Test
    void updateSupplier_OptimisticLockingMismatch_ThrowsOptimisticLockingFailure() {
        Supplier supplier = Supplier.builder().id(1L).supplierCode("SUP-01").supplierName("Name").version(2L).isActive(true).build();
        SupplierUpdateRequest request = new SupplierUpdateRequest("New Name", null, null, null, null, 0, true, 1L);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThatThrownBy(() -> service.updateSupplier(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.CONCURRENT_MODIFICATION);
    }

    @Test
    void listSuppliers_ReturnsPage() {
        Supplier supplier = Supplier.builder().id(1L).supplierCode("SUP-01").supplierName("Bosch").isActive(true).build();
        Page<Supplier> page = new PageImpl<>(List.of(supplier));
        SupplierResponse response = SupplierResponse.builder().id(1L).supplierCode("SUP-01").supplierName("Bosch").build();

        when(supplierRepository.searchSuppliers(any(), any(), any(Pageable.class))).thenReturn(page);
        when(supplierMapper.toResponse(supplier)).thenReturn(response);

        Page<SupplierResponse> result = service.searchSuppliers("Bosch", true, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSupplierCode()).isEqualTo("SUP-01");
    }
}
