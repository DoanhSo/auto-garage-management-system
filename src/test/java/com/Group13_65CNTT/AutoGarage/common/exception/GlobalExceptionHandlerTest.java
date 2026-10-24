package com.Group13_65CNTT.AutoGarage.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.Stream;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {
    private static final String SENSITIVE =
            "password=private-value Authorization=Bearer private-token";
    private MockMvc mvc;

    @BeforeEach
    void initialize() {
        mvc =
                MockMvcBuilders.standaloneSetup(new FailureController())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    static Stream<Arguments> failures() {
        return Stream.of(
                Arguments.of("business", 404, 2002),
                Arguments.of("shared-rule-forbidden", 403, 3002),
                Arguments.of("shared-conflict-stale", 409, 2005),
                Arguments.of("upload-too-large", 413, 2013),
                Arguments.of("credentials", 401, 3005),
                Arguments.of("username", 401, 3005),
                Arguments.of("unauthenticated", 401, 3001),
                Arguments.of("auth-infrastructure", 503, 4002),
                Arguments.of("forbidden", 403, 3002),
                Arguments.of("mysql-unique", 409, 2003),
                Arguments.of("h2-unique", 409, 2003),
                Arguments.of("duplicate", 409, 2003),
                Arguments.of("foreign-key", 500, 4001),
                Arguments.of("check-constraint", 500, 4001),
                Arguments.of("not-null", 500, 4001),
                Arguments.of("optimistic", 409, 2010),
                Arguments.of("lock", 409, 2010),
                Arguments.of("timeout", 409, 2010),
                Arguments.of("database", 503, 4003),
                Arguments.of("transaction", 503, 4003),
                Arguments.of("sql-bug", 500, 4001),
                Arguments.of("provider", 503, 4004),
                Arguments.of("unexpected", 500, 4001));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void everyFailureUsesSafeHrmsEnvelope(String kind, int status, int code, CapturedOutput output)
            throws Exception {
        mvc.perform(get("/test/failure").param("kind", kind))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/test/failure"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(
                        result ->
                                assertThat(result.getResponse().getContentAsString())
                                        .doesNotContain(
                                                "private-value", "private-token", "SQLException"));
        assertThat(output.getAll()).doesNotContain("private-value", "private-token");
        if (status >= 500)
            assertThat(output.getAll()).contains("exception trace (messages excluded)");
    }

    @Test
    void objectLevelValidationHasAResponseInsteadOfClassCastFailure() throws Exception {
        mvc.perform(get("/test/failure").param("kind", "object-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.data.request").value("Fields are inconsistent"));
    }

    @Test
    void fieldValidationExcludesRejectedValues() throws Exception {
        mvc.perform(
                        post("/test/body")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.data.name").value("Name is required"));
    }

    @Test
    void malformedJsonAndWrongEnumOrIdCannotExposeParserDiagnostics() throws Exception {
        mvc.perform(
                        post("/test/body")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"private-value\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2004))
                .andExpect(
                        result ->
                                assertThat(result.getResponse().getContentAsString())
                                        .doesNotContain("private-value"));
        mvc.perform(get("/test/number").param("id", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2004));
        mvc.perform(get("/test/types").param("role", "ROOT").param("at", "2026-10-15T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2004));
        mvc.perform(get("/test/types").param("role", "ADMIN").param("at", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2004));
        mvc.perform(get("/test/number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2004));
    }

    @Test
    void unsupportedMethodMediaAndResponseFormatKeepEnvelope() throws Exception {
        mvc.perform(put("/test/body"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(jsonPath("$.code").value(2008));
        mvc.perform(post("/test/body").contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(2009));
        mvc.perform(get("/test/number").param("id", "1").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value(2011));
        mvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @RestController
    static class FailureController {
        @GetMapping("/test/failure")
        ApiResponse<Void> failure(@RequestParam String kind) throws BindException {
            throw switch (kind) {
                case "business" -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
                case "shared-rule-forbidden" -> new com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException(ResponseCode.FORBIDDEN);
                case "shared-conflict-stale" -> new com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException(ResponseCode.STALE_USER_VERSION);
                case "upload-too-large" -> new org.springframework.web.multipart.MaxUploadSizeExceededException(100);
                case "credentials" -> new BadCredentialsException(SENSITIVE);
                case "username" -> new UsernameNotFoundException(SENSITIVE);
                case "unauthenticated" -> new InsufficientAuthenticationException(SENSITIVE);
                case "auth-infrastructure" -> new AuthenticationServiceException(SENSITIVE);
                case "forbidden" -> new AccessDeniedException(SENSITIVE);
                case "mysql-unique" -> integrity("23000", 1062);
                case "h2-unique" -> integrity("23505", 23505);
                case "duplicate" -> new DuplicateKeyException(SENSITIVE);
                case "foreign-key" -> integrity("23000", 1452);
                case "check-constraint" -> integrity("HY000", 3819);
                case "not-null" -> integrity("23502", 23502);
                case "optimistic" -> new OptimisticLockingFailureException(SENSITIVE);
                case "lock" -> new CannotAcquireLockException(SENSITIVE);
                case "timeout" -> new QueryTimeoutException(SENSITIVE);
                case "database" -> new DataAccessResourceFailureException(SENSITIVE);
                case "transaction" -> new CannotCreateTransactionException(SENSITIVE);
                case "sql-bug" -> new InvalidDataAccessResourceUsageException(SENSITIVE);
                case "provider" ->
                        new NotificationDeliveryException(new IllegalStateException(SENSITIVE));
                case "object-validation" -> {
                    var binding = new BeanPropertyBindingResult(new Object(), "request");
                    binding.addError(new ObjectError("request", "Fields are inconsistent"));
                    throw new BindException(binding);
                }
                default -> new IllegalStateException(SENSITIVE);
            };
        }

        private DataIntegrityViolationException integrity(String state, int code) {
            return new DataIntegrityViolationException(
                    SENSITIVE, new SQLException(SENSITIVE, state, code));
        }

        @PostMapping(value = "/test/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> body(@Valid @RequestBody Input input) {
            return ApiResponse.success(ResponseCode.SUCCESS, null, "/test/body");
        }

        @GetMapping("/test/types")
        ApiResponse<Void> types(@RequestParam Role role, @RequestParam Instant at) {
            return ApiResponse.success(ResponseCode.SUCCESS, null, "/test/types");
        }

        @GetMapping("/test/number")
        ApiResponse<Void> number(@RequestParam Long id) {
            return ApiResponse.success(ResponseCode.SUCCESS, null, "/test/number");
        }
    }

    record Input(@NotBlank(message = "Name is required") String name) {}
}
