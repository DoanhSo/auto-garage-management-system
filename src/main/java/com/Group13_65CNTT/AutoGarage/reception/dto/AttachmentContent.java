package com.Group13_65CNTT.AutoGarage.reception.dto;

/** Nội dung nhị phân của tệp đính kèm để phục vụ xem trực tiếp trên trình duyệt. */
public record AttachmentContent(byte[] bytes, String contentType, String originalFilename) {}
