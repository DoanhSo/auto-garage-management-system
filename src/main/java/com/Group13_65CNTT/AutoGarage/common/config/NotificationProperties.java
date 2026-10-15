package com.Group13_65CNTT.AutoGarage.common.config;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;

@Getter
@Setter
@Validated
@ConfigurationProperties("garage.notification")
public class NotificationProperties {
    private boolean workerEnabled;
    private boolean remindersEnabled;

    @Min(1)
    @Max(100)
    private int batchSize = 20;

    @Min(1)
    @Max(10)
    private int maxAttempts = 3;

    @NotNull private Duration lease = Duration.ofMinutes(2);
    @NotNull private Duration retryDelay = Duration.ofMinutes(5);
    @NotBlank private String zone = "Asia/Bangkok";

    @Min(1)
    @Max(90)
    private int reminderDays = 15;

    private boolean emailEnabled;
    private String emailFrom;
    private boolean smsEnabled;
    private String smsUrl;
    private String smsToken;

    @NotNull private Duration connectTimeout = Duration.ofSeconds(5);
    @NotNull private Duration requestTimeout = Duration.ofSeconds(10);
    @NotNull private Duration pollDelay = Duration.ofSeconds(5);
    @NotBlank private String reminderCron = "0 0 8 * * *";

    @Min(1)
    @Max(1000)
    private int reminderMaxBatches = 100;

    @NotBlank
    @Size(max = 255)
    private String defaultSubject = "AutoGarage notification";

    @AssertTrue(
            message =
                    "Provider timeouts must be positive and below the lease; scheduling policy must"
                        + " be valid")
    public boolean isDeliveryPolicyValid() {
        return positive(connectTimeout)
                && positive(requestTimeout)
                && positive(pollDelay)
                && lease != null
                && requestTimeout.compareTo(lease) < 0
                && connectTimeout.compareTo(requestTimeout) <= 0
                && org.springframework.scheduling.support.CronExpression.isValidExpression(
                        reminderCron)
                && defaultSubject != null
                && !defaultSubject.contains("\r")
                && !defaultSubject.contains("\n");
    }

    private boolean positive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }

    @AssertTrue(message = "Lease and retry delay must be positive and zone must be valid")
    public boolean isPolicyValid() {
        try {
            ZoneId.of(zone);
        } catch (java.time.DateTimeException | NullPointerException invalid) {
            return false;
        }
        if (emailEnabled
                && (emailFrom == null || !emailFrom.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))
            return false;
        if (smsEnabled) {
            try {
                var uri = URI.create(smsUrl);
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null
                        || uri.getQuery() != null
                        || uri.getFragment() != null) return false;
            } catch (IllegalArgumentException | NullPointerException invalid) {
                return false;
            }
            if (smsToken == null || smsToken.isBlank()) return false;
        }
        return lease != null
                && !lease.isNegative()
                && !lease.isZero()
                && retryDelay != null
                && !retryDelay.isNegative()
                && !retryDelay.isZero();
    }
}
