package com.Group13_65CNTT.AutoGarage.common.config;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.security.JsonCorsProcessor;
import com.Group13_65CNTT.AutoGarage.common.security.SecurityErrorWriter;
import com.Group13_65CNTT.AutoGarage.common.util.SafeFailureLog;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(SecurityConstants.ROLE_CLAIM);
        roles.setAuthorityPrefix(SecurityConstants.ROLE_PREFIX);
        JwtAuthenticationConverter authentication = new JwtAuthenticationConverter();
        authentication.setJwtGrantedAuthoritiesConverter(roles);
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
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll();
                            a.requestMatchers(HttpMethod.POST, ApiPaths.AUTH_BASE + ApiPaths.LOGIN)
                                    .permitAll()
                                    .requestMatchers(ApiPaths.AUTH_BASE + "/**")
                                    .authenticated()
                                    .requestMatchers(
                                            ApiPaths.USERS_BASE, ApiPaths.USERS_BASE + "/**")
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(
                                            ApiPaths.AUDIT_BASE + "/**",
                                            ApiPaths.AUDIT_BASE,
                                            ApiPaths.REPORTS_BASE + "/**")
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(HttpMethod.POST, ApiPaths.NOTIFICATIONS_BASE)
                                    .hasRole(Role.ADMIN.name())
                                    .requestMatchers(
                                            ApiPaths.NOTIFICATIONS_BASE,
                                            ApiPaths.NOTIFICATIONS_BASE + "/**")
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
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Location"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(properties.corsMaxAge().toSeconds());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    private static final class InfrastructureFailureBoundary extends OncePerRequestFilter {
        private static final Logger LOG =
                LoggerFactory.getLogger(InfrastructureFailureBoundary.class);
        private final SecurityErrorWriter errors;

        private InfrastructureFailureBoundary(SecurityErrorWriter errors) {
            this.errors = errors;
        }

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
