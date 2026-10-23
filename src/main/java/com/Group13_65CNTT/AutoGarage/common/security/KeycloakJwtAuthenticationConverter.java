package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Extracts AutoGarage roles from Keycloak OIDC claims ({@code realm_access.roles} or {@code role})
 * and maps them into Spring Security {@link GrantedAuthority} instances.
 */
public final class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities =
                extractSingleRole(jwt)
                        .<List<GrantedAuthority>>map(
                                role ->
                                        List.of(
                                                new SimpleGrantedAuthority(
                                                        SecurityConstants.ROLE_PREFIX
                                                                + role.name())))
                        .orElseGet(List::of);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    public static Optional<Role> extractSingleRole(Jwt token) {
        Set<Role> matched = new LinkedHashSet<>();
        Object realmAccess = token.getClaims().get(SecurityConstants.REALM_ACCESS_CLAIM);
        if (realmAccess instanceof Map<?, ?> map) {
            Object rawRoles = map.get(SecurityConstants.ROLES_CLAIM);
            if (!(rawRoles instanceof Collection<?> collection)) {
                return Optional.empty();
            }
            for (Object item : collection) {
                if (!(item instanceof String roleName)) {
                    return Optional.empty();
                }
                try {
                    matched.add(Role.valueOf(roleName));
                } catch (IllegalArgumentException ignoredNonGarageRole) {
                    // Ignore Keycloak built-in roles such as default-roles-* or offline_access.
                }
            }
        } else if (realmAccess != null) {
            return Optional.empty();
        }

        if (token.hasClaim(SecurityConstants.ROLE_CLAIM)) {
            Object directRole = token.getClaims().get(SecurityConstants.ROLE_CLAIM);
            if (!(directRole instanceof String roleName)) {
                return Optional.empty();
            }
            try {
                matched.add(Role.valueOf(roleName));
            } catch (IllegalArgumentException invalidRole) {
                return Optional.empty();
            }
        }

        return matched.size() == 1 ? Optional.of(matched.iterator().next()) : Optional.empty();
    }
}
