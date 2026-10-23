package com.Group13_65CNTT.AutoGarage.inventory.mapper;

import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryDocumentItemResponse;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryDocumentResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryDocument;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryDocumentItem;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InventoryDocumentMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "documentCode", source = "documentCode")
    @Mapping(target = "documentType", source = "documentType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.supplierName")
    @Mapping(target = "repairOrderId", source = "repairOrderId")
    @Mapping(target = "sourceDocumentId", source = "sourceDocument.id")
    @Mapping(target = "sourceDocumentCode", source = "sourceDocument.documentCode")
    @Mapping(target = "supplierInvoiceNumber", source = "supplierInvoiceNumber")
    @Mapping(target = "supplierInvoiceDate", source = "supplierInvoiceDate")
    @Mapping(target = "dueDate", source = "dueDate")
    @Mapping(target = "subtotal", source = "subtotal")
    @Mapping(target = "taxAmount", source = "taxAmount")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "postedBy", source = "postedBy")
    @Mapping(target = "postedAt", source = "postedAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "items", source = "items")
    InventoryDocumentResponse toResponse(InventoryDocument document);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "lineNo", source = "lineNo")
    @Mapping(target = "partId", source = "part.id")
    @Mapping(target = "partSku", source = "part.sku")
    @Mapping(target = "partName", source = "part.partName")
    @Mapping(target = "unit", source = "part.unit")
    @Mapping(target = "estimateItemId", source = "estimateItemId")
    @Mapping(target = "repairOrderPartId", source = "repairOrderPart.id")
    @Mapping(target = "sourceItemId", source = "sourceItem.id")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitCost", source = "unitCost")
    @Mapping(target = "lineTotal", source = "lineTotal")
    @Mapping(target = "notes", source = "notes")
    InventoryDocumentItemResponse toItemResponse(InventoryDocumentItem item);
}
