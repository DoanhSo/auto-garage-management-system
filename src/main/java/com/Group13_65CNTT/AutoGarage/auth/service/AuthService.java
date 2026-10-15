package com.Group13_65CNTT.AutoGarage.auth.service;

import com.Group13_65CNTT.AutoGarage.auth.dto.request.LoginRequest;
import com.Group13_65CNTT.AutoGarage.auth.dto.request.PasswordChangeRequest;
import com.Group13_65CNTT.AutoGarage.auth.dto.response.LoginResponse;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    UserResponse me();

    void logout();

    void changePassword(PasswordChangeRequest request);
}
