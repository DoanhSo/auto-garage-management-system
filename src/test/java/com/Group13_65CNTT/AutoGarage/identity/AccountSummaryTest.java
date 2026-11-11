package com.Group13_65CNTT.AutoGarage.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.AccountStatus;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.AccountSummary;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountSummaryTest {

    private User user() {
        return User.builder().username("ktv1").fullName("Kỹ thuật viên").role(Role.TECHNICIAN).active(true).build();
    }

    @Test
    @DisplayName("Không có tài khoản: NONE, không tên đăng nhập")
    void noAccount() {
        assertThat(AccountSummary.of(null)).isEqualTo(new AccountSummary(AccountStatus.NONE, null, null));
    }

    @Test
    @DisplayName("Có tài khoản nhưng chưa nối Keycloak và chưa đăng nhập: PENDING")
    void neverSignedIn() {
        assertThat(AccountSummary.statusOf(user())).isEqualTo(AccountStatus.PENDING);
    }

    @Test
    @DisplayName("Chỉ nối Keycloak mà chưa đăng nhập vẫn là PENDING; đã đăng nhập thì ACTIVE")
    void signedIn() {
        User linked = user();
        linked.setIdentityId("6f1c2c1e-0000-0000-0000-000000000001");
        assertThat(AccountSummary.statusOf(linked)).isEqualTo(AccountStatus.PENDING);

        User loggedIn = user();
        Instant at = Instant.parse("2026-11-10T08:15:00Z");
        loggedIn.setLastLoginAt(at);
        AccountSummary summary = AccountSummary.of(loggedIn);
        assertThat(summary.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(summary.username()).isEqualTo("ktv1");
        assertThat(summary.lastLoginAt()).isEqualTo(at);
    }

    @Test
    @DisplayName("Bị khóa hoặc đã xóa: LOCKED, kể cả khi đã từng đăng nhập")
    void blocked() {
        User inactive = user();
        inactive.setActive(false);
        inactive.setIdentityId("6f1c2c1e-0000-0000-0000-000000000002");
        assertThat(AccountSummary.statusOf(inactive)).isEqualTo(AccountStatus.LOCKED);

        User deleted = user();
        deleted.setDeletedAt(Instant.now());
        assertThat(AccountSummary.statusOf(deleted)).isEqualTo(AccountStatus.LOCKED);
    }
}
