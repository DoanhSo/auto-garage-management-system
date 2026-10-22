package com.Group13_65CNTT.AutoGarage.auth.service;

import com.Group13_65CNTT.AutoGarage.common.config.BootstrapProperties;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;

public interface BootstrapService {
    UserResponse initialize(BootstrapProperties properties);
}
