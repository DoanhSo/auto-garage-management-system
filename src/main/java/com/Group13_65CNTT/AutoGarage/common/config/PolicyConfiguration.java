package com.Group13_65CNTT.AutoGarage.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
    PasswordProperties.class,
    PaginationProperties.class,
    ReportProperties.class,
    WorkshopProperties.class
})
public class PolicyConfiguration {}
