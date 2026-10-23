package com.Group13_65CNTT.AutoGarage.inventory.mapper;

import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.PartResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Part;

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
public interface PartMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "sku", source = "sku")
    @Mapping(target = "partName", source = "partName")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "purchasePrice", source = "purchasePrice")
    @Mapping(target = "sellingPrice", source = "sellingPrice")
    @Mapping(target = "reorderLevel", source = "reorderLevel")
    @Mapping(target = "warrantyDays", source = "warrantyDays")
    @Mapping(target = "warrantyKm", source = "warrantyKm")
    @Mapping(target = "warrantyTerms", source = "warrantyTerms")
    Part toEntity(PartCreateRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sku", source = "sku")
    @Mapping(target = "partName", source = "partName")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "preferredSupplierId", source = "preferredSupplier.id")
    @Mapping(target = "preferredSupplierName", source = "preferredSupplier.supplierName")
    @Mapping(target = "purchasePrice", source = "purchasePrice")
    @Mapping(target = "sellingPrice", source = "sellingPrice")
    @Mapping(target = "stockQuantity", source = "stockQuantity")
    @Mapping(target = "reservedQuantity", source = "reservedQuantity")
    @Mapping(target = "availableQuantity", expression = "java(part.getAvailableQuantity())")
    @Mapping(target = "reorderLevel", source = "reorderLevel")
    @Mapping(target = "warrantyDays", source = "warrantyDays")
    @Mapping(target = "warrantyKm", source = "warrantyKm")
    @Mapping(target = "warrantyTerms", source = "warrantyTerms")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "version", source = "version")
    PartResponse toResponse(Part part);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "partName", source = "partName")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "purchasePrice", source = "purchasePrice")
    @Mapping(target = "sellingPrice", source = "sellingPrice")
    @Mapping(target = "reorderLevel", source = "reorderLevel")
    @Mapping(target = "warrantyDays", source = "warrantyDays")
    @Mapping(target = "warrantyKm", source = "warrantyKm")
    @Mapping(target = "warrantyTerms", source = "warrantyTerms")
    @Mapping(target = "isActive", source = "isActive")
    void updateEntityFromRequest(PartUpdateRequest request, @MappingTarget Part part);
}
