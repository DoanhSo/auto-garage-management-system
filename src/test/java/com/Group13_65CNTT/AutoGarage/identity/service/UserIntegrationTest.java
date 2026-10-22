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

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
@DisplayName("Layered UserService integration on H2")
@RequiredArgsConstructor
class UserIntegrationTest extends IntegrationTestSupport {
    private final UserService service;
    private final UserRepository users;
    private final AuditEventRepository audit;
    private Long adminId;

    @BeforeEach
    void admin() {
        User admin =
                users.saveAndFlush(
                        User.builder()
                                .identityId("550e8400-e29b-41d4-a716-446655440010")
                                .username("h2.admin")
                                .fullName("Administrator")
                                .role(Role.ADMIN)
                                .build());
        adminId = admin.getId();
        Jwt jwt =
                Jwt.withTokenValue("test-only")
                        .header("alg", "RS256")
                        .subject("550e8400-e29b-41d4-a716-446655440010")
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
    void createPersistsKeycloakIdentityBaseEntityAuditorsAndAspectAudit() {
        var response = service.createUser(request());
        User stored = users.findById(response.getId()).orElseThrow();
        assertThat(stored.getIdentityId()).isEqualTo("550e8400-e29b-41d4-a716-446655440011");
        assertThat(stored.getPasswordHash()).isNull();
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
        assertThat(updated.getIdentityId()).isEqualTo("550e8400-e29b-41d4-a716-446655440011");
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
                .identityId("550e8400-e29b-41d4-a716-446655440011")
                .username("h2.customer")
                .email("integration@example.test")
                .fullName("Integration Customer")
                .role(Role.CUSTOMER)
                .build();
    }
}
