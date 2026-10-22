package com.Group13_65CNTT.AutoGarage.reporting.service.impl;

import com.Group13_65CNTT.AutoGarage.common.config.ReportProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.ReportConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.reporting.dto.response.DashboardResponse;
import com.Group13_65CNTT.AutoGarage.reporting.dto.response.InvoiceBalanceResponse;
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
                ReportConstants.CSV_HEADER
                        + ReportConstants.METRIC_REVENUE
                        + report.getRevenue().toPlainString()
                        + ReportConstants.CSV_LINE_ENDING
                        + ReportConstants.METRIC_CASH_IN
                        + report.getCashIn().toPlainString()
                        + ReportConstants.CSV_LINE_ENDING
                        + ReportConstants.METRIC_CASH_OUT
                        + report.getCashOut().toPlainString()
                        + ReportConstants.CSV_LINE_ENDING
                        + ReportConstants.METRIC_CUSTOMER_DEBT
                        + report.getCustomerDebt().toPlainString()
                        + ReportConstants.CSV_LINE_ENDING
                        + ReportConstants.METRIC_SUPPLIER_DEBT
                        + report.getSupplierDebt().toPlainString()
                        + ReportConstants.CSV_LINE_ENDING
                        + ReportConstants.METRIC_STOCK_VALUE
                        + report.getStockValue().toPlainString()
                        + ReportConstants.CSV_LINE_ENDING
                        + ReportConstants.METRIC_LABOR_HOURS
                        + report.getLaborHours().toPlainString()
                        + ReportConstants.CSV_LINE_ENDING;
        return csv.getBytes(StandardCharsets.UTF_8);
    }
}
