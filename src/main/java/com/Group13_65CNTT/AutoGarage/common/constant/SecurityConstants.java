package com.Group13_65CNTT.AutoGarage.common.constant;

import java.util.List;

public final class SecurityConstants {
    public static final String PERMIT_ALL = "permitAll()";
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_ROLE_RECEPTIONIST = "hasRole('RECEPTIONIST')";
    public static final String HAS_ROLE_TECHNICIAN = "hasRole('TECHNICIAN')";
    public static final String HAS_ROLE_WAREHOUSE_STAFF = "hasRole('WAREHOUSE_STAFF')";
    public static final String HAS_ROLE_WAREHOUSE_OR_ADMIN =
            "hasAnyRole('ADMIN', 'WAREHOUSE_STAFF')";
    public static final String HAS_ROLE_RECEPTION_OR_ADMIN =
            "hasAnyRole('ADMIN', 'RECEPTIONIST')";
    public static final String HAS_ROLE_ADMIN_OR_RECEPTIONIST =
            "hasAnyRole('ADMIN', 'RECEPTIONIST')";
    public static final String HAS_ROLE_ADMIN_OR_TECHNICIAN =
            "hasAnyRole('ADMIN', 'TECHNICIAN')";
    public static final String HAS_ROLE_FINANCE_STAFF =
            "hasAnyRole('ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF')";
    public static final String HAS_ROLE_STAFF =
            "hasAnyRole('ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF', 'TECHNICIAN')";
    public static final String HAS_ROLE_INTERNAL_STAFF =
            "hasAnyRole('ADMIN', 'RECEPTIONIST', 'TECHNICIAN', 'WAREHOUSE_STAFF')";
    public static final String HAS_ROLE_RECEPTIONIST_OR_ADMIN = HAS_ROLE_ADMIN_OR_RECEPTIONIST;
    public static final String HAS_ROLE_RECEPTIONIST_OR_TECHNICIAN_OR_ADMIN = "hasAnyRole('ADMIN','RECEPTIONIST','TECHNICIAN')";
    public static final String HAS_ANY_ROLE = "isAuthenticated()";
    public static final String HAS_STAFF_OR_ADMIN =
            "hasAnyRole('ADMIN','RECEPTIONIST','WAREHOUSE_STAFF','TECHNICIAN')";
    public static final String HAS_FRONT_DESK = "hasAnyRole('ADMIN','RECEPTIONIST')";
    public static final String HAS_BOOKING_ACCESS = "hasAnyRole('ADMIN','RECEPTIONIST','CUSTOMER')";
    public static final String HAS_TECHNICAL_ACCESS =
            "hasAnyRole('ADMIN','RECEPTIONIST','TECHNICIAN')";

    public static final String REALM_ACCESS_CLAIM = "realm_access";
    public static final String ROLES_CLAIM = "roles";
    public static final String ROLE_CLAIM = "role";
    public static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    public static final String EMAIL_CLAIM = "email";
    public static final String EMAIL_VERIFIED_CLAIM = "email_verified";
    public static final String NAME_CLAIM = "name";
    public static final String AUTH_VERSION_CLAIM = "auth_version";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String BEARER_TOKEN_TYPE = "Bearer";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String OPENAPI_SECURITY_SCHEME_NAME = "bearerAuth";
    public static final String OPENAPI_BEARER_SCHEME = "bearer";
    public static final String OPENAPI_BEARER_FORMAT = "JWT";
    public static final String OPENAPI_TITLE = "AutoGarage API";
    public static final String OPENAPI_VERSION = "v1";

    public static final String OAUTH2_INVALID_TOKEN_ERROR = "invalid_token";
    public static final String OAUTH2_INVALID_TOKEN_DESCRIPTION = "Invalid or revoked token";

    public static final int MAX_SUBJECT_LENGTH = 100;
    public static final long DEFAULT_AUTH_VERSION = 0L;
    public static final String AUTH_VERSION_PATTERN = "[0-9]{1,19}";
    public static final String POSITIVE_LONG_ID_PATTERN = "[1-9][0-9]{0,18}";

    public static final int IDENTITY_ID_MAX_LENGTH = 36;
    public static final String IDENTITY_ID_PATTERN = "[A-Za-z0-9-]{1,36}";
    public static final int USERNAME_MAX_LENGTH = 100;
    public static final String USERNAME_PATTERN = "[A-Za-z0-9._-]{3,100}";
    public static final int EMAIL_MAX_LENGTH = 254;
    public static final int FULL_NAME_MAX_LENGTH = 150;
    public static final int ROLE_MAX_LENGTH = 30;
    public static final int PASSWORD_HASH_MAX_LENGTH = 255;

    public static final String CUSTOMER_CODE_OIDC_PREFIX = "CUS-OIDC-";
    public static final String DEFAULT_CUSTOMER_NAME = "Khách hàng AutoGarage";

    public static final long MAX_CLOCK_SKEW_SECONDS = 60L;
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";
    public static final String UTF_8_ENCODING = "UTF-8";

    public static final List<String> CORS_ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    public static final List<String> CORS_ALLOWED_HEADERS =
            List.of("Authorization", "Content-Type");
    public static final List<String> CORS_EXPOSED_HEADERS = List.of("Location");

    private SecurityConstants() {}
}
