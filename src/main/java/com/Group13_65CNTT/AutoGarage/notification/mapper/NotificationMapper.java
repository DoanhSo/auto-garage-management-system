package com.Group13_65CNTT.AutoGarage.notification.mapper;

import com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationResponse;
import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "payloadJson", source = "payloadJson")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "readAt", source = "readAt")
    @Mapping(target = "createdAt", source = "createdAt")
    NotificationResponse toResponse(Notification notification);
}
