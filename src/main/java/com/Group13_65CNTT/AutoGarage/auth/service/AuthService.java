package com.Group13_65CNTT.AutoGarage.auth.service;

import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;

public interface AuthService {
    UserResponse me();

    void logout();
}
