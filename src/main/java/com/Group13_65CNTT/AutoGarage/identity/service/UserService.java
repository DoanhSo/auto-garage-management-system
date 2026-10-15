package com.Group13_65CNTT.AutoGarage.identity.service;

import com.Group13_65CNTT.AutoGarage.identity.dto.request.UserCreateRequest;
import com.Group13_65CNTT.AutoGarage.identity.dto.request.UserUpdateRequest;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);

    Page<UserResponse> searchUsers(String keyword, Boolean active, Pageable pageable);

    UserResponse getUser(Long id);

    UserResponse updateUser(Long id, UserUpdateRequest request);

    void deactivateUser(Long id, long version);
}
