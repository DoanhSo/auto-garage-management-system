package com.Group13_65CNTT.AutoGarage;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:autogarage_registration;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@RequiredArgsConstructor
class RegistrationHttpIntegrationTest extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final ObjectMapper json;
    @MockitoBean KeycloakIdentityService identities;

    @Test
    void anonymousRegistrationCreatesOnlyCustomerAndReturnsNoCredentials() throws Exception {
        when(identities.createOrLinkUser(anyString(), anyString(), anyString(), any(), anyString())).thenReturn(Optional.of("new-identity"));
        mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON).content(body("customer@gmail.com", "test-password-123")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(1001)).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.path").value(ApiPaths.REGISTRATION)).andExpect(jsonPath("$.data").isEmpty());
        verify(identities).createOrLinkUser("customer.one", "customer@gmail.com", "Customer Name", Role.CUSTOMER, "test-password-123");
    }

    @Test
    void cannotMassAssignAdministratorRoleOrIdentity() throws Exception {
        mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"customer.one\",\"fullName\":\"Customer\",\"email\":\"customer@gmail.com\",\"password\":\"test-password-123\",\"role\":\"ADMIN\",\"identityId\":\"existing-admin\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(identities);
    }

    @Test
    void validatesEmailAndSharedPasswordPolicyBeforeCallingKeycloak() throws Exception {
        mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON).content(body("invalid-email", "test-password-123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2001));
        for (String password : new String[]{"short", "😀".repeat(19)}) {
            mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON).content(body("customer@gmail.com", password)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2007));
        }
        verifyNoInteractions(identities);
    }

    @Test
    void duplicateEmailReturnsConflictWithoutUpdatingExistingIdentity() throws Exception {
        when(identities.createOrLinkUser(anyString(), anyString(), anyString(), any(), anyString())).thenThrow(new BusinessException(ResponseCode.DUPLICATE_RESOURCE));
        mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON).content(body("customer@gmail.com", "test-password-123")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(2003));
        verify(identities, never()).syncUserUpdate(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void providerOutageReturnsSafeServiceUnavailable() throws Exception {
        when(identities.createOrLinkUser(anyString(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE, new IllegalStateException("private-provider-detail")));
        mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON).content(body("customer@gmail.com", "test-password-123")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(4002))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-provider-detail"))));
    }

    @Test
    void registrationDoesNotMakeOtherAuthenticationEndpointsPublic() throws Exception {
        mvc.perform(get(ApiPaths.AUTH_BASE + ApiPaths.ME)).andExpect(status().isUnauthorized());
        mvc.perform(get(ApiPaths.REGISTRATION)).andExpect(status().isUnauthorized());
    }

    private String body(String email, String password) {
        return json.writeValueAsString(Map.of("username", "customer.one", "fullName", "Customer Name", "email", email, "password", password));
    }

    @Test
    void requiresValidUsernameBeforeCallingIdentityProvider() throws Exception {
        for (String username : new String[]{"", "ab", "a".repeat(101), "customer name", "customer@example.test", "nguyễn"}) {
            var payload = Map.of("username", username, "fullName", "Customer Name", "email", "customer@gmail.com", "password", "test-password-123");
            mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.data.username").isString());
        }
        mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Customer\",\"email\":\"customer@gmail.com\",\"password\":\"test-password-123\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.username").isString());
        verifyNoInteractions(identities);
    }

    @Test
    void normalizesMixedCaseUsernameWithoutReplacingItWithEmail() throws Exception {
        when(identities.createOrLinkUser(anyString(), anyString(), anyString(), any(), anyString())).thenReturn(Optional.of("new-identity"));
        mvc.perform(post(ApiPaths.REGISTRATION).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", "Customer.One", "fullName", "Customer Name", "email", "customer@gmail.com", "password", "test-password-123"))))
            .andExpect(status().isCreated());
        verify(identities).createOrLinkUser("customer.one", "customer@gmail.com", "Customer Name", Role.CUSTOMER, "test-password-123");
    }
}
