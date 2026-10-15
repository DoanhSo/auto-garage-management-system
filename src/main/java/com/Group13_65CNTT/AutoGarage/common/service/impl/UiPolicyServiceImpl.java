package com.Group13_65CNTT.AutoGarage.common.service.impl;

import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.common.config.PaginationProperties;
import com.Group13_65CNTT.AutoGarage.common.config.PasswordProperties;
import com.Group13_65CNTT.AutoGarage.common.config.ReportProperties;
import com.Group13_65CNTT.AutoGarage.common.dto.UiPolicyResponse;
import com.Group13_65CNTT.AutoGarage.common.service.UiPolicyService;
import com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UiPolicyServiceImpl implements UiPolicyService {
    private final PasswordProperties passwords;
    private final PaginationProperties pagination;
    private final ReportProperties reports;
    private final NotificationProperties notifications;

    @Override
    public UiPolicyResponse getPolicy() {
        return UiPolicyResponse.builder()
                .passwordMinLength(passwords.getMinLength())
                .passwordMaxUtf8Bytes(PasswordPolicy.BCRYPT_MAX_BYTES)
                .paginationDefaultSize(pagination.getDefaultSize())
                .paginationMaxSize(pagination.getMaxSize())
                .paginationMaxPage(pagination.getMaxPage())
                .reportMaxPeriodSeconds(reports.getMaxPeriod().toSeconds())
                .garageTimeZone(notifications.getZone())
                .build();
    }
}
