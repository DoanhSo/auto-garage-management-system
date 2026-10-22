package com.Group13_65CNTT.AutoGarage.common.exception;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;

public class NotificationDeliveryException extends BusinessException {
    public NotificationDeliveryException() {
        super(ResponseCode.NOTIFICATION_DELIVERY_UNAVAILABLE);
    }

    public NotificationDeliveryException(Throwable cause) {
        super(ResponseCode.NOTIFICATION_DELIVERY_UNAVAILABLE, cause);
    }
}
