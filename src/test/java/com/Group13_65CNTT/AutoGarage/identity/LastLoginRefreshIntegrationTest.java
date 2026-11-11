package com.Group13_65CNTT.AutoGarage.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** users.last_login_at is refreshed at most once per interval, against MySQL. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class LastLoginRefreshIntegrationTest extends IntegrationTestSupport {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    @Test
    @DisplayName("Ghi lần đăng nhập khi trống hoặc đã quá hạn, bỏ qua khi vừa ghi gần đây")
    void touchLastLoginRespectsTheRefreshInterval() {
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(930501,'touch_login','x','Touch','TECHNICIAN')");
        Duration gap = SecurityConstants.LAST_LOGIN_REFRESH_INTERVAL;
        Instant first = Instant.parse("2026-11-11T08:00:00Z");

        assertThat(users.touchLastLogin(930501L, first, first.minus(gap))).isEqualTo(1);
        Instant soon = first.plus(Duration.ofMinutes(10));
        assertThat(users.touchLastLogin(930501L, soon, soon.minus(gap))).isZero();
        Instant later = first.plus(gap).plus(Duration.ofMinutes(1));
        assertThat(users.touchLastLogin(930501L, later, later.minus(gap))).isEqualTo(1);
    }
}
