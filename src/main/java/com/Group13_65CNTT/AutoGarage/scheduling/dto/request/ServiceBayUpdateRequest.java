package com.Group13_65CNTT.AutoGarage.scheduling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBayUpdateRequest {

    @NotBlank(message = "Bay name is required")
    @Size(max = 100, message = "Bay name must not exceed 100 characters")
    private String bayName;

    @NotBlank(message = "Bay type is required")
    @Size(max = 30, message = "Bay type must not exceed 30 characters")
    private String bayType;

    private String description;

    private Boolean isActive;
}
