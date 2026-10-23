package com.Group13_65CNTT.AutoGarage.inventory.mapper;

import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryTransactionResponse;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.RepairOrderPartResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryTransaction;
import com.Group13_65CNTT.AutoGarage.inventory.entity.RepairOrderPart;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InventoryTransactionMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "documentItemId", source = "documentItem.id")
    @Mapping(target = "documentId", source = "documentItem.document.id")
    @Mapping(target = "documentCode", source = "documentItem.document.documentCode")
    @Mapping(target = "partId", source = "part.id")
    @Mapping(target = "partSku", source = "part.sku")
    @Mapping(target = "partName", source = "part.partName")
    @Mapping(target = "transactionType", source = "transactionType")
    @Mapping(target = "quantityChange", source = "quantityChange")
    @Mapping(target = "stockAfter", source = "stockAfter")
    @Mapping(target = "unitCost", source = "unitCost")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "createdAt", source = "createdAt")
    InventoryTransactionResponse toResponse(InventoryTransaction transaction);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "repairOrderId", source = "repairOrderId")
    @Mapping(target = "estimateItemId", source = "estimateItemId")
    @Mapping(target = "partId", source = "part.id")
    @Mapping(target = "partSku", source = "part.sku")
    @Mapping(target = "partName", source = "part.partName")
    @Mapping(target = "unit", source = "part.unit")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "unitCost", source = "unitCost")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "version", source = "version")
    RepairOrderPartResponse toRepairOrderPartResponse(RepairOrderPart repairOrderPart);
}
