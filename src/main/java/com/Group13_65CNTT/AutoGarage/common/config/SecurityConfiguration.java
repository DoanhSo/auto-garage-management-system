package com.Group13_65CNTT.AutoGarage.common.config;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.security.JsonCorsProcessor;
import com.Group13_65CNTT.AutoGarage.common.security.KeycloakJwtAuthenticationConverter;
import com.Group13_65CNTT.AutoGarage.common.security.SecurityErrorWriter;
import com.Group13_65CNTT.AutoGarage.common.util.SafeFailureLog;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            SecurityErrorWriter errors,
            @Value("${springdoc.api-docs.enabled:false}") boolean docsEnabled)
            throws Exception {
        KeycloakJwtAuthenticationConverter authentication =
                new KeycloakJwtAuthenticationConverter();
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable()) // Authorization: Bearer only; no session/cookie
                // authentication
                .cors(Customizer.withDefaults())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .authorizeHttpRequests(
                        a -> {
                            if (docsEnabled)
                                a.requestMatchers(
                                                ApiPaths.OPENAPI_DOCS_PATTERN,
                                                ApiPaths.SWAGGER_UI_PATTERN,
                                                ApiPaths.SWAGGER_UI_HTML)
                                        .permitAll();
                            a.requestMatchers(HttpMethod.GET, ApiPaths.UI_POLICY)
                                    .permitAll()
                                    .requestMatchers(ApiPaths.AUTH_WILDCARD)
                                    .authenticated()
                                    .requestMatchers(ApiPaths.USERS_BASE, ApiPaths.USERS_WILDCARD)
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(
                                            ApiPaths.AUDIT_WILDCARD,
                                            ApiPaths.AUDIT_BASE,
                                            ApiPaths.REPORTS_WILDCARD)
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(
                                            HttpMethod.POST,
                                            ApiPaths.NOTIFICATIONS_BASE,
                                            ApiPaths.NOTIFICATIONS_BASE
                                                    + ApiPaths.NOTIFICATIONS_BROADCAST)
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(
                                            HttpMethod.GET,
                                            ApiPaths.NOTIFICATIONS_BASE
                                                    + ApiPaths.NOTIFICATIONS_ADMIN)
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(
                                            HttpMethod.DELETE, ApiPaths.NOTIFICATIONS_WILDCARD)
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(
                                            ApiPaths.NOTIFICATIONS_BASE,
                                            ApiPaths.NOTIFICATIONS_WILDCARD)
                                    .authenticated()
                                    .requestMatchers(
                                            HttpMethod.GET,
                                            ApiPaths.SERVICES_BASE,
                                            ApiPaths.SERVICES_WILDCARD)
                                    .authenticated()
                                    .requestMatchers(
                                            ApiPaths.SERVICES_BASE, ApiPaths.SERVICES_WILDCARD)
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(
                                            ApiPaths.CUSTOMERS_BASE, ApiPaths.CUSTOMERS_WILDCARD)
                                    .hasAnyRole(
                                            Role.ADMIN.name(),
                                            Role.RECEPTIONIST.name(),
                                            Role.CUSTOMER.name())
                                    .requestMatchers(
                                            ApiPaths.VEHICLES_BASE, ApiPaths.VEHICLES_WILDCARD)
                                    .hasAnyRole(
                                            Role.ADMIN.name(),
                                            Role.RECEPTIONIST.name(),
                                            Role.TECHNICIAN.name(),
                                            Role.CUSTOMER.name())
                                    .requestMatchers(
                                            ApiPaths.BOOKINGS_BASE, ApiPaths.BOOKINGS_WILDCARD)
                                    .hasAnyRole(
                                            Role.ADMIN.name(),
                                            Role.RECEPTIONIST.name(),
                                            Role.CUSTOMER.name())
                                    .requestMatchers(
                                            ApiPaths.RECEPTION_BASE, ApiPaths.RECEPTION_WILDCARD)
                                    .hasAnyRole(Role.ADMIN.name(), Role.RECEPTIONIST.name())
                                    .requestMatchers(
                                            ApiPaths.VEHICLE_CHECKS_BASE,
                                            ApiPaths.VEHICLE_CHECKS_WILDCARD,
                                            ApiPaths.ATTACHMENTS_BASE,
                                            ApiPaths.ATTACHMENTS_WILDCARD)
                                    .hasAnyRole(
                                            Role.ADMIN.name(),
                                            Role.RECEPTIONIST.name(),
                                            Role.TECHNICIAN.name(),
                                            Role.CUSTOMER.name())
                                    .requestMatchers(
                                            ApiPaths.EMPLOYEES_BASE,
                                            ApiPaths.EMPLOYEES_WILDCARD,
                                            ApiPaths.SHIFTS_BASE,
                                            ApiPaths.SHIFTS_WILDCARD,
                                            ApiPaths.SERVICE_BAYS_BASE,
                                            ApiPaths.SERVICE_BAYS_WILDCARD,
                                            ApiPaths.BAY_RESERVATIONS_BASE,
                                            ApiPaths.BAY_RESERVATIONS_WILDCARD,
                                            ApiPaths.REPAIR_ORDERS_BASE,
                                            ApiPaths.REPAIR_ORDERS_WILDCARD,
                                            ApiPaths.REPAIR_TASKS_BASE,
                                            ApiPaths.REPAIR_TASKS_WILDCARD,
                                            ApiPaths.QUALITY_CHECKS_BASE,
                                            ApiPaths.QUALITY_CHECKS_WILDCARD,
                                            ApiPaths.WARRANTIES_BASE,
                                            ApiPaths.WARRANTIES_WILDCARD)
                                    .hasAnyRole(
                                            Role.ADMIN.name(),
                                            Role.RECEPTIONIST.name(),
                                            Role.TECHNICIAN.name(),
                                            Role.WAREHOUSE_STAFF.name())
                                    .requestMatchers(
                                            ApiPaths.ESTIMATES_BASE,
                                            ApiPaths.ESTIMATES_WILDCARD,
                                            ApiPaths.INVENTORY_WILDCARD,
                                            ApiPaths.BILLING_WILDCARD)
                                    .authenticated()
                                    .requestMatchers(
                                            ApiPaths.CUSTOMERS_BASE, ApiPaths.CUSTOMERS_BASE + "/**",
                                            ApiPaths.VEHICLES_BASE, ApiPaths.VEHICLES_BASE + "/**",
                                            ApiPaths.SERVICES_BASE, ApiPaths.SERVICES_BASE + "/**",
                                            ApiPaths.BOOKINGS_BASE, ApiPaths.BOOKINGS_BASE + "/**",
                                            ApiPaths.RECEPTION_BASE, ApiPaths.RECEPTION_BASE + "/**",
                                            ApiPaths.VEHICLE_CHECKS_BASE, ApiPaths.VEHICLE_CHECKS_BASE + "/**",
                                            ApiPaths.ATTACHMENTS_BASE, ApiPaths.ATTACHMENTS_BASE + "/**")
                                    .authenticated()
                                    .anyRequest()
                                    .denyAll();
                        })
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (request, response, error) ->
                                                        errors.write(
                                                                request,
                                                                response,
                                                                ResponseCode.UNAUTHORIZED))
                                        .accessDeniedHandler(
                                                (request, response, error) ->
                                                        errors.write(
                                                                request,
                                                                response,
                                                                ResponseCode.FORBIDDEN)))
                .oauth2ResourceServer(
                        o ->
                                o.jwt(j -> j.jwtAuthenticationConverter(authentication))
                                        .authenticationEntryPoint(
                                                (request, response, error) ->
                                                        errors.write(
                                                                request,
                                                                response,
                                                                ResponseCode.INVALID_TOKEN))
                                        .accessDeniedHandler(
                                                (request, response, error) ->
                                                        errors.write(
                                                                request,
                                                                response,
                                                                ResponseCode.FORBIDDEN)))
                .addFilterBefore(
                        new InfrastructureFailureBoundary(errors),
                        BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsFilter corsFilter(
            @Qualifier("corsConfiguration") CorsConfigurationSource source,
            SecurityErrorWriter errors) {
        CorsFilter filter = new CorsFilter(source);
        filter.setCorsProcessor(new JsonCorsProcessor(errors));
        return filter;
    }

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter filter) {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Run once inside the Spring Security filter chain.
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfiguration(AuthProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(
                properties.allowedOrigins() == null
                        ? List.of()
                        : properties.allowedOrigins().stream().filter(s -> !s.isBlank()).toList());
        cors.setAllowedMethods(SecurityConstants.CORS_ALLOWED_METHODS);
        cors.setAllowedHeaders(SecurityConstants.CORS_ALLOWED_HEADERS);
        cors.setExposedHeaders(SecurityConstants.CORS_EXPOSED_HEADERS);
        cors.setAllowCredentials(false);
        cors.setMaxAge(properties.corsMaxAge().toSeconds());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ApiPaths.API_CORS_PATTERN, cors);
        return source;
    }

    @RequiredArgsConstructor
    private static final class InfrastructureFailureBoundary extends OncePerRequestFilter {
        private static final Logger LOG =
                LoggerFactory.getLogger(InfrastructureFailureBoundary.class);
        private final SecurityErrorWriter errors;

        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            try {
                chain.doFilter(request, response);
            } catch (DataAccessException | AuthenticationServiceException unavailable) {
                SafeFailureLog.error(LOG, "Security infrastructure failure", unavailable);
                if (response.isCommitted()) throw unavailable;
                errors.write(request, response, ResponseCode.AUTHENTICATION_UNAVAILABLE);
            }
        }
    }
}
