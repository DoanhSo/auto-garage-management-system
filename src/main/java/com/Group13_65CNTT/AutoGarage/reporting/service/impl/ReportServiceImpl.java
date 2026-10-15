package com.Group13_65CNTT.AutoGarage.reporting.service.impl;

import com.Group13_65CNTT.AutoGarage.common.config.ReportProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.reporting.dto.response.*;
import com.Group13_65CNTT.AutoGarage.reporting.mapper.ReportMapper;
import com.Group13_65CNTT.AutoGarage.reporting.repository.ReportRepository;
import com.Group13_65CNTT.AutoGarage.reporting.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {
    private final PaginationPolicy pagination;
    private final ReportRepository repository;
    private final ReportMapper mapper;
    private final Clock clock;
    private final ReportProperties properties;

    @Override
    public DashboardResponse dashboard(Instant from, Instant until) {
        if (from == null
                || until == null
                || !until.isAfter(from)
                || Duration.between(from, until).compareTo(properties.getMaxPeriod()) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        DashboardResponse response = mapper.toResponse(repository.dashboard(from, until));
        response.setFrom(from);
        response.setUntil(until);
        response.setSnapshotAt(clock.instant());
        return response;
    }

    @Override
    public Page<InvoiceBalanceResponse> outstandingInvoices(Pageable pageable) {
        pagination.validate(pageable);
        return repository.outstandingInvoices(pageable).map(mapper::toResponse);
    }

    @Override
    public byte[] exportDashboard(Instant from, Instant until) {
        DashboardResponse report = dashboard(from, until);
        String csv =
                "metric,value\r\nrevenue,"
                        + report.getRevenue().toPlainString()
                        + "\r\ncash_in,"
                        + report.getCashIn().toPlainString()
                        + "\r\ncash_out,"
                        + report.getCashOut().toPlainString()
                        + "\r\ncustomer_debt,"
                        + report.getCustomerDebt().toPlainString()
                        + "\r\nsupplier_debt,"
                        + report.getSupplierDebt().toPlainString()
                        + "\r\nstock_value,"
                        + report.getStockValue().toPlainString()
                        + "\r\nlabor_hours,"
                        + report.getLaborHours().toPlainString()
                        + "\r\n";
        return csv.getBytes(StandardCharsets.UTF_8);
    }
}
