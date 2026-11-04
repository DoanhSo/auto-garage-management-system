package com.Group13_65CNTT.AutoGarage.warranty;

import com.Group13_65CNTT.AutoGarage.warranty.entity.Warranty;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyStatus;
import com.Group13_65CNTT.AutoGarage.warranty.mapper.WarrantyMapper;
import com.Group13_65CNTT.AutoGarage.warranty.mapper.WarrantyMapperImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WarrantyMapperTest {

    private final WarrantyMapper mapper = new WarrantyMapperImpl();

    @Test
    void activeBookPastItsEndDateReadsAsExpired() {
        Warranty warranty = Warranty.builder()
                .status(WarrantyStatus.ACTIVE)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        assertThat(mapper.toResponse(warranty).getStatus()).isEqualTo(WarrantyStatus.EXPIRED);
    }

    @Test
    void activeBookWithinItsTermStaysActive() {
        Warranty warranty = Warranty.builder()
                .status(WarrantyStatus.ACTIVE)
                .expiresAt(Instant.now().plus(10, ChronoUnit.DAYS))
                .build();

        assertThat(mapper.toResponse(warranty).getStatus()).isEqualTo(WarrantyStatus.ACTIVE);
    }

    @Test
    void kmOnlyBookWithoutEndDateStaysActive() {
        Warranty warranty = Warranty.builder().status(WarrantyStatus.ACTIVE).build();

        assertThat(mapper.toResponse(warranty).getStatus()).isEqualTo(WarrantyStatus.ACTIVE);
    }
}
