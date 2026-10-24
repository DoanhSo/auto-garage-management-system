package com.Group13_65CNTT.AutoGarage.shared.exception;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

public class BusinessRuleException extends BusinessException {
    public BusinessRuleException(String message) {
        super(ResponseCode.BAD_REQUEST, message);
    }

    public BusinessRuleException(ResponseCode responseCode) {
        super(responseCode);
    }
}
