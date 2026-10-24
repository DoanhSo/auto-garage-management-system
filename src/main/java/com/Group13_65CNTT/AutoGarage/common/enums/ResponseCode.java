package com.Group13_65CNTT.AutoGarage.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {
    SUCCESS(1000, "Success", HttpStatus.OK),
    CREATED(1001, "Created", HttpStatus.CREATED),
    UPDATED(1002, "Updated", HttpStatus.OK),
    DELETED(1003, "Deleted", HttpStatus.OK),
    VALIDATION_ERROR(2001, "Request validation failed", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(2002, "Resource not found", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(
            2003,
            "Resource already exists or conflicts with an earlier request",
            HttpStatus.CONFLICT),
    BAD_REQUEST(2004, "Request fields or JSON are invalid", HttpStatus.BAD_REQUEST),
    STALE_USER_VERSION(2005, "Account was modified; reload before updating", HttpStatus.CONFLICT),
    LAST_ADMIN(
            2006,
            "The last active administrator cannot be disabled or demoted",
            HttpStatus.CONFLICT),
    INVALID_PASSWORD(
            2007,
            "Password does not satisfy the configured password policy",
            HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(2008, "HTTP method is not supported", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(
            2009, "Content type is not supported", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    CONCURRENT_MODIFICATION(
            2010, "Resource is being modified; retry the request", HttpStatus.CONFLICT),
    NOT_ACCEPTABLE(2011, "Requested response format is not supported", HttpStatus.NOT_ACCEPTABLE),
    BOOTSTRAP_ACCOUNT_CONFLICT(
            2012,
            "Configured bootstrap account is not an active administrator",
            HttpStatus.CONFLICT),
    UPLOAD_TOO_LARGE(2013, "Upload exceeds the configured size limit", HttpStatus.PAYLOAD_TOO_LARGE),
    UNAUTHORIZED(3001, "A valid bearer token is required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(3002, "You do not have permission for this operation", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(3003, "A valid bearer token is required", HttpStatus.UNAUTHORIZED),
    BAD_CREDENTIALS(3005, "Invalid username/email or password", HttpStatus.UNAUTHORIZED),
    INTERNAL_SERVER_ERROR(4001, "Unable to complete the request", HttpStatus.INTERNAL_SERVER_ERROR),
    AUTHENTICATION_UNAVAILABLE(
            4002, "Authentication service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    SERVICE_UNAVAILABLE(4003, "Service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    NOTIFICATION_DELIVERY_UNAVAILABLE(
            4004, "Notification provider is unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
