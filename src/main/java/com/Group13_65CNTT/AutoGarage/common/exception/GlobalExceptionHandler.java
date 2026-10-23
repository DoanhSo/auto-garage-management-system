package com.Group13_65CNTT.AutoGarage.common.exception;

import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.DatabaseFailures;
import com.Group13_65CNTT.AutoGarage.common.util.SafeFailureLog;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(
            BusinessException failure, HttpServletRequest request) {
        if (failure.getResponseCode().getHttpStatus().is5xxServerError()) {
            SafeFailureLog.error(log, "Business operation unavailable", failure);
        }
        return error(failure.getResponseCode(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> sharedNotFound(
            ResourceNotFoundException failure, HttpServletRequest request) {
        return error(ResponseCode.RESOURCE_NOT_FOUND, request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> sharedBusinessRule(
            BusinessRuleException failure, HttpServletRequest request) {
        return error(ResponseCode.BAD_REQUEST, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> sharedConflict(
            ConflictException failure, HttpServletRequest request) {
        return error(ResponseCode.DUPLICATE_RESOURCE, request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Map<String, String>>> invalidFields(
            BindException failure, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (ObjectError item : failure.getBindingResult().getAllErrors()) {
            String name = item instanceof FieldError field ? field.getField() : item.getObjectName();
            String message = item.getDefaultMessage();
            if (message == null || (item instanceof FieldError field && field.isBindingFailure())) {
                message = "Invalid value";
            }
            fields.putIfAbsent(name, message);
        }
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        ApiResponse.error(
                                ResponseCode.VALIDATION_ERROR, fields, request.getRequestURI()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> invalidMethod(
            HandlerMethodValidationException failure, HttpServletRequest request) {
        if (failure.isForReturnValue()) return unexpected(failure, request);
        return error(ResponseCode.BAD_REQUEST, request);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        ServletRequestBindingException.class
    })
    public ResponseEntity<ApiResponse<Void>> invalidInput(
            Exception failure, HttpServletRequest request) {
        return error(ResponseCode.BAD_REQUEST, request);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> badCredentials(
            AuthenticationException failure, HttpServletRequest request) {
        return error(ResponseCode.BAD_CREDENTIALS, request);
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<ApiResponse<Void>> authenticationUnavailable(
            AuthenticationServiceException failure, HttpServletRequest request) {
        SafeFailureLog.error(log, "Authentication operation unavailable", failure);
        return error(ResponseCode.AUTHENTICATION_UNAVAILABLE, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> unauthenticated(
            AuthenticationException failure, HttpServletRequest request) {
        return error(ResponseCode.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> denied(
            AccessDeniedException failure, HttpServletRequest request) {
        return error(ResponseCode.FORBIDDEN, request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> missingRoute(
            Exception failure, HttpServletRequest request) {
        return error(ResponseCode.RESOURCE_NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> wrongMethod(
            HttpRequestMethodNotSupportedException failure, HttpServletRequest request) {
        ResponseCode code = ResponseCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(code.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(
                        headers -> {
                            if (failure.getSupportedHttpMethods() != null) {
                                headers.setAllow(failure.getSupportedHttpMethods());
                            }
                        })
                .body(ApiResponse.error(code, null, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> wrongMedia(
            HttpMediaTypeNotSupportedException failure, HttpServletRequest request) {
        return error(ResponseCode.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> wrongResponseMedia(
            HttpMediaTypeNotAcceptableException failure, HttpServletRequest request) {
        return error(ResponseCode.NOT_ACCEPTABLE, request);
    }

    @ExceptionHandler({
        OptimisticLockingFailureException.class,
        PessimisticLockingFailureException.class,
        QueryTimeoutException.class
    })
    public ResponseEntity<ApiResponse<Void>> concurrentModification(
            DataAccessException failure, HttpServletRequest request) {
        return error(ResponseCode.CONCURRENT_MODIFICATION, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> integrity(
            DataIntegrityViolationException failure, HttpServletRequest request) {
        if (DatabaseFailures.isDuplicateKey(failure))
            return error(ResponseCode.DUPLICATE_RESOURCE, request);
        return unexpected(failure, request);
    }

    @ExceptionHandler({
        DataAccessResourceFailureException.class,
        TransientDataAccessResourceException.class,
        CannotCreateTransactionException.class,
        TransactionTimedOutException.class
    })
    public ResponseEntity<ApiResponse<Void>> infrastructure(
            Exception failure, HttpServletRequest request) {
        SafeFailureLog.error(log, "Data service unavailable", failure);
        return error(ResponseCode.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(
            Exception failure, HttpServletRequest request) {
        SafeFailureLog.error(log, "Unexpected request failure", failure);
        return error(ResponseCode.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ApiResponse<Void>> error(ResponseCode code, HttpServletRequest request) {
        return ResponseEntity.status(code.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(code, null, request.getRequestURI()));
    }
}
