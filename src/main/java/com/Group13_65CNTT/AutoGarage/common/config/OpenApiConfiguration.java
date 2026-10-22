package com.Group13_65CNTT.AutoGarage.common.config;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI garageOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title(SecurityConstants.OPENAPI_TITLE)
                                .version(SecurityConstants.OPENAPI_VERSION))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme(SecurityConstants.OPENAPI_BEARER_SCHEME)
                                                .bearerFormat(
                                                        SecurityConstants.OPENAPI_BEARER_FORMAT)));
    }
}
