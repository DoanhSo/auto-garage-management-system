package com.Group13_65CNTT.AutoGarage.billing.mapper;

import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.SupplierBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceBalanceView;
import com.Group13_65CNTT.AutoGarage.billing.entity.SupplierBalanceView;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DebtMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "invoiceId", source = "invoiceId")
    @Mapping(target = "invoiceCode", source = "invoiceCode")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "repairOrderId", source = "repairOrderId")
    @Mapping(target = "issuedAt", source = "issuedAt")
    @Mapping(target = "dueDate", source = "dueDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "paidAmount", source = "paidAmount")
    @Mapping(target = "balanceAmount", source = "balanceAmount")
    InvoiceBalanceResponse toInvoiceBalanceResponse(InvoiceBalanceView view);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "supplierId", source = "supplierId")
    @Mapping(target = "supplierCode", source = "supplierCode")
    @Mapping(target = "supplierName", source = "supplierName")
    @Mapping(target = "purchaseAmount", source = "purchaseAmount")
    @Mapping(target = "paidAmount", source = "paidAmount")
    @Mapping(target = "balanceAmount", source = "balanceAmount")
    SupplierBalanceResponse toSupplierBalanceResponse(SupplierBalanceView view);
}
