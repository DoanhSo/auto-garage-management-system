package com.Group13_65CNTT.AutoGarage.reporting.mapper;

import com.Group13_65CNTT.AutoGarage.reporting.dto.response.*;
import com.Group13_65CNTT.AutoGarage.reporting.entity.InvoiceBalance;
import com.Group13_65CNTT.AutoGarage.reporting.repository.projection.DashboardTotals;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReportMapper {
    @Mapping(target = "from", ignore = true)
    @Mapping(target = "until", ignore = true)
    @Mapping(target = "snapshotAt", ignore = true)
    DashboardResponse toResponse(DashboardTotals totals);

    InvoiceBalanceResponse toResponse(InvoiceBalance balance);
}
