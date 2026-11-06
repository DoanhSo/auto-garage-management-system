package com.Group13_65CNTT.AutoGarage.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;
import com.Group13_65CNTT.AutoGarage.audit.entity.AuditEvent;
import com.Group13_65CNTT.AutoGarage.audit.repository.AuditEventRepository;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditQueryService;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Free-text audit search on the real schema: keyword, resolved action codes, escaping and filters. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class AuditSearchIntegrationTest extends IntegrationTestSupport {
    private static final long ACTOR_AN = 920101L;
    private static final long ACTOR_BINH = 920102L;

    private final JdbcTemplate jdbc;
    private final AuditEventRepository events;
    private final AuditQueryService service;
    private final PaginationPolicy pagination;

    private AuditEvent login;
    private AuditEvent voidInvoice;
    private AuditEvent shiftEvent;

    private AuditEvent save(Long actor, String action, String entityType, Long entityId) {
        return events.saveAndFlush(AuditEvent.builder()
                .actorUserId(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .build());
    }

    @BeforeEach
    void fixtures() {
        Jwt jwt = Jwt.withTokenValue("test-only")
                .header("alg", "RS256")
                .subject("550e8400-e29b-41d4-a716-446655440020")
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES"
                + "(920101,'an.nguyen','test','Nguyễn Văn An','ADMIN'),"
                + "(920102,'binh.tran','test','Trần Bình','RECEPTIONIST')");
        login = save(ACTOR_AN, "USER_LOGIN", "USER", ACTOR_AN);
        voidInvoice = save(ACTOR_BINH, "INVOICE_VOID", "INVOICE", 4242L);
        shiftEvent = save(ACTOR_BINH, "SHIFT_CANCEL", "EMPLOYEE_SHIFT", 77L);
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    private Pageable page() {
        return pagination.page(0, 50, Sort.by(UserQueryConstants.DEFAULT_SORT_BY).descending());
    }

    private List<Long> search(Long actor, String action, String query, List<String> actions) {
        return service.searchEvents(actor, action, query, actions, page()).getContent().stream()
                .map(AuditEventResponse::getId)
                .toList();
    }

    @Test
    @DisplayName("Từ khóa khớp mã hành động và loại đối tượng")
    void keywordMatchesActionAndEntityType() {
        assertThat(search(null, null, "invoice", null)).contains(voidInvoice.getId()).doesNotContain(login.getId());
        assertThat(search(null, null, "login", null)).contains(login.getId()).doesNotContain(voidInvoice.getId());
        assertThat(search(null, null, "shift", null)).contains(shiftEvent.getId());
    }

    @Test
    @DisplayName("Từ khóa là số khớp mã đối tượng")
    void numericKeywordMatchesEntityId() {
        assertThat(search(null, null, "4242", null)).contains(voidInvoice.getId()).doesNotContain(login.getId());
    }

    @Test
    @DisplayName("Từ khóa khớp tên hoặc tài khoản người thực hiện")
    void keywordMatchesActorNameOrUsername() {
        assertThat(search(null, null, "an.nguyen", null)).contains(login.getId()).doesNotContain(voidInvoice.getId());
        assertThat(search(null, null, "Văn An", null)).contains(login.getId()).doesNotContain(voidInvoice.getId());
        assertThat(search(null, null, "bình", null)).contains(voidInvoice.getId(), shiftEvent.getId());
    }

    @Test
    @DisplayName("Mã hành động do client suy từ nhãn tiếng Việt cũng khớp")
    void resolvedActionCodesMatch() {
        assertThat(search(null, null, "đăng nhập", null)).doesNotContain(login.getId());
        assertThat(search(null, null, "đăng nhập", List.of("USER_LOGIN"))).contains(login.getId());
    }

    @Test
    @DisplayName("Ký tự % và _ trong từ khóa khớp theo nghĩa đen, không phải ký tự đại diện")
    void likeWildcardsAreEscaped() {
        assertThat(search(null, null, "r%l", null)).doesNotContain(login.getId());
        assertThat(search(null, null, "r_l", null)).contains(login.getId());
        assertThat(search(null, null, "%", null)).isEmpty();
    }

    @Test
    @DisplayName("Từ khóa kết hợp VÀ với bộ lọc người thực hiện và hành động")
    void keywordCombinesWithExistingFilters() {
        assertThat(search(ACTOR_BINH, null, "invoice", null)).containsExactly(voidInvoice.getId());
        assertThat(search(ACTOR_AN, null, "invoice", null)).isEmpty();
        assertThat(search(null, "SHIFT_CANCEL", "binh", null)).containsExactly(shiftEvent.getId());
        assertThat(search(null, "USER_LOGIN", "invoice", null)).isEmpty();
    }

    @Test
    @DisplayName("Không có từ khóa: hành vi cũ, các bộ lọc vẫn hoạt động")
    void withoutKeywordBehavesAsBefore() {
        assertThat(search(null, null, null, null)).contains(login.getId(), voidInvoice.getId(), shiftEvent.getId());
        assertThat(search(null, null, "   ", List.of())).contains(login.getId(), voidInvoice.getId());
        assertThat(search(ACTOR_AN, null, null, null)).contains(login.getId()).doesNotContain(voidInvoice.getId());
    }

    @Test
    @DisplayName("Từ khóa quá dài hoặc quá nhiều mã hành động bị từ chối")
    void oversizedInputIsRejected() {
        assertThatThrownBy(() -> search(null, null, "x".repeat(101), null)).isInstanceOf(BusinessException.class);
        List<String> tooMany = IntStream.range(0, 51).mapToObj(i -> "ACTION_" + i).toList();
        assertThatThrownBy(() -> search(null, null, "abc", tooMany)).isInstanceOf(BusinessException.class);
    }
}
