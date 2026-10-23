package com.Group13_65CNTT.AutoGarage.billing.mapper;

import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceItemResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceResponse;
import com.Group13_65CNTT.AutoGarage.billing.entity.Invoice;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceItem;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InvoiceMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "invoiceCode", source = "invoiceCode")
    @Mapping(target = "repairOrderId", source = "repairOrderId")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "customerSnapshot", source = "customerSnapshot")
    @Mapping(target = "vehicleSnapshot", source = "vehicleSnapshot")
    @Mapping(target = "subtotal", source = "subtotal")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "taxAmount", source = "taxAmount")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "issuedBy", source = "issuedBy")
    @Mapping(target = "issuedAt", source = "issuedAt")
    @Mapping(target = "dueDate", source = "dueDate")
    @Mapping(target = "voidReason", source = "voidReason")
    @Mapping(target = "voidedBy", source = "voidedBy")
    @Mapping(target = "voidedAt", source = "voidedAt")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "items", source = "items")
    InvoiceResponse toResponse(Invoice invoice);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "lineNo", source = "lineNo")
    @Mapping(target = "itemType", source = "itemType")
    @Mapping(target = "repairTaskId", source = "repairTaskId")
    @Mapping(target = "repairOrderPartId", source = "repairOrderPartId")
    @Mapping(target = "itemName", source = "itemName")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "unitCost", source = "unitCost")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "taxAmount", source = "taxAmount")
    @Mapping(target = "lineTotal", source = "lineTotal")
    @Mapping(target = "warrantyDays", source = "warrantyDays")
    @Mapping(target = "warrantyKm", source = "warrantyKm")
    @Mapping(target = "warrantyTerms", source = "warrantyTerms")
    InvoiceItemResponse toItemResponse(InvoiceItem item);
}
