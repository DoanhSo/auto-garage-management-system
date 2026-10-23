package com.Group13_65CNTT.AutoGarage.billing.mapper;

import com.Group13_65CNTT.AutoGarage.billing.dto.response.PaymentResponse;
import com.Group13_65CNTT.AutoGarage.billing.entity.Payment;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PaymentMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "transactionCode", source = "transactionCode")
    @Mapping(target = "direction", source = "direction")
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "inventoryDocumentId", source = "inventoryDocumentId")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "paymentMethod", source = "paymentMethod")
    @Mapping(target = "providerReference", source = "providerReference")
    @Mapping(target = "paidAt", source = "paidAt")
    @Mapping(target = "recordedBy", source = "recordedBy")
    @Mapping(target = "notes", source = "notes")
    PaymentResponse toResponse(Payment payment);
}
