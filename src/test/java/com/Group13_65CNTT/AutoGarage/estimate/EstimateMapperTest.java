package com.Group13_65CNTT.AutoGarage.estimate;

import com.Group13_65CNTT.AutoGarage.estimate.entity.Estimate;
import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus;
import com.Group13_65CNTT.AutoGarage.estimate.mapper.EstimateMapper;
import com.Group13_65CNTT.AutoGarage.estimate.mapper.EstimateMapperImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EstimateMapperTest {

    private final EstimateMapper mapper = new EstimateMapperImpl();

    @Test
    void sentEstimatePastValidityReadsAsExpired() {
        Estimate estimate = Estimate.builder()
                .status(EstimateStatus.SENT)
                .validUntil(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertThat(mapper.toResponse(estimate).getStatus()).isEqualTo(EstimateStatus.EXPIRED);
        assertThat(estimate.getStatus()).isEqualTo(EstimateStatus.SENT);
    }

    @Test
    void sentEstimateWithinValidityStaysSent() {
        Estimate estimate = Estimate.builder()
                .status(EstimateStatus.SENT)
                .validUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        assertThat(mapper.toResponse(estimate).getStatus()).isEqualTo(EstimateStatus.SENT);
    }

    @Test
    void decidedEstimateKeepsItsStatusAfterValidity() {
        Estimate estimate = Estimate.builder()
                .status(EstimateStatus.PARTIALLY_APPROVED)
                .validUntil(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        assertThat(mapper.toResponse(estimate).getStatus()).isEqualTo(EstimateStatus.PARTIALLY_APPROVED);
    }
}
