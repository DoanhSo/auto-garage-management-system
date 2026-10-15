package com.Group13_65CNTT.AutoGarage.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("security.password")
public class PasswordProperties {
    @Min(12)
    @Max(72)
    private int minLength = 12;
}
