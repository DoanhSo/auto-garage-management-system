package com.Group13_65CNTT.AutoGarage.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

class WorkshopPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PolicyConfiguration.class);

    @Test
    void bindsNonDefaultWorkshopPolicies() {
        runner.withPropertyValues("garage.workshop.default-service-minutes=45",
                        "garage.workshop.max-attachment-bytes=4096",
                        "garage.workshop.max-upload-request-bytes=8192",
                        "garage.workshop.reservation-hold=5m")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    WorkshopProperties properties = context.getBean(WorkshopProperties.class);
                    assertThat(properties.getDefaultServiceMinutes()).isEqualTo(45);
                    assertThat(properties.getMaxAttachmentBytes()).isEqualTo(4096);
                    assertThat(properties.getMaxUploadRequestBytes()).isEqualTo(8192);
                    assertThat(properties.getReservationHold()).isEqualTo(Duration.ofMinutes(5));
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "garage.workshop.default-service-minutes=0",
            "garage.workshop.max-attachment-bytes=0",
            "garage.workshop.reservation-hold=0s",
            "garage.workshop.reservation-hold=-1s",
            "garage.workshop.max-upload-request-bytes=1024"
    })
    void refusesInvalidWorkshopPolicy(String property) {
        runner.withPropertyValues(property).run(context -> assertThat(context).hasFailed());
    }
}
