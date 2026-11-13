package com.Group13_65CNTT.AutoGarage.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.auth.service.AuthService;
import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerAccountRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.LinkCustomerAccountRequest;
import com.Group13_65CNTT.AutoGarage.customer.service.CustomerService;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Front desk creates or moves a customer's sign-in account, against MySQL. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class CustomerAccountIntegrationTest extends IntegrationTestSupport {
    private static final long ADMIN_ID = 930601L;
    private static final String NEW_IDENTITY = "6f1c2c1e-0000-4000-8000-000000930601";

    @MockitoBean KeycloakIdentityService keycloak;

    private final JdbcTemplate jdbc;
    private final CustomerService customers;
    private final AuthService auth;

    @BeforeEach
    void fixtures() {
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(?,?,?,?,?)",
                ADMIN_ID, "acct_admin", "x", "Quản trị", "ADMIN");
        jdbc.update("INSERT INTO customers(id,customer_code,full_name,phone,email,credit_limit,is_active)"
                + " VALUES(930601,'KH-ACC-01','Khách Tại Quầy','0930601001','tai.quay@example.test',0,true)");
        signIn(String.valueOf(ADMIN_ID), "ADMIN", "acct_admin");
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private void signIn(String subject, String role, String username) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(subject)
                .claim("role", role).claim("preferred_username", username).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    @Test
    @DisplayName("Tài khoản lễ tân tạo đăng nhập lần đầu dùng đúng hồ sơ cũ, không sinh hồ sơ mới")
    void createdAccountSignsIntoTheExistingProfile() {
        when(keycloak.createOrLinkUser(anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(Optional.of(NEW_IDENTITY));

        var created = customers.createAccount(930601L, CreateCustomerAccountRequest.builder()
                .username("khach.taiquay").password("MatKhauBanDau#2026").build());

        assertThat(created.getUserId()).isNotNull();
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND identity_id = ? AND email = ?",
                created.getUserId(), NEW_IDENTITY, "tai.quay@example.test")).isEqualTo(1L);
        long profilesBefore = count("SELECT COUNT(*) FROM customers");

        signIn(NEW_IDENTITY, "CUSTOMER", "khach.taiquay");
        var me = auth.me();

        assertThat(me.getId()).isEqualTo(created.getUserId());
        assertThat(count("SELECT COUNT(*) FROM customers")).isEqualTo(profilesBefore);
        assertThat(count("SELECT COUNT(*) FROM customers WHERE id = 930601 AND user_id = ?", me.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("Nối tài khoản: chuyển tài khoản từ hồ sơ trùng sang hồ sơ cũ và ngừng hồ sơ trùng")
    void linkMovesTheAccountAndRetiresTheDuplicate() {
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(930602,'tu.dang.ky','x','Khách','CUSTOMER')");
        jdbc.update("INSERT INTO customers(id,user_id,customer_code,full_name,phone,credit_limit,is_active)"
                + " VALUES(930602,930602,'CUS-OIDC-930602','Khách Tự Đăng Ký','0930601002',0,true)");

        var linked = customers.linkAccount(930601L, new LinkCustomerAccountRequest(930602L));

        assertThat(linked.getUserId()).isEqualTo(930602L);
        assertThat(count("SELECT COUNT(*) FROM customers WHERE id = 930602 AND user_id IS NULL AND is_active = false"))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("Không gộp khi hồ sơ trùng đã có xe; thông báo nêu loại dữ liệu")
    void linkRefusesWhenTheDuplicateHasBusinessData() {
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(930603,'co.xe','x','Khách','CUSTOMER')");
        jdbc.update("INSERT INTO customers(id,user_id,customer_code,full_name,phone,credit_limit,is_active)"
                + " VALUES(930603,930603,'CUS-OIDC-930603','Khách Có Xe','0930601003',0,true)");
        jdbc.update("INSERT INTO vehicles(id,customer_id,license_plate,brand,model,odometer,is_active)"
                + " VALUES(930603,930603,'30K-930.60','Toyota','Vios',1000,true)");

        assertThatThrownBy(() -> customers.linkAccount(930601L, new LinkCustomerAccountRequest(930603L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("1 xe");
        assertThat(count("SELECT COUNT(*) FROM customers WHERE id = 930601 AND user_id IS NULL")).isEqualTo(1L);
    }
}
