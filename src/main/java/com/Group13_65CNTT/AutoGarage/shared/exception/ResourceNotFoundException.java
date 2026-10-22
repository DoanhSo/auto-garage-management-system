package com.Group13_65CNTT.AutoGarage.shared.exception;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(ResponseCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ResponseCode responseCode) {
        super(responseCode);
    }
}
