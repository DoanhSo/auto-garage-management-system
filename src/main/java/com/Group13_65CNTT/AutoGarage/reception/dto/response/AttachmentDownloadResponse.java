package com.Group13_65CNTT.AutoGarage.reception.dto.response;

public record AttachmentDownloadResponse(String filename, String contentType, byte[] content) {}
