package com.Group13_65CNTT.AutoGarage.identity.service;

import static org.assertj.core.api.Assertions.*;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.audit.repository.AuditEventRepository;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.dto.request.*;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
@DisplayName("Layered UserService integration on H2")
class UserIntegrationTest extends IntegrationTestSupport {
    @Autowired UserService service;
    @Autowired UserRepository users;
    @Autowired AuditEventRepository audit;
    @Autowired PasswordEncoder passwords;
    private Long adminId;

    @BeforeEach
    void admin() {
        User admin =
                users.saveAndFlush(
                        User.builder()
                                .username("h2.admin")
                                .fullName("Administrator")
                                .passwordHash(passwords.encode("IntegrationTest!123"))
                                .role(Role.ADMIN)
                                .build());
        adminId = admin.getId();
        Jwt jwt =
                Jwt.withTokenValue("test-only")
                        .header("alg", "HS256")
                        .subject(adminId.toString())
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPersistsHashBaseEntityAuditorsAndAspectAudit() {
        var response = service.createUser(request());
        User stored = users.findById(response.getId()).orElseThrow();
        assertThat(passwords.matches("IntegrationTest!123", stored.getPasswordHash())).isTrue();
        assertThat(stored.getCreatedBy()).isEqualTo(adminId);
        assertThat(stored.getUpdatedBy()).isEqualTo(adminId);
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(audit.findAll()).extracting("action").contains("USER_CREATED");
    }

    @Test
    void mapstructUpdateClearsEmailAndReturnsIncrementedVersion() {
        var created = service.createUser(request());
        var updated =
                service.updateUser(
                        created.getId(),
                        UserUpdateRequest.builder()
                                .fullName("Changed")
                                .role(Role.CUSTOMER)
                                .active(true)
                                .version(created.getVersion())
                                .build());
        assertThat(updated.getEmail()).isNull();
        assertThat(updated.getVersion()).isGreaterThan(created.getVersion());
        assertThat(users.findById(created.getId()).orElseThrow().getPasswordHash())
                .startsWith("$2");
    }

    @Test
    void dynamicJpqlSearchUsesKeywordActiveAndPagination() {
        service.createUser(request());
        var page = service.searchUsers("integration", true, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getUsername()).isEqualTo("h2.customer");
    }

    @Test
    void duplicateUserRaisesTypedBusinessException() {
        service.createUser(request());
        assertThatThrownBy(() -> service.createUser(request()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode())
                                        .isEqualTo(ResponseCode.DUPLICATE_RESOURCE));
    }

    private UserCreateRequest request() {
        return UserCreateRequest.builder()
                .username("h2.customer")
                .email("integration@example.test")
                .fullName("Integration Customer")
                .role(Role.CUSTOMER)
                .password("IntegrationTest!123")
                .build();
    }
}
