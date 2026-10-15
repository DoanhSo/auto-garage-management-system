package com.Group13_65CNTT.AutoGarage.common.dto;

import lombok.Builder;
import lombok.Getter;

/** Public validation limits only; never include credentials or provider configuration. */
@Getter
@Builder
public class UiPolicyResponse {
    private final int passwordMinLength;
    private final int passwordMaxUtf8Bytes;
    private final int paginationDefaultSize;
    private final int paginationMaxSize;
    private final int paginationMaxPage;
    private final long reportMaxPeriodSeconds;
    private final String garageTimeZone;
}
