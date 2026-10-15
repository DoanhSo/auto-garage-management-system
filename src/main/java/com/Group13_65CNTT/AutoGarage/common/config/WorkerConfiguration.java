package com.Group13_65CNTT.AutoGarage.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.http.HttpClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificationProperties.class)
public class WorkerConfiguration {
    @Bean
    HttpClient notificationHttpClient(NotificationProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
    }
}
