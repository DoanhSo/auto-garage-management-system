package com.Group13_65CNTT.AutoGarage.reporting.service;

import com.Group13_65CNTT.AutoGarage.reporting.dto.response.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface ReportService {
    DashboardResponse dashboard(Instant from, Instant until);

    Page<InvoiceBalanceResponse> outstandingInvoices(Pageable pageable);

    byte[] exportDashboard(Instant from, Instant until);
}
