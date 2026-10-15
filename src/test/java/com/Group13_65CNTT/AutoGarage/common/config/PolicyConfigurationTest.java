package com.Group13_65CNTT.AutoGarage.common.config;

import static org.assertj.core.api.Assertions.*;

import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

class PolicyConfigurationTest {
    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withUserConfiguration(PolicyConfiguration.class, WorkerConfiguration.class);

    @Test
    void passwordAndPaginationFollowBoundConfiguration() {
        context.withPropertyValues(
                        "security.password.min-length=20",
                        "garage.pagination.max-size=30",
                        "garage.pagination.max-page=50",
                        "garage.pagination.default-size=10")
                .run(
                        c -> {
                            assertThat(c).hasNotFailed();
                            var passwords = new PasswordPolicy(c.getBean(PasswordProperties.class));
                            assertThatThrownBy(
                                            () -> passwords.validateNewPassword("TwelveChars!123"))
                                    .isInstanceOf(BusinessException.class);
                            assertThatCode(
                                            () ->
                                                    passwords.validateNewPassword(
                                                            "ValidLongPassword!123456"))
                                    .doesNotThrowAnyException();
                            assertThatThrownBy(() -> passwords.validateNewPassword("漢".repeat(25)))
                                    .isInstanceOf(BusinessException.class);
                            var paging =
                                    new PaginationPolicy(c.getBean(PaginationProperties.class));
                            assertThat(paging.page(50, 30, Sort.unsorted()).getPageSize())
                                    .isEqualTo(30);
                            assertThatThrownBy(() -> paging.page(0, 31, Sort.unsorted()))
                                    .isInstanceOf(BusinessException.class);
                            assertThatThrownBy(() -> paging.page(51, 10, Sort.unsorted()))
                                    .isInstanceOf(BusinessException.class);
                            assertThatThrownBy(() -> paging.validate(Pageable.unpaged()))
                                    .isInstanceOf(BusinessException.class);
                        });
    }

    @Test
    void invalidPoliciesFailStartupRatherThanFailingARequestLater() {
        for (String setting :
                List.of(
                        "security.password.min-length=3",
                        "security.password.min-length=73",
                        "garage.pagination.max-size=0",
                        "garage.pagination.default-size=101",
                        "garage.pagination.max-page=-1",
                        "garage.report.max-period=0s",
                        "garage.notification.request-timeout=3m",
                        "garage.notification.connect-timeout=0s",
                        "garage.notification.poll-delay=-1s",
                        "garage.notification.reminder-cron=invalid",
                        "garage.notification.reminder-max-batches=0")) {
            context.withPropertyValues(setting).run(c -> assertThat(c).hasFailed());
        }
    }

    @Test
    void reportAndProviderPoliciesBindWithoutSourceChanges() {
        context.withPropertyValues(
                        "garage.report.max-period=30d",
                        "garage.notification.request-timeout=20s",
                        "garage.notification.connect-timeout=8s",
                        "garage.notification.reminder-max-batches=7")
                .run(
                        c -> {
                            assertThat(c).hasNotFailed();
                            assertThat(c.getBean(ReportProperties.class).getMaxPeriod())
                                    .isEqualTo(Duration.ofDays(30));
                            assertThat(c.getBean(NotificationProperties.class).getRequestTimeout())
                                    .isEqualTo(Duration.ofSeconds(20));
                            assertThat(
                                            c.getBean(NotificationProperties.class)
                                                    .getReminderMaxBatches())
                                    .isEqualTo(7);
                            assertThat(c.getBean(HttpClient.class).connectTimeout())
                                    .contains(Duration.ofSeconds(8));
                        });
    }
}
