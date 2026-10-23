package com.Group13_65CNTT.AutoGarage.inventory.mapper;

import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.SupplierResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SupplierMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "supplierCode", source = "supplierCode")
    @Mapping(target = "supplierName", source = "supplierName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "taxCode", source = "taxCode")
    @Mapping(target = "paymentTermDays", source = "paymentTermDays")
    Supplier toEntity(SupplierCreateRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "supplierCode", source = "supplierCode")
    @Mapping(target = "supplierName", source = "supplierName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "taxCode", source = "taxCode")
    @Mapping(target = "paymentTermDays", source = "paymentTermDays")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "version", source = "version")
    SupplierResponse toResponse(Supplier supplier);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "supplierName", source = "supplierName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "taxCode", source = "taxCode")
    @Mapping(target = "paymentTermDays", source = "paymentTermDays")
    @Mapping(target = "isActive", source = "isActive")
    void updateEntityFromRequest(SupplierUpdateRequest request, @MappingTarget Supplier supplier);
}
