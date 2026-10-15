package com.Group13_65CNTT.AutoGarage.common.constant;

public final class SecurityConstants {
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_ANY_ROLE = "isAuthenticated()";

    public static final String ROLE_CLAIM = "role";
    public static final String AUTH_VERSION_CLAIM = "auth_version";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String BEARER_TOKEN_TYPE = "Bearer";

    private SecurityConstants() {}
}
