package com.Group13_65CNTT.AutoGarage.shared.exception;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(ResponseCode.DUPLICATE_RESOURCE, message);
    }

    public ConflictException(ResponseCode responseCode, String message) {
        super(responseCode, message);
    }

    public ConflictException(ResponseCode responseCode) {
        super(responseCode);
    }
}
