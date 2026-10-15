package com.Group13_65CNTT.AutoGarage.reporting.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.common.config.PaginationProperties;
import com.Group13_65CNTT.AutoGarage.common.config.ReportProperties;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.reporting.mapper.ReportMapper;
import com.Group13_65CNTT.AutoGarage.reporting.repository.ReportRepository;
import com.Group13_65CNTT.AutoGarage.reporting.service.impl.ReportServiceImpl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Report period policy")
class ReportServiceTest {
    @Mock ReportRepository repository;
    @Mock ReportMapper mapper;
    @Mock Clock clock;
    @Spy ReportProperties properties = new ReportProperties();
    @Spy PaginationPolicy pagination = new PaginationPolicy(new PaginationProperties());
    @InjectMocks ReportServiceImpl service;

    @Test
    void configuredPeriodAppliesBeforeQuerying() {
        properties.setMaxPeriod(Duration.ofDays(30));
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        assertThatThrownBy(
                        () -> service.dashboard(now, now.plus(Duration.ofDays(30)).plusSeconds(1)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void invertedPeriodIsRejectedBeforeAnyQuery() {
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        assertThatThrownBy(() -> service.dashboard(now, now.minusSeconds(1)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void unboundedPeriodIsRejectedBeforeAnyQuery() {
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        assertThatThrownBy(() -> service.dashboard(now, now.plus(Duration.ofDays(367))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void oneSecondPastTheMaximumCannotPassByTruncatingDays() {
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        assertThatThrownBy(
                        () -> service.dashboard(now, now.plus(Duration.ofDays(366)).plusSeconds(1)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository, mapper);
    }
}
