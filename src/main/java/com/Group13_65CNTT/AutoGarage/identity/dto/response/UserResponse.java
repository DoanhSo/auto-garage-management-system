package com.Group13_65CNTT.AutoGarage.identity.dto.response;

import com.Group13_65CNTT.AutoGarage.common.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String identityId;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private boolean active;
    private long version;
    private Instant createdAt;
}
